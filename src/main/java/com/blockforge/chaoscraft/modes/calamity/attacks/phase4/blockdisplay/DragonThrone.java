package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.blockdisplay;

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
 * Phase 4D Block Display -- GROUP 1: THE DRAGON'S THRONE APPARATUS (#1-8)
 * 8 attacks forming the central sanctum structures -- the gilded landing zone's
 * architectural anchors and ritual geometry for the Void Emperor arena.
 *
 * Structures:
 *  #1  VoidThroneDaisCore     -- 3x3x2 stacked gold/blackstone/amethyst dais
 *  #2  NorthCardinalPillar    -- North throne pillar with end rod spire
 *  #3  SouthCardinalPillar    -- South pillar with phase-shifted breathing
 *  #4  EastCardinalPillar     -- East pillar with triple-pronged crown
 *  #5  WestCardinalPillar     -- West pillar with calcite crown tines
 *  #6  SanctumArchGateway     -- North approach arch with gold particle curtain
 *  #7  VoidOrbSuspension      -- Central suspended amethyst orb with satellites
 *  #8  ThroneBackFinArray     -- Seven-fin throne back fan structure
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - Void Emperor palette: magenta(180,0,200), cyan(0,200,255), purple(128,0,255)
 * - Materials: OBSIDIAN, CRYING_OBSIDIAN, AMETHYST_BLOCK, END_STONE, PURPUR_BLOCK,
 *   PURPUR_PILLAR, END_ROD, SEA_LANTERN, POLISHED_BLACKSTONE, DARK_PRISMARINE
 * - Damage HP 4.0-12.0
 * - AxisAngle4f ONLY (never Quaternionf)
 * - Static DisplayBuilder methods
 * - spawnedEntities.add(h.entity()) ALWAYS
 * - onCleanup -> displayBuilder.removeAll()
 */
public final class DragonThrone {

