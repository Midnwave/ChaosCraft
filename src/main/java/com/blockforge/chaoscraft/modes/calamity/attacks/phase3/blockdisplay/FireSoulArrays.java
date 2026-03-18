package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 (4C) Block Display -- GROUP 4: FIRE AND SOUL ARRAYS
 * Structures 26-35: Ritualistic fire formations and soul memorials.
 * Tier 2 -- 80% HP: "The Island Remembers What It Burned"
 *
 * Dweller palette: crimson glow(200,0,50), orange glow(255,100,0), soul blue glow(0,150,255)
 * Materials: NETHERRACK, MAGMA_BLOCK, BASALT, SMOOTH_BASALT, BLACKSTONE,
 *            POLISHED_BLACKSTONE, CRIMSON_NYLIUM, NETHER_WART_BLOCK, SOUL_SOIL,
 *            CRACKED_STONE_BRICKS, OBSIDIAN, CRYING_OBSIDIAN
 *
 * Rules applied:
 * - NO status effects
 * - Always spawn straight (yaw=0, pitch=0)
 * - Damage in HP (4.0-12.0 range)
 * - AxisAngle4f for all rotations, never Quaternionf
 * - Impact damage via triggerImpactDamage()
 */
public final class FireSoulArrays {

    private FireSoulArrays() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BlazeRodRitualPyre(plugin));
        registry.register(new SwordInquisitionRing(plugin));
        registry.register(new SoulFireRitualHexagon(plugin));
        registry.register(new FireChargeConstellationDome(plugin));
        registry.register(new MagmaCreamFloorIgnition(plugin));
        registry.register(new NetheriteSwordPalisade(plugin));
        registry.register(new SoulFlameTridentArray(plugin));
        registry.register(new LavaBeamGridTower(plugin));
        registry.register(new ShieldWallObsidianFacade(plugin));
        registry.register(new TotemInfernoSpire(plugin));
    }

    // ================================================================
    // 26. BLAZE ROD RITUAL PYRE -- Circular formation of blaze rod
    //     columns surrounding a central fire charge, with soul soil base
    // ================================================================
    public static class BlazeRodRitualPyre extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> innerRods = new ArrayList<>();
        private final List<BlockDisplayHandle> soulSoilBase = new ArrayList<>();
        private final List<ItemDisplayHandle> outerSentinels = new ArrayList<>();
        private ItemDisplayHandle centralFireCharge;

        public BlazeRodRitualPyre(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blaze_rod_ritual_pyre", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 8 inner blaze rods in a ring, radius 4, scaled tall
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, 0, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.BLAZE_ROD));
                h.scale(1.0f, 3.5f, 1.0f)
                 .rotate((float) angle, 0, 1, 0)
                 .glow(255, 100, 0);
                innerRods.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central fire charge floating 2 blocks above ground
            Location fireChargeLoc = center.clone().add(0, 2, 0);
            centralFireCharge = displayBuilder.spawnItem(fireChargeLoc, new ItemStack(Material.FIRE_CHARGE));
            centralFireCharge.scale(4.0f, 4.0f, 4.0f).glow(200, 0, 50);
            spawnedEntities.add(centralFireCharge.entity());

            // 8 soul soil blocks at ground level between rod columns
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8 + Math.PI / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 150, 255);
                soulSoilBase.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 outer sentinel rods at 5.5 radius (NE, NW, SE, SW)
            double[][] sentinelAngles = {{0.785}, {2.356}, {3.927}, {5.498}};
            for (double[] sa : sentinelAngles) {
                double x = Math.cos(sa[0]) * 5.5;
                double z = Math.sin(sa[0]) * 5.5;
                Location loc = center.clone().add(x, 0, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.BLAZE_ROD));
                h.scale(1.0f, 2.0f, 1.0f).glow(255, 100, 0);
                outerSentinels.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Inner rods: Y-axis rotation at 0.6 deg/tick (~0.01047 rad/tick)
            float innerRot = ticksAlive * 0.01047f;
            // Bob: sinusoidal 90-tick cycle, 0.3 amplitude
            float bob = 0.3f * (float) Math.sin(ticksAlive * 2 * Math.PI / 90);

            for (int i = 0; i < innerRods.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8 + innerRot;
                double x = Math.cos(baseAngle) * 4.0;
                double z = Math.sin(baseAngle) * 4.0;
                innerRods.get(i).entity().teleport(center.clone().add(x, bob, z));
                innerRods.get(i).rotate(innerRot, 0, 1, 0);
                innerRods.get(i).interpolation(8, 0);
            }

            // Central fire charge: complex tumbling rotation
            if (centralFireCharge != null) {
                float yRot = ticksAlive * 0.0157f;  // 0.9 deg/tick
                float xRot = ticksAlive * 0.00349f;  // 0.2 deg/tick
                float combinedAngle = yRot + xRot;
                centralFireCharge.entity().teleport(center.clone().add(0, 2 + bob, 0));
                centralFireCharge.rotate(combinedAngle, 0.2f, 0.9f, 0.4f);
                centralFireCharge.interpolation(8, 0);
            }

            // Outer sentinels: opposite direction at 1.2 deg/tick
            float outerRot = -ticksAlive * 0.02094f;
            for (int i = 0; i < outerSentinels.size(); i++) {
                outerSentinels.get(i).rotate(outerRot, 0, 1, 0);
                outerSentinels.get(i).interpolation(8, 0);
            }

            // FLAME particles from rod tips
            if (ticksAlive % 4 == 0) {
                for (ItemDisplayHandle rod : innerRods) {
                    Location tip = rod.entity().getLocation().add(0, 3, 0);
                    w.spawnParticle(Particle.FLAME, tip, 3, 0.05, 0.5, 0.05, 0.01);
                }
            }

            // Soul fire octagonal ring laser lines
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < innerRods.size(); i++) {
                    Location from = innerRods.get(i).entity().getLocation().add(0, 3, 0);
                    Location to = innerRods.get((i + 1) % 8).entity().getLocation().add(0, 3, 0);
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 4, null);
                }
            }

            // LAVA disc spray from central fire charge
            if (ticksAlive % 7 == 0 && centralFireCharge != null) {
                Location fcLoc = centralFireCharge.entity().getLocation();
                w.spawnParticle(Particle.LAVA, fcLoc, 5, 1.5, 0.1, 1.5, 0);
            }

            // Lava explosion burst every 15 seconds (300 ticks)
            if (ticksAlive % 300 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 2, 0), 30, 2, 1, 2, 0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 15, 2.0);
            }

            // Ambient sound loop
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlazeRodRitualPyre(plugin); }
    }

    // ================================================================
    // 27. SWORD INQUISITION RING -- 12 netherite swords in an orbiting
    //     ring converging on a central shield, with blackstone columns
    // ================================================================
    public static class SwordInquisitionRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swordBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private BlockDisplayHandle centralShield;

        public SwordInquisitionRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sword_inquisition_ring", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(900);
            config.setCooldownTicks(450);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 sword positions in ring at 5-block radius, staggered heights
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 5.0;
                double z = Math.sin(angle) * 5.0;
                float height = (i % 2 == 0) ? 2.0f : 2.5f;
                Location loc = center.clone().add(x, height, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.2f, 2.8f, 0.2f)
                 .rotate((float) angle, 0, 1, 0)
                 .glow(200, 0, 50)
                 .interpolation(3, 0);
                swordBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 blackstone column pairs at 5.5 radius
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 5.5;
                double z = Math.sin(angle) * 5.5;
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(x, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    columnBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Central shield: polished blackstone block
            centralShield = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
            centralShield.scale(2.5f, 2.5f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(centralShield.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ring orbits on Y-axis at 0.4 deg/tick
            float ringRot = ticksAlive * 0.00698f;

            // Height swap: 120-tick cycle
            float heightPhase = (float) Math.sin(ticksAlive * 2 * Math.PI / 120);

            for (int i = 0; i < swordBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 12 + ringRot;
                double x = Math.cos(baseAngle) * 5.0;
                double z = Math.sin(baseAngle) * 5.0;
                float baseHeight = (i % 2 == 0) ? 2.0f : 2.5f;
                float heightAdj = (i % 2 == 0) ? heightPhase * 0.25f : -heightPhase * 0.25f;
                // Scale pulse: 2.8 -> 3.1 -> 2.8 over 40 ticks, offset per sword
                float scalePhase = (float) Math.sin((ticksAlive + i * 3.33) * 2 * Math.PI / 40);
                float scalePulse = 2.8f + scalePhase * 0.15f;

                Location target = center.clone().add(x, baseHeight + heightAdj, z);
                swordBlocks.get(i).entity().teleport(target);
                swordBlocks.get(i).scale(0.2f, scalePulse, 0.2f);
                swordBlocks.get(i).rotate((float) baseAngle, 0, 1, 0);
                swordBlocks.get(i).interpolation(3, 0);
            }

            // Central shield: counter-rotate at 0.8 deg/tick, X oscillation
            if (centralShield != null) {
                float shieldRot = -ticksAlive * 0.01396f;
                float xPendulum = (float) Math.sin(ticksAlive * 2 * Math.PI / 70) * 0.1396f;
                centralShield.rotate(shieldRot + xPendulum, 0.1f, 0.9f, 0);
                centralShield.interpolation(3, 0);
            }

            // FLAME convergence laser lines from each sword to center
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle sword : swordBlocks) {
                    Location tip = sword.entity().getLocation();
                    Location shieldLoc = center.clone().add(0, 2, 0);
                    DisplayBuilder.particleLine(tip, shieldLoc, Particle.FLAME, 3, null);
                }
            }

            // Crimson spore rain from tips
            if (ticksAlive % 6 == 0) {
                for (BlockDisplayHandle sword : swordBlocks) {
                    Location tip = sword.entity().getLocation();
                    w.spawnParticle(Particle.CRIMSON_SPORE, tip, 2, 0.1, 0.3, 0.1, 0);
                }
            }

            // Height swap shockwave every 120 ticks
            if (ticksAlive % 120 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.7f);
                DisplayBuilder.crimsonDust(center, 20, 3.0);
            }

            // Ambient sound
            if (ticksAlive % 200 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SwordInquisitionRing(plugin); }
    }

    // ================================================================
    // 28. SOUL FIRE RITUAL HEXAGON -- Flat hexagonal soul soil formation
    //     with totem-corner pillars and central magma cream orb
    // ================================================================
    public static class SoulFireRitualHexagon extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> soulTiles = new ArrayList<>();
        private final List<BlockDisplayHandle> totemCorners = new ArrayList<>();
        private final List<BlockDisplayHandle> crackedCollar = new ArrayList<>();
        private BlockDisplayHandle centralOrb;

        public SoulFireRitualHexagon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_ritual_hexagon", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(4.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 soul soil tiles in hexagonal pattern (3 rings of 6)
            double[][] hexOffsets = {
                // Outer ring (6)
                {3, 0, 0}, {1.5, 0, 2.6}, {-1.5, 0, 2.6}, {-3, 0, 0}, {-1.5, 0, -2.6}, {1.5, 0, -2.6},
                // Middle ring (6)
                {2, 0, 0}, {1, 0, 1.73}, {-1, 0, 1.73}, {-2, 0, 0}, {-1, 0, -1.73}, {1, 0, -1.73},
                // Inner ring (6)
                {1, 0, 0}, {0.5, 0, 0.87}, {-0.5, 0, 0.87}, {-1, 0, 0}, {-0.5, 0, -0.87}, {0.5, 0, -0.87}
            };
            for (double[] off : hexOffsets) {
                Location loc = center.clone().add(off[0], 0, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SOIL);
                h.scale(1.0f, 0.3f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                soulTiles.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 totem corner pillars at outer hexagon vertices
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * 3.3;
                double z = Math.sin(angle) * 3.3;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                h.scale(0.5f, 1.8f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                totemCorners.add(h);
                spawnedEntities.add(h.entity());
            }

            // Central magma cream orb (represented as magma block)
            centralOrb = displayBuilder.spawnBlock(center.clone().add(0, 1, 0), Material.MAGMA_BLOCK);
            centralOrb.scale(3.0f, 3.0f, 3.0f).glow(255, 100, 0).interpolation(3, 0);
            spawnedEntities.add(centralOrb.entity());

            // 6 cracked stone brick collar at midpoints
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6 + Math.PI / 6;
                double x = Math.cos(angle) * 3.8;
                double z = Math.sin(angle) * 3.8;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                crackedCollar.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Totem corners: Y-axis rotation at 0.5 deg/tick
            float totemRot = ticksAlive * 0.00873f;
            for (BlockDisplayHandle totem : totemCorners) {
                totem.rotate(totemRot, 0, 1, 0);
                totem.interpolation(3, 0);
            }

            // Central orb: vertical oscillation (1.0 to 1.8) and scale pulse (3.0 to 3.6)
            float orbPhase = (float) Math.sin(ticksAlive * 2 * Math.PI / 80);
            float orbY = 1.0f + orbPhase * 0.4f + 0.4f;
            float orbScale = 3.0f + orbPhase * 0.3f + 0.3f;
            if (centralOrb != null) {
                centralOrb.entity().teleport(center.clone().add(0, orbY, 0));
                centralOrb.scale(orbScale, orbScale, orbScale);
                centralOrb.interpolation(6, 0);
            }

            // Soul soil tiles: slow tilt on alternating axes
            float tiltAngle = (float) Math.sin(ticksAlive * 2 * Math.PI / 150) * 0.0873f;
            for (int i = 0; i < soulTiles.size(); i++) {
                if (i % 2 == 0) {
                    soulTiles.get(i).rotate(tiltAngle, 1, 0, 0);
                } else {
                    soulTiles.get(i).rotate(tiltAngle, 0, 0, 1);
                }
                soulTiles.get(i).interpolation(5, 0);
            }

            // Cracked stone collar: ultra-slow Y rotation
            float collarRot = ticksAlive * 0.00262f;
            for (BlockDisplayHandle collar : crackedCollar) {
                collar.rotate(collarRot, 0, 1, 0);
                collar.interpolation(5, 0);
            }

            // SOUL_FIRE_FLAME hexagonal perimeter laser lines
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location from = totemCorners.get(i).entity().getLocation().add(0, 1.5, 0);
                    Location to = totemCorners.get((i + 1) % 6).entity().getLocation().add(0, 1.5, 0);
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 5, null);
                }
            }

            // Soul fire columns rising from totems
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle totem : totemCorners) {
                    Location top = totem.entity().getLocation().add(0, 2, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, top, 2, 0.05, 0.8, 0.05, 0.01);
                }
            }

            // ASH drift from soul soil
            if (ticksAlive % 5 == 0) {
                w.spawnParticle(Particle.ASH, center.clone().add(0, 0.5, 0), 4, 3.5, 0.2, 3.5, 0);
            }

            // Wither pulse at orb peak (every 80 ticks): damage burst
            if (ticksAlive % 80 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
                DisplayBuilder.dustParticles(center.clone().add(0, orbY, 0), 15, 2.0, 0, 150, 255, 1.5f);
            }

            // Ambient soul sand valley loop
            if (ticksAlive % 120 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFireRitualHexagon(plugin); }
    }

    // ================================================================
    // 29. FIRE CHARGE CONSTELLATION DOME -- Hemispherical dome of fire
    //     charges orbiting at different heights with lava rain
    // ================================================================
    public static class FireChargeConstellationDome extends BlockDisplayAttack {

        private final List<ItemDisplayHandle> outerCharges = new ArrayList<>();
        private final List<ItemDisplayHandle> middleCharges = new ArrayList<>();
        private final List<ItemDisplayHandle> apexCharges = new ArrayList<>();
        private ItemDisplayHandle crownCharge;
        private final List<BlockDisplayHandle> groundAnchors = new ArrayList<>();

        public FireChargeConstellationDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_charge_constellation_dome", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(6.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(700);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 8 fire charges at 2-block height, radius 7
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 7.0;
                double z = Math.sin(angle) * 7.0;
                Location loc = center.clone().add(x, 2, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.FIRE_CHARGE));
                h.scale(2.0f, 2.0f, 2.0f).glow(255, 100, 0);
                outerCharges.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 7 fire charges at 4-block height, radius 5
            for (int i = 0; i < 7; i++) {
                double angle = (2 * Math.PI * i) / 7;
                double x = Math.cos(angle) * 5.0;
                double z = Math.sin(angle) * 5.0;
                Location loc = center.clone().add(x, 4, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.FIRE_CHARGE));
                h.scale(3.5f, 3.5f, 3.5f).glow(200, 0, 50);
                middleCharges.add(h);
                spawnedEntities.add(h.entity());
            }

            // Apex ring: 4 fire charges at 5-block height, radius 2
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                Location loc = center.clone().add(x, 5, z);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.FIRE_CHARGE));
                h.scale(2.5f, 2.5f, 2.5f).glow(255, 100, 0);
                apexCharges.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown fire charge at absolute apex
            crownCharge = displayBuilder.spawnItem(center.clone().add(0, 5.5, 0), new ItemStack(Material.FIRE_CHARGE));
            crownCharge.scale(5.0f, 5.0f, 5.0f).glow(200, 0, 50);
            spawnedEntities.add(crownCharge.entity());

            // 8 obsidian ground anchors
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 7.0;
                double z = Math.sin(angle) * 7.0;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                groundAnchors.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: orbit Y at 0.35 deg/tick
            float outerRot = ticksAlive * 0.00611f;
            for (int i = 0; i < outerCharges.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 8 + outerRot;
                float vertBob = 0.25f * (float) Math.sin((ticksAlive + i * 18) * 2 * Math.PI / 90);
                double x = Math.cos(baseAngle) * 7.0;
                double z = Math.sin(baseAngle) * 7.0;
                outerCharges.get(i).entity().teleport(center.clone().add(x, 2 + vertBob, z));
                outerCharges.get(i).interpolation(6, 0);
            }

            // Middle ring: orbit Y at -0.5 deg/tick (opposite)
            float middleRot = -ticksAlive * 0.00873f;
            for (int i = 0; i < middleCharges.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 7 + middleRot;
                float vertBob = 0.25f * (float) Math.sin((ticksAlive + i * 18) * 2 * Math.PI / 90);
                double x = Math.cos(baseAngle) * 5.0;
                double z = Math.sin(baseAngle) * 5.0;
                middleCharges.get(i).entity().teleport(center.clone().add(x, 4 + vertBob, z));
                middleCharges.get(i).interpolation(6, 0);
            }

            // Apex ring: orbit Y at 0.7 deg/tick
            float apexRot = ticksAlive * 0.01222f;
            for (int i = 0; i < apexCharges.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4 + apexRot;
                double x = Math.cos(baseAngle) * 2.0;
                double z = Math.sin(baseAngle) * 2.0;
                apexCharges.get(i).entity().teleport(center.clone().add(x, 5, z));
                apexCharges.get(i).interpolation(6, 0);
            }

            // Crown: Z-axis rotation at 1.0 deg/tick, scale pulse 5.0-6.0
            if (crownCharge != null) {
                float crownRot = ticksAlive * 0.01745f;
                float crownScale = 5.0f + 0.5f * (float) Math.sin(ticksAlive * 2 * Math.PI / 50) + 0.5f;
                crownCharge.rotate(crownRot, 0, 0, 1);
                crownCharge.scale(crownScale, crownScale, crownScale);
                crownCharge.interpolation(6, 0);
            }

            // LAVA rain from charges
            if (ticksAlive % 3 == 0) {
                for (ItemDisplayHandle ch : outerCharges) {
                    Location chLoc = ch.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, chLoc, 1, 0.3, 0, 0.3, 0);
                }
                for (ItemDisplayHandle ch : middleCharges) {
                    Location chLoc = ch.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, chLoc, 1, 0.3, 0, 0.3, 0);
                }
            }

            // FLAME from ground anchors rising
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle anchor : groundAnchors) {
                    Location aLoc = anchor.entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, aLoc, 2, 0.1, 0.5, 0.1, 0.02);
                }
            }

            // Dripping lava column from crown
            if (ticksAlive % 4 == 0 && crownCharge != null) {
                Location crownLoc = crownCharge.entity().getLocation();
                w.spawnParticle(Particle.DRIPPING_LAVA, crownLoc, 2, 0.1, 0, 0.1, 0);
            }

            // Crown pulse burst every 50 ticks
            if (ticksAlive % 50 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.4f);
                w.spawnParticle(Particle.LAVA, center.clone().add(0, 5.5, 0), 20, 2, 1, 2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FireChargeConstellationDome(plugin); }
    }

    // ================================================================
    // 30. MAGMA CREAM FLOOR IGNITION PATTERN -- 7x7 cross-shaped floor
    //     of magma blocks with soul sand corners and fire emitters
    // ================================================================
    public static class MagmaCreamFloorIgnition extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> crossBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> armTipOrbs = new ArrayList<>();
        private BlockDisplayHandle centerOrb;
        private final List<BlockDisplayHandle> torches = new ArrayList<>();

        public MagmaCreamFloorIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_cream_floor_ignition", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(840);
            config.setCooldownTicks(420);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 13 magma blocks forming a plus/cross pattern within 7x7
            int[][] crossPositions = {
                {0,0}, {1,0}, {2,0}, {3,0}, {-1,0}, {-2,0}, {-3,0},
                {0,1}, {0,2}, {0,3}, {0,-1}, {0,-2}, {0,-3}
            };
            for (int[] pos : crossPositions) {
                Location loc = center.clone().add(pos[0], 0, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.5f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                crossBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 soul sand blocks filling corners of 7x7 grid
            int[][] cornerPositions = {
                {1,1}, {1,-1}, {-1,1}, {-1,-1},
                {2,1}, {2,-1}, {-2,1}, {-2,-1},
                {1,2}, {1,-2}, {-1,2}, {-1,-2}
            };
            for (int[] pos : cornerPositions) {
                Location loc = center.clone().add(pos[0], 0, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SOUL_SAND);
                h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                cornerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 arm tip orbs at ends of the cross (magma block as orb)
            int[][] armTips = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
            for (int[] tip : armTips) {
                Location loc = center.clone().add(tip[0], 0.5, tip[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(2.2f, 2.2f, 2.2f).glow(255, 100, 0).interpolation(3, 0);
                armTipOrbs.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center orb
            centerOrb = displayBuilder.spawnBlock(center.clone().add(0, 1.2, 0), Material.MAGMA_BLOCK);
            centerOrb.scale(4.0f, 4.0f, 4.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(centerOrb.entity());

            // 4 blaze rod torches (represented as netherrack pillar)
            int[][] torchPos = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] tp : torchPos) {
                Location loc = center.clone().add(tp[0], 0, tp[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.3f, 1.5f, 0.3f).glow(255, 100, 0).interpolation(3, 0);
                torches.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Center orb: Y-axis 2.0 deg/tick, X-axis 0.5 deg/tick wobble
            if (centerOrb != null) {
                float yRot = ticksAlive * 0.0349f;
                float xWobble = ticksAlive * 0.00873f;
                centerOrb.rotate(yRot + xWobble, 0.2f, 0.9f, 0);
                centerOrb.interpolation(3, 0);
            }

            // Arm tip orbs: orbit slowly at 0.4 deg/tick around center
            float armOrbit = ticksAlive * 0.00698f;
            for (BlockDisplayHandle orb : armTipOrbs) {
                orb.rotate(armOrbit, 0, 1, 0);
                orb.interpolation(3, 0);
            }

            // Torch Y-axis spin at 1.0 deg/tick
            float torchRot = ticksAlive * 0.01745f;
            for (BlockDisplayHandle torch : torches) {
                torch.rotate(torchRot, 0, 1, 0);
                torch.interpolation(3, 0);
            }

            // Propagating scale wave through cross blocks
            for (int i = 0; i < crossBlocks.size(); i++) {
                int distance = Math.abs(i <= 6 ? i - 3 : (i - 7));
                float wavePhase = (float) Math.sin((ticksAlive - distance * 7) * 2 * Math.PI / 35);
                float scalePulse = 1.0f + wavePhase * 0.04f;
                crossBlocks.get(i).scale(scalePulse, 0.5f, scalePulse);
                crossBlocks.get(i).interpolation(3, 0);
            }

            // LAVA eruptions from magma floor tiles
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle block : crossBlocks) {
                    Location bLoc = block.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.LAVA, bLoc, 1, 0.3, 0.2, 0.3, 0);
                }
            }

            // FLAME spiral from center
            if (ticksAlive % 7 == 0 && centerOrb != null) {
                Location orbLoc = centerOrb.entity().getLocation();
                DisplayBuilder.particleRing(orbLoc, ticksAlive % 21 / 7.0, Particle.FLAME, 12, null);
            }

            // Soul fire from corner tiles
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle corner : cornerBlocks) {
                    Location cLoc = corner.entity().getLocation().add(0, 0.3, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, cLoc, 2, 0.1, 0.3, 0.1, 0.01);
                }
            }

            // Arm tip ignition burst every 280 ticks (14 ticks after center fires)
            if (ticksAlive % 35 == 14 && ticksAlive > 14) {
                for (BlockDisplayHandle orb : armTipOrbs) {
                    Location orbLoc = orb.entity().getLocation();
                    DisplayBuilder.playSound(orbLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.9f);
                    w.spawnParticle(Particle.FLAME, orbLoc, 10, 0.5, 0.5, 0.5, 0.05);
                }
            }

            // Ambient magma step
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.6f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaCreamFloorIgnition(plugin); }
    }

    // ================================================================
    // 31. NETHERITE SWORD PALISADE -- Semicircular arc of vertical blades
    //     with basalt/cracked stone sockets and shield flanks
    // ================================================================
    public static class NetheriteSwordPalisade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swordBlades = new ArrayList<>();
        private final List<BlockDisplayHandle> socketBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> stepPlatform = new ArrayList<>();
        private final List<BlockDisplayHandle> flankShields = new ArrayList<>();

        public NetheriteSwordPalisade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_sword_palisade", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(760);
            config.setCooldownTicks(380);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 9 swords in semicircular arc
            for (int i = 0; i < 9; i++) {
                double angle = Math.PI * i / 8; // 0 to PI semicircle
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.25f, 3.2f, 0.15f).glow(200, 0, 50).interpolation(3, 0);
                swordBlades.add(h);
                spawnedEntities.add(h.entity());
            }

            // Socket blocks: 8 basalt + 8 cracked stone between swords
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * (i + 0.5) / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                // Basalt at ground
                Location basaltLoc = center.clone().add(x, 0, z);
                BlockDisplayHandle basalt = displayBuilder.spawnBlock(basaltLoc, Material.BASALT);
                basalt.scale(0.8f, 1.0f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                socketBlocks.add(basalt);
                spawnedEntities.add(basalt.entity());
                // Cracked stone above
                Location crackedLoc = center.clone().add(x, 1, z);
                BlockDisplayHandle cracked = displayBuilder.spawnBlock(crackedLoc, Material.CRACKED_STONE_BRICKS);
                cracked.scale(0.8f, 1.0f, 0.8f).glow(200, 0, 50).interpolation(3, 0);
                socketBlocks.add(cracked);
                spawnedEntities.add(cracked.entity());
            }

            // 3 polished blackstone step platform behind arc
            for (int i = -1; i <= 1; i++) {
                Location loc = center.clone().add(i, 0, -1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(1.0f, 0.5f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
                stepPlatform.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 shield flanks on end swords
            for (int end = 0; end < 2; end++) {
                int idx = end == 0 ? 0 : 8;
                double angle = Math.PI * idx / 8;
                double x = Math.cos(angle) * 4.0;
                double z = Math.sin(angle) * 4.0;
                Location loc = center.clone().add(x, 1.5, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(2.0f, 2.0f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
                flankShields.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.7f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rippling forward lean wave through blades
            for (int i = 0; i < swordBlades.size(); i++) {
                int waveOffset = i * 5;
                int cyclePos = (ticksAlive - waveOffset) % 90;
                float leanAngle;
                if (cyclePos < 45) {
                    leanAngle = (float) Math.sin(cyclePos * Math.PI / 45) * 0.2094f;
                } else {
                    leanAngle = (float) Math.sin((90 - cyclePos) * Math.PI / 45) * 0.2094f;
                }
                // Independent Z-axis sway
                float zSway = (float) Math.sin(ticksAlive * 2 * Math.PI / 70) * 0.0698f;
                swordBlades.get(i).rotate(leanAngle + zSway, 1, 0, 0.3f);
                swordBlades.get(i).interpolation(3, 0);
            }

            // Flank shield rotation
            float shieldRot = ticksAlive * 0.00524f;
            for (BlockDisplayHandle shield : flankShields) {
                shield.rotate(shieldRot, 0, 1, 0);
                shield.interpolation(3, 0);
            }

            // FLAME laser lines between sword tips
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < swordBlades.size() - 1; i++) {
                    Location from = swordBlades.get(i).entity().getLocation().add(0, 3, 0);
                    Location to = swordBlades.get(i + 1).entity().getLocation().add(0, 3, 0);
                    DisplayBuilder.particleLine(from, to, Particle.FLAME, 4, null);
                }
            }

            // Soul fire dripping from blade tips
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle blade : swordBlades) {
                    Location tip = blade.entity().getLocation().add(0, 3.2, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, tip, 1, 0, -0.3, 0, 0.02);
                }
            }

            // Crimson spore drift from convex face
            if (ticksAlive % 6 == 0) {
                Location driftLoc = center.clone().add(0, 1.5, 2);
                w.spawnParticle(Particle.CRIMSON_SPORE, driftLoc, 2, 2, 0.5, 0.5, 0);
            }

            // Lean wave complete sound
            if (ticksAlive % 90 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
            }

            // Ambient basalt
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.4f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NetheriteSwordPalisade(plugin); }
    }

    // ================================================================
    // 32. SOUL FLAME TRIDENT ARRAY -- 7-trident starburst radiating
    //     from a central soul sand anchor with fire charge tips
    // ================================================================
    public static class SoulFlameTridentArray extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tridentShafts = new ArrayList<>();
        private final List<BlockDisplayHandle> tipCharges = new ArrayList<>();
        private BlockDisplayHandle soulAnchor;

        public SoulFlameTridentArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_flame_trident_array", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(880);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 7 trident shafts in starburst: 1 up, 2 at 30deg, 2 at 60deg, 2 horizontal
            double[][] tridentAngles = {
                {0, 1, 0},     // straight up
                {0.5, 0.87, 0}, {-0.5, 0.87, 0}, // 30 deg
                {0.87, 0.5, 0}, {-0.87, 0.5, 0},  // 60 deg
                {1, 0, 0}, {-1, 0, 0}              // horizontal
            };
            for (double[] dir : tridentAngles) {
                Location loc = center.clone().add(dir[0] * 1.5, dir[1] * 1.5, dir[2] * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_BASALT);
                h.scale(0.3f, 2.4f, 0.3f).glow(0, 150, 255).interpolation(3, 0);
                // Rotate to match direction
                float angle = (float) Math.atan2(dir[0], dir[1]);
                h.rotate(angle, 0, 0, 1);
                tridentShafts.add(h);
                spawnedEntities.add(h.entity());

                // Fire charge tip at each trident's end
                Location tipLoc = center.clone().add(dir[0] * 3.5, dir[1] * 3.5, dir[2] * 3.5);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.MAGMA_BLOCK);
                tip.scale(0.9f, 0.9f, 0.9f).glow(255, 100, 0).interpolation(3, 0);
                tipCharges.add(tip);
                spawnedEntities.add(tip.entity());
            }

            // Soul sand anchor at center
            soulAnchor = displayBuilder.spawnBlock(center, Material.SOUL_SOIL);
            soulAnchor.scale(1.0f, 1.0f, 1.0f).glow(0, 150, 255).interpolation(3, 0);
            spawnedEntities.add(soulAnchor.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Whole starburst Y-axis rotation at 0.5 deg/tick
            float yRot = ticksAlive * 0.00873f;
            // Vertical bob: 0.4 blocks, 100-tick sine
            float bob = 0.4f * (float) Math.sin(ticksAlive * 2 * Math.PI / 100);

            double[][] tridentDirs = {
                {0, 1, 0}, {0.5, 0.87, 0}, {-0.5, 0.87, 0},
                {0.87, 0.5, 0}, {-0.87, 0.5, 0}, {1, 0, 0}, {-1, 0, 0}
            };

            for (int i = 0; i < tridentShafts.size(); i++) {
                double[] dir = tridentDirs[i];
                // Rotate direction by yRot around Y
                double rotX = dir[0] * Math.cos(yRot) - dir[2] * Math.sin(yRot);
                double rotZ = dir[0] * Math.sin(yRot) + dir[2] * Math.cos(yRot);
                double rotY = dir[1];

                Location shaftLoc = center.clone().add(rotX * 1.5, rotY * 1.5 + bob, rotZ * 1.5);
                tridentShafts.get(i).entity().teleport(shaftLoc);

                // Flex oscillation on each trident's axis
                float flex = (float) Math.sin((ticksAlive + i * 10) * 2 * Math.PI / 55) * 0.1047f;
                float baseAngle = (float) Math.atan2(dir[0], dir[1]);
                tridentShafts.get(i).rotate(baseAngle + flex, 0, 0, 1);
                tridentShafts.get(i).interpolation(3, 0);

                // Tip position
                Location tipLoc = center.clone().add(rotX * 3.5, rotY * 3.5 + bob, rotZ * 3.5);
                tipCharges.get(i).entity().teleport(tipLoc);
                tipCharges.get(i).interpolation(3, 0);
            }

            // Vertical trident: extra Z-axis spin at 1.5 deg/tick
            if (!tridentShafts.isEmpty()) {
                float extraSpin = ticksAlive * 0.02618f;
                tridentShafts.get(0).rotate(extraSpin, 0, 0, 1);
                tridentShafts.get(0).interpolation(3, 0);
            }

            // Soul fire jets from each tip outward
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < tipCharges.size(); i++) {
                    Location tipLoc = tipCharges.get(i).entity().getLocation();
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, tipLoc, 3, 0.2, 0.2, 0.2, 0.03);
                }
            }

            // Soul light cage: laser lines between adjacent tips
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < tipCharges.size(); i++) {
                    Location from = tipCharges.get(i).entity().getLocation();
                    Location to = tipCharges.get((i + 1) % tipCharges.size()).entity().getLocation();
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 3, null);
                }
            }

            // Dripping lava from horizontal trident tips
            if (ticksAlive % 7 == 0 && tipCharges.size() >= 7) {
                for (int i = 5; i < 7; i++) {
                    Location dripLoc = tipCharges.get(i).entity().getLocation();
                    w.spawnParticle(Particle.DRIPPING_LAVA, dripLoc, 1, 0, 0, 0, 0);
                }
            }

            // Thunder sound every 10 seconds
            if (ticksAlive % 200 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulFlameTridentArray(plugin); }
    }

    // ================================================================
    // 33. LAVA BEAM GRID TOWER -- 3x3 grid of vertical towers connected
    //     by lava laser lines, with blaze rod emitters at each apex
    // ================================================================
    public static class LavaBeamGridTower extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> towerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> blazeRods = new ArrayList<>();
        private BlockDisplayHandle centerCharge;

        public LavaBeamGridTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_beam_grid_tower", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(960);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 grid of towers, 3-block spacing
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    double x = gx * 3.0;
                    double z = gz * 3.0;

                    // Tower base: 2 netherrack
                    for (int y = 0; y < 2; y++) {
                        Location loc = center.clone().add(x, y, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                        h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                        towerBlocks.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Tower top: 1 magma block
                    Location topLoc = center.clone().add(x, 2, z);
                    BlockDisplayHandle top = displayBuilder.spawnBlock(topLoc, Material.MAGMA_BLOCK);
                    top.scale(1.0f, 1.0f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                    towerBlocks.add(top);
                    spawnedEntities.add(top.entity());

                    // Blaze rod at apex (represented as thin netherrack column)
                    Location rodLoc = center.clone().add(x, 3, z);
                    BlockDisplayHandle rod = displayBuilder.spawnBlock(rodLoc, Material.NETHERRACK);
                    rod.scale(0.2f, 2.0f, 0.2f).glow(255, 100, 0).interpolation(3, 0);
                    blazeRods.add(rod);
                    spawnedEntities.add(rod.entity());
                }
            }

            // Center tower extra: fire charge (magma block, oversized)
            centerCharge = displayBuilder.spawnBlock(center.clone().add(0, 4.5, 0), Material.MAGMA_BLOCK);
            centerCharge.scale(2.5f, 2.5f, 2.5f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(centerCharge.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Ultra-slow Y rotation of entire grid: 0.2 deg/tick
            float gridRot = ticksAlive * 0.00349f;

            // Center fire charge: Y spin at 1.8 deg/tick
            if (centerCharge != null) {
                float centerRot = ticksAlive * 0.03142f;
                centerCharge.rotate(centerRot, 0, 1, 0);
                centerCharge.interpolation(10, 0);
            }

            // Blaze rod spin: 0.9 deg/tick
            float rodRot = ticksAlive * 0.0157f;
            for (BlockDisplayHandle rod : blazeRods) {
                rod.rotate(rodRot, 0, 1, 0);
                rod.interpolation(10, 0);
            }

            // Tower bob: different per position
            int towerIdx = 0;
            for (int gx = -1; gx <= 1; gx++) {
                for (int gz = -1; gz <= 1; gz++) {
                    boolean isCorner = (gx != 0 && gz != 0);
                    boolean isCenter = (gx == 0 && gz == 0);
                    float amplitude = isCenter ? 0.15f : (isCorner ? 0.2f : 0.3f);
                    int period = isCenter ? 100 : (isCorner ? 60 : 80);
                    float bobY = amplitude * (float) Math.sin(ticksAlive * 2 * Math.PI / period);

                    // Update the 3 tower blocks + blaze rod for this position
                    for (int y = 0; y < 3; y++) {
                        int idx = towerIdx * 3 + y;
                        if (idx < towerBlocks.size()) {
                            BlockDisplay bd = towerBlocks.get(idx).entity();
                            Location target = center.clone().add(gx * 3.0, y + bobY, gz * 3.0);
                            bd.teleport(target);
                        }
                    }
                    if (towerIdx < blazeRods.size()) {
                        Location rodTarget = center.clone().add(gx * 3.0, 3 + bobY, gz * 3.0);
                        blazeRods.get(towerIdx).entity().teleport(rodTarget);
                    }
                    towerIdx++;
                }
            }

            // LAVA laser lines between adjacent towers (horizontal + vertical = 12, diagonal corners = 4)
            if (ticksAlive % 3 == 0) {
                // Adjacent connections
                int[][] adjacencies = {
                    {0,1},{1,2},{3,4},{4,5},{6,7},{7,8}, // rows
                    {0,3},{1,4},{2,5},{3,6},{4,7},{5,8}, // columns
                    {0,4},{2,4},{4,6},{4,8}              // diagonals through center
                };
                for (int[] adj : adjacencies) {
                    if (adj[0] < blazeRods.size() && adj[1] < blazeRods.size()) {
                        Location from = blazeRods.get(adj[0]).entity().getLocation().add(0, 1, 0);
                        Location to = blazeRods.get(adj[1]).entity().getLocation().add(0, 1, 0);
                        DisplayBuilder.particleLine(from, to, Particle.LAVA, 4, null);
                    }
                }
            }

            // FLAME from tower bases
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < towerBlocks.size(); i += 3) {
                    Location baseLoc = towerBlocks.get(i).entity().getLocation().add(0, 0.5, 0);
                    w.spawnParticle(Particle.FLAME, baseLoc, 2, 0.2, 0.3, 0.2, 0.01);
                }
            }

            // Dripping lava from center charge
            if (ticksAlive % 5 == 0 && centerCharge != null) {
                Location chargeLoc = centerCharge.entity().getLocation();
                w.spawnParticle(Particle.DRIPPING_LAVA, chargeLoc, 1, 0.1, 0, 0.1, 0);
            }

            // Center fire charge rotation sound
            if (ticksAlive % 160 == 0 && ticksAlive > 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaBeamGridTower(plugin); }
    }

    // ================================================================
    // 34. SHIELD WALL OBSIDIAN FACADE -- Flat vertical wall of obsidian
    //     with shield grid, sword thrusts, and blaze rod battlements
    // ================================================================
    public static class ShieldWallObsidianFacade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> shieldDisplays = new ArrayList<>();
        private final List<BlockDisplayHandle> swordThrusts = new ArrayList<>();
        private final List<BlockDisplayHandle> battlements = new ArrayList<>();

        public ShieldWallObsidianFacade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shield_wall_obsidian_facade", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(5.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Wall backing: 9 wide, 5 tall
            // Core: 9x2 obsidian at center
            for (int x = -4; x <= 4; x++) {
                for (int y = 1; y <= 2; y++) {
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Top/bottom border: polished blackstone
            for (int x = -4; x <= 4; x++) {
                for (int y : new int[]{0, 3}) {
                    if (Math.abs(x) > 3 && y == 3) continue; // Skip upper corners for cracked stone
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // 4 cracked stone corners
            for (int x : new int[]{-4, 4}) {
                Location loc = center.clone().add(x, 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_STONE_BRICKS);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                wallBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 6 shields in 3x2 grid (polished blackstone representing shield face)
            for (int col = -2; col <= 2; col += 2) {
                for (int row = 1; row <= 2; row++) {
                    Location loc = center.clone().add(col, row, 0.3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                    h.scale(2.5f, 2.5f, 0.2f).glow(0, 150, 255).interpolation(3, 0);
                    shieldDisplays.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // 3 horizontal swords between shield pairs (blackstone representing netherite sword)
            for (int col = -1; col <= 1; col += 1) {
                Location loc = center.clone().add(col * 2, 1.5, 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.15f, 0.15f, 1.8f).glow(200, 0, 50).interpolation(3, 0);
                swordThrusts.add(h);
                spawnedEntities.add(h.entity());
            }

            // 3 blaze rod battlements at top (thin netherrack pillars)
            for (int col = -2; col <= 2; col += 2) {
                Location loc = center.clone().add(col, 4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERRACK);
                h.scale(0.25f, 2.0f, 0.25f).glow(255, 100, 0).interpolation(3, 0);
                battlements.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Shield wave: X-axis oscillation, staggered
            for (int i = 0; i < shieldDisplays.size(); i++) {
                float phase = (float) Math.sin((ticksAlive + i * 13) * 2 * Math.PI / 80) * 0.1745f;
                shieldDisplays.get(i).rotate(phase, 1, 0, 0);
                shieldDisplays.get(i).interpolation(3, 0);
            }

            // Sword thrust: translate outward on sine wave
            for (int i = 0; i < swordThrusts.size(); i++) {
                float thrust = 0.4f * (float) Math.sin((ticksAlive + i * 17) * 2 * Math.PI / 50);
                thrust = Math.max(0, thrust); // Only thrust forward
                Location base = center.clone().add((i - 1) * 2, 1.5, 0.5 + thrust);
                swordThrusts.get(i).entity().teleport(base);
                swordThrusts.get(i).interpolation(3, 0);
            }

            // Battlement Y-axis spin at 1.1 deg/tick
            float battRot = ticksAlive * 0.01920f;
            for (BlockDisplayHandle batt : battlements) {
                batt.rotate(battRot, 0, 1, 0);
                batt.interpolation(3, 0);
            }

            // Wall sway: Z-axis 2 degrees over 200 ticks
            float sway = (float) Math.sin(ticksAlive * 2 * Math.PI / 200) * 0.0349f;
            for (BlockDisplayHandle block : wallBlocks) {
                block.rotate(sway, 0, 0, 1);
                block.interpolation(5, 0);
            }

            // FLAME laser lines between adjacent shields
            if (ticksAlive % 4 == 0) {
                for (int i = 0; i < shieldDisplays.size() - 1; i++) {
                    Location from = shieldDisplays.get(i).entity().getLocation();
                    Location to = shieldDisplays.get(i + 1).entity().getLocation();
                    DisplayBuilder.particleLine(from, to, Particle.FLAME, 3, null);
                }
            }

            // Soul fire from battlements
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle batt : battlements) {
                    Location top = batt.entity().getLocation().add(0, 2, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, top, 2, 0.05, 0.5, 0.05, 0.01);
                }
            }

            // Crimson spore drift from wall face
            if (ticksAlive % 6 == 0) {
                Location driftLoc = center.clone().add(0, 1.5, 1);
                w.spawnParticle(Particle.CRIMSON_SPORE, driftLoc, 2, 3, 1, 0.3, 0);
            }

            // Sword thrust sound
            if (ticksAlive % 50 == 25) {
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.8f);
            }

            // Ambient
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShieldWallObsidianFacade(plugin); }
    }

    // ================================================================
    // 35. TOTEM INFERNO SPIRE -- 10-block-tall central spire with
    //     counter-helixing totem orbits and crown fire charge
    // ================================================================
    public static class TotemInfernoSpire extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spireCore = new ArrayList<>();
        private final List<BlockDisplayHandle> leftHelix = new ArrayList<>();
        private final List<BlockDisplayHandle> rightHelix = new ArrayList<>();
        private BlockDisplayHandle crownCharge;
        private final List<BlockDisplayHandle> basePlinth = new ArrayList<>();

        public TotemInfernoSpire(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("totem_inferno_spire", AttackType.BLOCK_DISPLAY, 3));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(1000);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spire core: 5 basalt (base) + 3 smooth basalt (mid) + 2 polished blackstone (cap)
            Material[] coreMats = {
                Material.BASALT, Material.BASALT, Material.BASALT,
                Material.BASALT, Material.BASALT,
                Material.SMOOTH_BASALT, Material.SMOOTH_BASALT, Material.SMOOTH_BASALT,
                Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE
            };
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, coreMats[i]);
                h.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                spireCore.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 totems on left spiral and 4 on right spiral (nether wart block representing totem)
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 4; // 45 degrees per tier
                float height = i * 1.25f + 1.0f;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location loc = center.clone().add(x, height, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                h.scale(0.8f, 1.6f, 0.8f).glow(255, 100, 0).interpolation(3, 0);
                leftHelix.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 4 + Math.PI; // Offset 180 degrees
                float height = i * 1.25f + 1.0f;
                double x = Math.cos(angle) * 1.5;
                double z = Math.sin(angle) * 1.5;
                Location loc = center.clone().add(x, height, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHER_WART_BLOCK);
                h.scale(0.8f, 1.6f, 0.8f).glow(0, 150, 255).interpolation(3, 0);
                rightHelix.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crown fire charge at top
            crownCharge = displayBuilder.spawnBlock(center.clone().add(0, 10.5, 0), Material.MAGMA_BLOCK);
            crownCharge.scale(3.0f, 3.0f, 3.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(crownCharge.entity());

            // 3x3 plinth at base
            for (int px = -1; px <= 1; px++) {
                for (int pz = -1; pz <= 1; pz++) {
                    Location loc = center.clone().add(px, -0.5, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(1.0f, 0.5f, 1.0f).glow(200, 0, 50).interpolation(3, 0);
                    basePlinth.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Left helix: orbit at 0.4 deg/tick
            float leftRot = ticksAlive * 0.00698f;
            for (int i = 0; i < leftHelix.size(); i++) {
                double baseAngle = i * Math.PI / 4 + leftRot;
                float height = i * 1.25f + 1.0f;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                // Vertical bob
                float bob = 0.15f * (float) Math.sin(ticksAlive * 2 * Math.PI / 120);
                leftHelix.get(i).entity().teleport(center.clone().add(x, height + bob, z));
                // Self rotation at 1.2 deg/tick
                float selfRot = ticksAlive * 0.02094f;
                leftHelix.get(i).rotate(selfRot, 0, 1, 0);
                leftHelix.get(i).interpolation(3, 0);
            }

            // Right helix: orbit at -0.4 deg/tick (counter-rotating)
            float rightRot = -ticksAlive * 0.00698f;
            for (int i = 0; i < rightHelix.size(); i++) {
                double baseAngle = i * Math.PI / 4 + Math.PI + rightRot;
                float height = i * 1.25f + 1.0f;
                double x = Math.cos(baseAngle) * 1.5;
                double z = Math.sin(baseAngle) * 1.5;
                float bob = 0.15f * (float) Math.sin(ticksAlive * 2 * Math.PI / 120);
                rightHelix.get(i).entity().teleport(center.clone().add(x, height + bob, z));
                float selfRot = ticksAlive * 0.02094f;
                rightHelix.get(i).rotate(selfRot, 0, 1, 0);
                rightHelix.get(i).interpolation(3, 0);
            }

            // Crown: Y-axis 2.0 deg/tick, scale pulse 3.0 to 4.0
            if (crownCharge != null) {
                float crownRot = ticksAlive * 0.0349f;
                float crownScale = 3.0f + 0.5f * (float) Math.sin(ticksAlive * 2 * Math.PI / 60) + 0.5f;
                crownCharge.rotate(crownRot, 0, 1, 0);
                crownCharge.scale(crownScale, crownScale, crownScale);
                crownCharge.interpolation(3, 0);
            }

            // Soul fire helix laser strands (connecting adjacent totems on each spiral)
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < leftHelix.size() - 1; i++) {
                    Location from = leftHelix.get(i).entity().getLocation();
                    Location to = leftHelix.get(i + 1).entity().getLocation();
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 5, null);
                }
                for (int i = 0; i < rightHelix.size() - 1; i++) {
                    Location from = rightHelix.get(i).entity().getLocation();
                    Location to = rightHelix.get(i + 1).entity().getLocation();
                    DisplayBuilder.particleLine(from, to, Particle.SOUL_FIRE_FLAME, 5, null);
                }
            }

            // FLAME column from crown
            if (ticksAlive % 3 == 0 && crownCharge != null) {
                Location crownLoc = crownCharge.entity().getLocation();
                w.spawnParticle(Particle.FLAME, crownLoc.clone().add(0, 1, 0), 5, 0.1, 1.0, 0.1, 0.02);
            }

            // LAVA burst from totem positions when facing outward
            if (ticksAlive % 20 == 0) {
                for (BlockDisplayHandle totem : leftHelix) {
                    Location tLoc = totem.entity().getLocation();
                    w.spawnParticle(Particle.LAVA, tLoc, 2, 0.3, 0.3, 0.3, 0);
                }
            }

            // Crown scale pulse fires burst
            if (ticksAlive % 60 == 30) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.6f);
                DisplayBuilder.dustParticles(center.clone().add(0, 10.5, 0), 15, 2.0, 200, 0, 50, 1.5f);
            }

            // Ambient
            if (ticksAlive % 140 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotemInfernoSpire(plugin); }
    }
}