    private DragonThrone() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidThroneDaisCore(plugin));
        registry.register(new NorthCardinalPillar(plugin));
        registry.register(new SouthCardinalPillar(plugin));
        registry.register(new EastCardinalPillar(plugin));
        registry.register(new WestCardinalPillar(plugin));
        registry.register(new SanctumArchGateway(plugin));
        registry.register(new VoidOrbSuspension(plugin));
        registry.register(new ThroneBackFinArray(plugin));
    }

    // ================================================================
    // #1 -- VOID THRONE DAIS CORE
    // 3x3x2 stacked structure: gold plate base, rotated polished blackstone
    // diamond at Y+1, amethyst crown at Y+2 with sinusoidal bob.
    // ================================================================
    public static class VoidThroneDaisCore extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> basePlates = new ArrayList<>();
        private BlockDisplayHandle diamondBlock;
        private BlockDisplayHandle amethystCrown;

        public VoidThroneDaisCore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_throne_dais_core", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Base layer: 3x3 gold plates at Y+0
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location loc = center.clone().add(x, 0, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GOLD_BLOCK);
                    h.scale(1.0f, 0.1f, 1.0f).glow(180, 0, 200).interpolation(3, 0);
                    basePlates.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Middle layer: large polished blackstone at Y+1, rotated 45 degrees on Y
            Location midLoc = center.clone().add(0, 1, 0);
            diamondBlock = displayBuilder.spawnBlock(midLoc, Material.POLISHED_BLACKSTONE);
            diamondBlock.scale(2.0f, 2.0f, 2.0f)
                    .rotate((float) Math.toRadians(45), 0, 1, 0)
                    .glow(128, 0, 255).interpolation(0, 0);
            spawnedEntities.add(diamondBlock.entity());

            // Top layer: amethyst crown at Y+2
            Location topLoc = center.clone().add(0, 2, 0);
            amethystCrown = displayBuilder.spawnBlock(topLoc, Material.AMETHYST_BLOCK);
            amethystCrown.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(5, 0);
            spawnedEntities.add(amethystCrown.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Diamond block: continuous Y rotation at 0.4 deg/tick
            if (diamondBlock != null) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 0.4);
                diamondBlock.rotate(rotAngle, 0, 1, 0);
            }

            // Amethyst crown: sinusoidal vertical bob
            if (amethystCrown != null) {
                float yDisp = 0.15f * (float) Math.sin(ticksAlive * 0.05);
                amethystCrown.translate(-0.4f, -0.5f + yDisp, -0.4f);
                amethystCrown.interpolation(5, 0);
            }

            // Amethyst chime every ~160-240 ticks (8-12 seconds randomized)
            if (ticksAlive % 180 == 0) {
                Location chime = center.clone().add(0, 2, 0);
                float pitch = 0.8f + (float) (Math.random() * 0.4);
                DisplayBuilder.playSound(chime, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, pitch);
            }

            // Portal heartbeat every 80 ticks (4 seconds)
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, 1, 0), Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.5f);
            }

            // Purple dust from crown
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 2.5, 0), 4, 0.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidThroneDaisCore(plugin); }
    }

    // ================================================================
    // #2 -- NORTH CARDINAL THRONE PILLAR
    // 7-element compound pillar: obsidian base, gold+blackstone stack,
    // amethyst capital, gold shelf, end rod spire. Located at Z-15.
    // ================================================================
    public static class NorthCardinalPillar extends BlockDisplayAttack {

        private BlockDisplayHandle obsidianBase;
        private BlockDisplayHandle goldBlock1;
        private BlockDisplayHandle blackstoneBlock;
        private BlockDisplayHandle goldBlock2;
        private BlockDisplayHandle amethystCapital;
        private BlockDisplayHandle goldShelf;
        private BlockDisplayHandle endRodSpire;

        public NorthCardinalPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("north_cardinal_pillar", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, -15);

            // Element 1: obsidian foundation
            obsidianBase = displayBuilder.spawnBlock(base, Material.OBSIDIAN);
            obsidianBase.scale(1.2f, 0.4f, 1.2f).glow(128, 0, 255);
            spawnedEntities.add(obsidianBase.entity());

            // Element 2: gold at Y+1
            goldBlock1 = displayBuilder.spawnBlock(base.clone().add(0, 1, 0), Material.GOLD_BLOCK);
            goldBlock1.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock1.entity());

            // Element 3: polished blackstone at Y+2
            blackstoneBlock = displayBuilder.spawnBlock(base.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            blackstoneBlock.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneBlock.entity());

            // Element 4: gold at Y+3
            goldBlock2 = displayBuilder.spawnBlock(base.clone().add(0, 3, 0), Material.GOLD_BLOCK);
            goldBlock2.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock2.entity());

            // Element 5: amethyst capital at Y+4 (step-in)
            amethystCapital = displayBuilder.spawnBlock(base.clone().add(0, 4, 0), Material.AMETHYST_BLOCK);
            amethystCapital.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(amethystCapital.entity());

            // Element 6: gold shelf at Y+5
            goldShelf = displayBuilder.spawnBlock(base.clone().add(0, 5, 0), Material.GOLD_BLOCK);
            goldShelf.scale(1.4f, 0.3f, 1.4f).glow(180, 0, 200);
            spawnedEntities.add(goldShelf.entity());

            // Element 7: end rod spire Y+5.5 to Y+11.5
            endRodSpire = displayBuilder.spawnBlock(base.clone().add(0, 5.5, 0), Material.END_ROD);
            endRodSpire.scale(0.3f, 6.0f, 0.3f).glow(0, 200, 255).interpolation(8, 0);
            spawnedEntities.add(endRodSpire.entity());

            // Spawn thunder crack
            DisplayBuilder.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(0, 0, -15);

            // Spire breathing: scale Y pulses +/- 0.2 on 4-second (80-tick) sin cycle
            if (endRodSpire != null) {
                float yScale = 6.0f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 40);
                endRodSpire.scale(0.3f, yScale, 0.3f);
                endRodSpire.interpolation(8, 0);
            }

            // Amethyst capital: slow Y rotation at 0.2 deg/tick
            if (amethystCapital != null) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 0.2);
                amethystCapital.rotate(rotAngle, 0, 1, 0);
                amethystCapital.interpolation(3, 0);
            }

            // END_ROD particle fountain at spire top
            if (ticksAlive % 4 == 0) {
                Location spireTop = base.clone().add(0, 12, 0);
                base.getWorld().spawnParticle(Particle.END_ROD, spireTop, 3, 0.5, 0.2, 0.5, 0.01);
            }

            // Amethyst cluster sound every 300 ticks (15 sec)
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(base.clone().add(0, 4, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.2f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NorthCardinalPillar(plugin); }
    }

    // ================================================================
    // #3 -- SOUTH CARDINAL THRONE PILLAR
    // Identical to North but at Z+15. Phase-shifted spire breathing
    // (pi offset), counter-clockwise amethyst rotation, drip lava underglow.
    // ================================================================
    public static class SouthCardinalPillar extends BlockDisplayAttack {

        private BlockDisplayHandle obsidianBase;
        private BlockDisplayHandle goldBlock1;
        private BlockDisplayHandle blackstoneBlock;
        private BlockDisplayHandle goldBlock2;
        private BlockDisplayHandle amethystCapital;
        private BlockDisplayHandle goldShelf;
        private BlockDisplayHandle endRodSpire;

        public SouthCardinalPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("south_cardinal_pillar", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(0, 0, 15);

            obsidianBase = displayBuilder.spawnBlock(base, Material.OBSIDIAN);
            obsidianBase.scale(1.2f, 0.4f, 1.2f).glow(128, 0, 255);
            spawnedEntities.add(obsidianBase.entity());

            goldBlock1 = displayBuilder.spawnBlock(base.clone().add(0, 1, 0), Material.GOLD_BLOCK);
            goldBlock1.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock1.entity());

            blackstoneBlock = displayBuilder.spawnBlock(base.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            blackstoneBlock.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneBlock.entity());

            goldBlock2 = displayBuilder.spawnBlock(base.clone().add(0, 3, 0), Material.GOLD_BLOCK);
            goldBlock2.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock2.entity());

            amethystCapital = displayBuilder.spawnBlock(base.clone().add(0, 4, 0), Material.AMETHYST_BLOCK);
            amethystCapital.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(amethystCapital.entity());

            goldShelf = displayBuilder.spawnBlock(base.clone().add(0, 5, 0), Material.GOLD_BLOCK);
            goldShelf.scale(1.4f, 0.3f, 1.4f).glow(180, 0, 200);
            spawnedEntities.add(goldShelf.entity());

            endRodSpire = displayBuilder.spawnBlock(base.clone().add(0, 5.5, 0), Material.END_ROD);
            endRodSpire.scale(0.3f, 6.0f, 0.3f).glow(0, 200, 255).interpolation(8, 0);
            spawnedEntities.add(endRodSpire.entity());

            DisplayBuilder.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(0, 0, 15);

            // Spire breathing: pi phase offset from north
            if (endRodSpire != null) {
                float yScale = 6.0f + 0.2f * (float) Math.sin(ticksAlive * Math.PI / 40 + Math.PI);
                endRodSpire.scale(0.3f, yScale, 0.3f);
                endRodSpire.interpolation(8, 0);
            }

            // Amethyst capital: counter-clockwise rotation
            if (amethystCapital != null) {
                float rotAngle = (float) Math.toRadians(-ticksAlive * 0.2);
                amethystCapital.rotate(rotAngle, 0, 1, 0);
                amethystCapital.interpolation(3, 0);
            }

            // END_ROD fountain
            if (ticksAlive % 4 == 0) {
                Location spireTop = base.clone().add(0, 12, 0);
                base.getWorld().spawnParticle(Particle.END_ROD, spireTop, 3, 0.5, 0.2, 0.5, 0.01);
            }

            // DRIP_LAVA underglow at Y+3.5 (south distinction)
            if (ticksAlive % 10 == 0) {
                Location underGlow = base.clone().add(0, 3.5, 0);
                base.getWorld().spawnParticle(Particle.DRIPPING_LAVA, underGlow, 1, 0.1, 0.2, 0.1, 0);
            }

            // Amethyst cluster sound
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(base.clone().add(0, 4, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.2f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SouthCardinalPillar(plugin); }
    }

    // ================================================================
    // #4 -- EAST CARDINAL THRONE PILLAR
    // Three-pronged end rod crown, gilded blackstone capital shelf,
    // CRIT particles at base. Located at X+15.
    // ================================================================
    public static class EastCardinalPillar extends BlockDisplayAttack {

        private BlockDisplayHandle obsidianBase;
        private BlockDisplayHandle goldBlock1;
        private BlockDisplayHandle blackstoneBlock;
        private BlockDisplayHandle goldBlock2;
        private BlockDisplayHandle amethystCapital;
        private BlockDisplayHandle blackstoneShelf;
        private final List<BlockDisplayHandle> crownSpires = new ArrayList<>();

        public EastCardinalPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("east_cardinal_pillar", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(15, 0, 0);

            obsidianBase = displayBuilder.spawnBlock(base, Material.OBSIDIAN);
            obsidianBase.scale(1.2f, 0.4f, 1.2f).glow(128, 0, 255);
            spawnedEntities.add(obsidianBase.entity());

            goldBlock1 = displayBuilder.spawnBlock(base.clone().add(0, 1, 0), Material.GOLD_BLOCK);
            goldBlock1.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock1.entity());

            blackstoneBlock = displayBuilder.spawnBlock(base.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            blackstoneBlock.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneBlock.entity());

            goldBlock2 = displayBuilder.spawnBlock(base.clone().add(0, 3, 0), Material.GOLD_BLOCK);
            goldBlock2.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock2.entity());

            amethystCapital = displayBuilder.spawnBlock(base.clone().add(0, 4, 0), Material.AMETHYST_BLOCK);
            amethystCapital.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(amethystCapital.entity());

            // Gilded blackstone shelf (east distinction)
            blackstoneShelf = displayBuilder.spawnBlock(base.clone().add(0, 5, 0), Material.POLISHED_BLACKSTONE);
            blackstoneShelf.scale(1.4f, 0.3f, 1.4f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneShelf.entity());

            // Three-pronged end rod crown at Y+6, each angled 10 deg outward, 120 deg apart
            for (int i = 0; i < 3; i++) {
                double yawAngle = Math.toRadians(120.0 * i);
                float offsetX = (float) (Math.cos(yawAngle) * 0.15);
                float offsetZ = (float) (Math.sin(yawAngle) * 0.15);
                Location spirePos = base.clone().add(offsetX, 6, offsetZ);
                BlockDisplayHandle spire = displayBuilder.spawnBlock(spirePos, Material.END_ROD);
                // Tilt each 10 degrees outward along its radial direction
                float tiltAngle = (float) Math.toRadians(10);
                float tiltAxisX = (float) -Math.sin(yawAngle);
                float tiltAxisZ = (float) Math.cos(yawAngle);
                spire.scale(0.2f, 4.0f, 0.2f)
                        .rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ)
                        .glow(0, 200, 255).interpolation(3, 0);
                crownSpires.add(spire);
                spawnedEntities.add(spire.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(15, 0, 0);

            // Crown spires rotate as a unit at 0.15 deg/tick
            // We teleport each crown spire around the center with the rotation
            float crownAngle = (float) Math.toRadians(ticksAlive * 0.15);
            for (int i = 0; i < crownSpires.size(); i++) {
                double baseYaw = Math.toRadians(120.0 * i);
                float tiltAngle = (float) Math.toRadians(10);
                float tiltAxisX = (float) -Math.sin(baseYaw + crownAngle);
                float tiltAxisZ = (float) Math.cos(baseYaw + crownAngle);
                crownSpires.get(i).rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                crownSpires.get(i).interpolation(3, 0);
            }

            // END_ROD particles from crown center Y+10
            if (ticksAlive % 3 == 0) {
                Location crownCenter = base.clone().add(0, 10, 0);
                base.getWorld().spawnParticle(Particle.END_ROD, crownCenter, 4, 0.3, 0.2, 0.3, 0.01);
            }

            // CRIT particles at base (east distinction)
            if (ticksAlive % 7 == 0) {
                Location baseSparkle = base.clone().add(0, 0.5, 0);
                base.getWorld().spawnParticle(Particle.CRIT, baseSparkle, 2, 0.3, 0.2, 0.3, 0.01);
            }

            // Sound
            if (ticksAlive % 300 == 0) {
                DisplayBuilder.playSound(base.clone().add(0, 4, 0),
                        Sound.BLOCK_AMETHYST_CLUSTER_HIT, 0.2f, 0.9f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EastCardinalPillar(plugin); }
    }

    // ================================================================
    // #5 -- WEST CARDINAL THRONE PILLAR
    // Mirrors east but calcite crown tines, SPELL_WITCH particles,
    // crying obsidian base. Located at X-15.
    // ================================================================
    public static class WestCardinalPillar extends BlockDisplayAttack {

        private BlockDisplayHandle cryingObsBase;
        private BlockDisplayHandle goldBlock1;
        private BlockDisplayHandle blackstoneBlock;
        private BlockDisplayHandle goldBlock2;
        private BlockDisplayHandle amethystCapital;
        private BlockDisplayHandle blackstoneShelf;
        private final List<BlockDisplayHandle> crownSpires = new ArrayList<>();

        public WestCardinalPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("west_cardinal_pillar", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(5.0);
            config.setDamageRadius(2.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location base = center.clone().add(-15, 0, 0);

            // Crying obsidian base (west distinction)
            cryingObsBase = displayBuilder.spawnBlock(base, Material.CRYING_OBSIDIAN);
            cryingObsBase.scale(1.2f, 0.4f, 1.2f).glow(180, 0, 200);
            spawnedEntities.add(cryingObsBase.entity());

            goldBlock1 = displayBuilder.spawnBlock(base.clone().add(0, 1, 0), Material.GOLD_BLOCK);
            goldBlock1.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock1.entity());

            blackstoneBlock = displayBuilder.spawnBlock(base.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            blackstoneBlock.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneBlock.entity());

            goldBlock2 = displayBuilder.spawnBlock(base.clone().add(0, 3, 0), Material.GOLD_BLOCK);
            goldBlock2.scale(1.0f, 1.0f, 1.0f).glow(180, 0, 200);
            spawnedEntities.add(goldBlock2.entity());

            amethystCapital = displayBuilder.spawnBlock(base.clone().add(0, 4, 0), Material.AMETHYST_BLOCK);
            amethystCapital.scale(0.8f, 0.8f, 0.8f).glow(128, 0, 255).interpolation(3, 0);
            spawnedEntities.add(amethystCapital.entity());

            blackstoneShelf = displayBuilder.spawnBlock(base.clone().add(0, 5, 0), Material.POLISHED_BLACKSTONE);
            blackstoneShelf.scale(1.4f, 0.3f, 1.4f).glow(128, 0, 255);
            spawnedEntities.add(blackstoneShelf.entity());

            // Three calcite crown tines (west distinction - no self-illumination)
            for (int i = 0; i < 3; i++) {
                double yawAngle = Math.toRadians(120.0 * i);
                float offsetX = (float) (Math.cos(yawAngle) * 0.15);
                float offsetZ = (float) (Math.sin(yawAngle) * 0.15);
                Location spirePos = base.clone().add(offsetX, 6, offsetZ);
                BlockDisplayHandle spire = displayBuilder.spawnBlock(spirePos, Material.CALCITE);
                float tiltAngle = (float) Math.toRadians(10);
                float tiltAxisX = (float) -Math.sin(yawAngle);
                float tiltAxisZ = (float) Math.cos(yawAngle);
                spire.scale(0.2f, 4.0f, 0.2f)
                        .rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ)
                        .glow(180, 0, 200).interpolation(3, 0);
                crownSpires.add(spire);
                spawnedEntities.add(spire.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location base = center.clone().add(-15, 0, 0);

            // Crown rotates opposite direction from east
            float crownAngle = (float) Math.toRadians(-ticksAlive * 0.15);
            for (int i = 0; i < crownSpires.size(); i++) {
                double baseYaw = Math.toRadians(120.0 * i);
                float tiltAngle = (float) Math.toRadians(10);
                float tiltAxisX = (float) -Math.sin(baseYaw + crownAngle);
                float tiltAxisZ = (float) Math.cos(baseYaw + crownAngle);
                crownSpires.get(i).rotate(tiltAngle, tiltAxisX, 0, tiltAxisZ);
                crownSpires.get(i).interpolation(3, 0);
            }

            // SPELL_WITCH particles between calcite tines at Y+10
            if (ticksAlive % 2 == 0) {
                Location tineCenter = base.clone().add(0, 10, 0);
                base.getWorld().spawnParticle(Particle.WITCH, tineCenter, 5, 0.3, 0.2, 0.3, 0.01);
            }

            // Amethyst capital: same rotation phase as north
            if (amethystCapital != null) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 0.2);
                amethystCapital.rotate(rotAngle, 0, 1, 0);
                amethystCapital.interpolation(3, 0);
            }

            // Resonate sound every 400 ticks (20 sec)
            if (ticksAlive % 400 == 0) {
                DisplayBuilder.playSound(base.clone().add(0, 5, 0),
                        Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.15f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WestCardinalPillar(plugin); }
    }

    // ================================================================
    // #6 -- SANCTUM ARCH GATEWAY (NORTH APPROACH)
    // Freestanding arch: two columns of alternating gold/amethyst,
    // gilded blackstone keystone, calcite inner lining, TOTEM curtain.
    // ================================================================
    public static class SanctumArchGateway extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftColumn = new ArrayList<>();
        private final List<BlockDisplayHandle> rightColumn = new ArrayList<>();
        private BlockDisplayHandle keystone;
        private final List<BlockDisplayHandle> calciteLining = new ArrayList<>();

        public SanctumArchGateway(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_arch_gateway", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location archCenter = center.clone().add(0, 0, -17);

            // Left column (X-2.5): 5 blocks alternating gold/amethyst
            for (int y = 0; y < 5; y++) {
                Material mat = (y % 2 == 0) ? Material.GOLD_BLOCK : Material.AMETHYST_BLOCK;
                Location loc = archCenter.clone().add(-2.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.9f, 1.0f, 0.9f).glow(180, 0, 200).interpolation(3, 0);
                leftColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right column (X+2.5): 5 blocks alternating gold/amethyst
            for (int y = 0; y < 5; y++) {
                Material mat = (y % 2 == 0) ? Material.GOLD_BLOCK : Material.AMETHYST_BLOCK;
                Location loc = archCenter.clone().add(2.5, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.9f, 1.0f, 0.9f).glow(180, 0, 200).interpolation(3, 0);
                rightColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            // Keystone at top center Y+9
            keystone = displayBuilder.spawnBlock(archCenter.clone().add(0, 9, 0), Material.POLISHED_BLACKSTONE);
            keystone.scale(2.5f, 1.2f, 0.4f).glow(128, 0, 255).interpolation(10, 0);
            spawnedEntities.add(keystone.entity());

            // Calcite inner lining: 7 small blocks along the arch curve
            for (int i = 0; i < 7; i++) {
                double t = (double) i / 6;
                double angle = Math.PI * t;
                double x = Math.cos(angle) * 2.0;
                double y = 5.0 + Math.sin(angle) * 4.0;
                Location loc = archCenter.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.3f, 0.3f, 0.3f).glow(0, 200, 255);
                calciteLining.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(archCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location archCenter = center.clone().add(0, 0, -17);

            // Keystone pulsing: scale Y between 1.2 and 1.4 on 6-sec (120-tick) cycle
            if (keystone != null) {
                float yScale = 1.3f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 60);
                keystone.scale(2.5f, yScale, 0.4f);
                keystone.interpolation(10, 0);
            }

            // Column top blocks pulse opposite phase to keystone
            float columnPulse = 1.0f + 0.1f * (float) Math.sin(ticksAlive * Math.PI / 60 + Math.PI);
            if (!leftColumn.isEmpty()) {
                leftColumn.get(leftColumn.size() - 1).scale(0.9f * columnPulse, 1.0f, 0.9f * columnPulse);
                leftColumn.get(leftColumn.size() - 1).interpolation(3, 0);
            }
            if (!rightColumn.isEmpty()) {
                rightColumn.get(rightColumn.size() - 1).scale(0.9f * columnPulse, 1.0f, 0.9f * columnPulse);
                rightColumn.get(rightColumn.size() - 1).interpolation(3, 0);
            }

            // TOTEM particles drifting down through the arch opening
            if (ticksAlive % 4 == 0) {
                Location keystonePos = archCenter.clone().add(0, 9.5, 0);
                archCenter.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, keystonePos, 3,
                        1.0, 0.3, 0.2, 0.01);
            }

            // Purple dust accent
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.purpleDust(archCenter.clone().add(0, 7, 0), 6, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SanctumArchGateway(plugin); }
    }

    // ================================================================
    // #7 -- VOID ORB SUSPENSION (CENTRAL ALTAR)
    // Large amethyst orb at Y+5 rotating and bobbing, with 4 satellite
    // blocks (gold, calcite, amethyst, polished blackstone) orbiting.
    // ================================================================
    public static class VoidOrbSuspension extends BlockDisplayAttack {

        private BlockDisplayHandle centralOrb;
        private final List<BlockDisplayHandle> satellites = new ArrayList<>();
        private static final Material[] SAT_MATS = {
                Material.GOLD_BLOCK, Material.CALCITE, Material.AMETHYST_BLOCK, Material.POLISHED_BLACKSTONE
        };

        public VoidOrbSuspension(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_orb_suspension", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central orb at Y+5
            Location orbLoc = center.clone().add(0, 5, 0);
            centralOrb = displayBuilder.spawnBlock(orbLoc, Material.AMETHYST_BLOCK);
            centralOrb.scale(1.8f, 1.8f, 1.8f).glow(128, 0, 255).interpolation(8, 0);
            spawnedEntities.add(centralOrb.entity());

            // 4 satellite blocks at orbit radius 1.2, same Y height
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                Location satLoc = orbLoc.clone().add(x, 0, z);
                BlockDisplayHandle sat = displayBuilder.spawnBlock(satLoc, SAT_MATS[i]);
                sat.scale(0.6f, 0.6f, 0.6f).glow(180, 0, 200).interpolation(3, 0);
                satellites.add(sat);
                spawnedEntities.add(sat.entity());
            }

            DisplayBuilder.playSound(orbLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            Location orbBase = center.clone().add(0, 5, 0);

            // Central orb: Y rotation at 1.0 deg/tick + vertical bob +/-0.4 on 5-sec cycle
            if (centralOrb != null) {
                float rotAngle = (float) Math.toRadians(ticksAlive * 1.0);
                centralOrb.rotate(rotAngle, 0, 1, 0);

                float yBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 50);
                Location newOrbLoc = orbBase.clone().add(0, yBob, 0);
                centralOrb.entity().teleport(newOrbLoc);
                centralOrb.interpolation(8, 0);
            }

            // Satellite orbit: 4-second revolution (80 ticks) + self-rotate at 2 deg/tick
            double orbitSpeed = (2 * Math.PI) / 80.0;
            for (int i = 0; i < satellites.size(); i++) {
                double baseAngle = (Math.PI / 2) * i;
                double angle = baseAngle + ticksAlive * orbitSpeed;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                float yBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 50);
                Location satLoc = orbBase.clone().add(x, yBob, z);
                satellites.get(i).entity().teleport(satLoc);

                float selfRot = (float) Math.toRadians(ticksAlive * 2.0);
                satellites.get(i).rotate(selfRot, 0, 1, 0);
                satellites.get(i).interpolation(3, 0);
            }

            // DRAGON_BREATH aura from orb center
            if (ticksAlive % 2 == 0) {
                float yBob = 0.4f * (float) Math.sin(ticksAlive * Math.PI / 50);
                Location particleLoc = orbBase.clone().add(0, yBob, 0);
                center.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLoc, 8,
                        2.0, 2.0, 2.0, 0);
            }

            // Resonate sound every 160 ticks (8 sec)
            if (ticksAlive % 160 == 0) {
                DisplayBuilder.playSound(orbBase, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidOrbSuspension(plugin); }
    }

    // ================================================================
    // #8 -- THRONE BACK FIN ARRAY
    // Seven vertical fin structures in an arc behind the dais. Each fin
    // is gold base + amethyst mid + calcite spire. Slow fanning oscillation.
    // ================================================================
    public static class ThroneBackFinArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> goldBases = new ArrayList<>();
        private final List<BlockDisplayHandle> amethystMids = new ArrayList<>();
        private final List<BlockDisplayHandle> calciteSpires = new ArrayList<>();

        private static final int FIN_COUNT = 7;
        private static final float ARC_SPREAD = (float) Math.toRadians(60); // 60 deg total arc

        public ThroneBackFinArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("throne_back_fin_array", AttackType.BLOCK_DISPLAY, 4));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Fin array behind dais, centered at Z-3
            Location arrayCenter = center.clone().add(0, 0, -3);

            for (int i = 0; i < FIN_COUNT; i++) {
                // Fan arc from -30 to +30 degrees around Y, facing north
                float finAngle = -ARC_SPREAD / 2 + (ARC_SPREAD / (FIN_COUNT - 1)) * i;

                double offsetX = Math.sin(finAngle) * 0.5;
                double offsetZ = -Math.cos(finAngle) * 0.5;

                // Gold base: Y+0 to Y+2
                Location baseLoc = arrayCenter.clone().add(offsetX, 0, offsetZ);
                BlockDisplayHandle gold = displayBuilder.spawnBlock(baseLoc, Material.GOLD_BLOCK);
                gold.scale(0.5f, 2.0f, 0.1f)
                        .rotate(finAngle, 0, 1, 0)
                        .glow(180, 0, 200).interpolation(12, 0);
                goldBases.add(gold);
                spawnedEntities.add(gold.entity());

                // Amethyst mid: Y+2 to Y+5
                Location midLoc = arrayCenter.clone().add(offsetX, 2, offsetZ);
                BlockDisplayHandle amethyst = displayBuilder.spawnBlock(midLoc, Material.AMETHYST_BLOCK);
                amethyst.scale(0.4f, 3.0f, 0.1f)
                        .rotate(finAngle, 0, 1, 0)
                        .glow(128, 0, 255).interpolation(12, 0);
                amethystMids.add(amethyst);
                spawnedEntities.add(amethyst.entity());

                // Calcite spire: Y+5 to Y+8
                Location spireLoc = arrayCenter.clone().add(offsetX, 5, offsetZ);
                BlockDisplayHandle calcite = displayBuilder.spawnBlock(spireLoc, Material.CALCITE);
                calcite.scale(0.2f, 3.0f, 0.1f)
                        .rotate(finAngle, 0, 1, 0)
                        .glow(0, 200, 255).interpolation(12, 0);
                calciteSpires.add(calcite);
                spawnedEntities.add(calcite.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fanning oscillation: center fin fixed, outer fins oscillate +/-3 degrees
            // with 1-second (20 tick) phase delay per fin
            for (int i = 0; i < FIN_COUNT; i++) {
                int distFromCenter = Math.abs(i - FIN_COUNT / 2);
                if (distFromCenter == 0) continue; // Center fin stays fixed

                float baseAngle = -ARC_SPREAD / 2 + (ARC_SPREAD / (FIN_COUNT - 1)) * i;
                float phaseDelay = distFromCenter * 20; // 1 sec per position
                float oscillation = (float) Math.toRadians(3.0) *
                        (float) Math.sin((ticksAlive - phaseDelay) * Math.PI / 80);
                float totalAngle = baseAngle + oscillation;

                goldBases.get(i).rotate(totalAngle, 0, 1, 0);
                goldBases.get(i).interpolation(12, 0);
                amethystMids.get(i).rotate(totalAngle, 0, 1, 0);
                amethystMids.get(i).interpolation(12, 0);
                calciteSpires.get(i).rotate(totalAngle, 0, 1, 0);
                calciteSpires.get(i).interpolation(12, 0);
            }

            // SPELL_WITCH particles from calcite spire tips
            if (ticksAlive % 5 == 0) {
                Location arrayCenter = center.clone().add(0, 0, -3);
                for (int i = 0; i < calciteSpires.size(); i++) {
                    Location tipLoc = calciteSpires.get(i).entity().getLocation().clone().add(0, 3, 0);
                    center.getWorld().spawnParticle(Particle.WITCH, tipLoc, 2, 0.3, 0.1, 0.3, 0.01);
                }
            }

            // Purple dust accent
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center.clone().add(0, 6, -3), 8, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThroneBackFinArray(plugin); }
    }
}
