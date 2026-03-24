package com.blockforge.chaoscraft.modes.tutorial.attacks;

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
 * Tutorial Mode — TOOL & ITEM SHAPE ATTACKS
 * 10 block display attacks showing recognizable tool/item shapes.
 * Zero damage — visual demonstrations of Minecraft tools and crafting.
 *
 * Color palette:
 * - Iron tool: RGB(200, 200, 210)
 * - Wood handle: RGB(160, 110, 50)
 * - Diamond: RGB(80, 220, 240)
 * - Crafting: RGB(180, 140, 80)
 * - Fire glow: RGB(255, 180, 50)
 */
public final class ToolItemShapes {

    private ToolItemShapes() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FloatingPickaxe(plugin));
        registry.register(new GiantAxe(plugin));
        registry.register(new HoveringSword(plugin));
        registry.register(new CraftingTableExploded(plugin));
        registry.register(new FurnaceBlueprint(plugin));
        registry.register(new ShieldEmblem(plugin));
        registry.register(new BowAndArrow(plugin));
        registry.register(new ShovelDisplay(plugin));
        registry.register(new HelmetShowcase(plugin));
        registry.register(new TorchDisplay(plugin));
    }

    // ================================================================
    // 1. FLOATING PICKAXE — Pickaxe shape from iron and oak blocks,
    //    slowly rotates on Z axis as if being swung
    // ================================================================
    public static class FloatingPickaxe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pickBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public FloatingPickaxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_pickaxe", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pickaxe: 3-block head across top, 3-block handle going down
            // Head (iron)
            double[][] headOffsets = {
                {-1, 4.5, 0}, {0, 4.5, 0}, {1, 4.5, 0},  // head row
                {-1, 5, 0}, {1, 5, 0},                      // head top corners
            };
            for (double[] off : headOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                block.scale(0.5f, 0.5f, 0.5f)
                     .glow(200, 200, 210)
                     .interpolation(3, 0);
                pickBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }
            // Handle (oak)
            double[][] handleOffsets = {
                {0, 4, 0}, {0, 3.5, 0}, {0, 3, 0}, {0, 2.5, 0}, {0, 2, 0},
            };
            for (double[] off : handleOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.OAK_PLANKS);
                block.scale(0.35f, 0.5f, 0.35f)
                     .glow(160, 110, 50)
                     .interpolation(3, 0);
                pickBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_USE, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle swing on Z axis
            float swingAngle = (float) Math.sin(ticksAlive * 0.08) * 0.3f;
            float bob = (float) Math.sin(ticksAlive * 0.06) * 0.2f;

            for (int i = 0; i < pickBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                // Rotate around center pivot (0, 3.5)
                double relY = base[1] - 3.5;
                double relX = base[0];
                double rotX = relX * Math.cos(swingAngle) - relY * Math.sin(swingAngle);
                double rotY = relX * Math.sin(swingAngle) + relY * Math.cos(swingAngle);
                pickBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, rotY + 3.5 + bob, base[2]));
            }

            // Metallic sparkles
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4.5 + bob, 0), 3, 0.5, 200, 200, 210, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingPickaxe(plugin); }
    }

    // ================================================================
    // 2. GIANT AXE — Large axe shape, spruce handle with iron head,
    //    slow rhythmic chop motion
    // ================================================================
    public static class GiantAxe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> axeBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public GiantAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("giant_axe", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Axe head (iron, right side)
            double[][] headOffsets = {
                {0.5, 5, 0}, {1, 5, 0}, {1.5, 5, 0},   // top row
                {0.5, 4.5, 0}, {1, 4.5, 0}, {1.5, 4.5, 0}, // mid row
                {1, 4, 0}, {1.5, 4, 0},                  // bottom edge
            };
            for (double[] off : headOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                block.scale(0.5f, 0.5f, 0.5f)
                     .glow(200, 200, 210)
                     .interpolation(3, 0);
                axeBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Handle (spruce, going down from head)
            double[][] handleOffsets = {
                {0, 5, 0}, {0, 4.5, 0}, {0, 4, 0}, {0, 3.5, 0},
                {0, 3, 0}, {0, 2.5, 0}, {0, 2, 0},
            };
            for (double[] off : handleOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.SPRUCE_PLANKS);
                block.scale(0.3f, 0.5f, 0.3f)
                     .glow(160, 110, 50)
                     .interpolation(3, 0);
                axeBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_AXE_STRIP, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow chop motion: rotate around bottom of handle
            float chopAngle = (float) Math.sin(ticksAlive * 0.06) * 0.25f;

            for (int i = 0; i < axeBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double relY = base[1] - 2.0; // pivot at bottom of handle
                double relX = base[0];
                double rotX = relX * Math.cos(chopAngle) - relY * Math.sin(chopAngle);
                double rotY = relX * Math.sin(chopAngle) + relY * Math.cos(chopAngle);
                axeBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, rotY + 2.0, base[2]));
            }

            // Wood chip particles at swing apex
            if (ticksAlive % 10 == 0) {
                Location headLoc = c.clone().add(1, 4.5, 0);
                c.getWorld().spawnParticle(Particle.BLOCK, headLoc, 5, 0.3, 0.3, 0.3, 0.05,
                        Material.OAK_PLANKS.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GiantAxe(plugin); }
    }

    // ================================================================
    // 3. HOVERING SWORD — Diamond sword shape, floats and slowly
    //    rotates on Y axis with enchantment-like sparkles
    // ================================================================
    public static class HoveringSword extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> swordBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public HoveringSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hovering_sword", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sword blade (diamond blocks, vertical)
            double[][] bladeOffsets = {
                {0, 6.5, 0},   // tip
                {0, 6, 0},
                {0, 5.5, 0},
                {0, 5, 0},
                {0, 4.5, 0},   // blade base
            };
            for (double[] off : bladeOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.DIAMOND_BLOCK);
                block.scale(0.35f, 0.5f, 0.15f)
                     .glow(80, 220, 240)
                     .interpolation(3, 0);
                swordBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Cross-guard (gold)
            double[][] guardOffsets = {{-0.6, 4.2, 0}, {0.6, 4.2, 0}};
            for (double[] off : guardOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.GOLD_BLOCK);
                block.scale(0.35f, 0.25f, 0.25f)
                     .glow(255, 215, 0)
                     .interpolation(3, 0);
                swordBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Handle (dark oak)
            double[][] handleOffsets = {{0, 3.8, 0}, {0, 3.4, 0}, {0, 3, 0}};
            for (double[] off : handleOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.DARK_OAK_PLANKS);
                block.scale(0.25f, 0.4f, 0.25f)
                     .glow(80, 50, 25)
                     .interpolation(3, 0);
                swordBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Pommel (gold)
            BlockDisplayHandle pommel = displayBuilder.spawnBlock(
                    center.clone().add(0, 2.7, 0), Material.GOLD_BLOCK);
            pommel.scale(0.3f, 0.3f, 0.3f)
                  .glow(255, 215, 0)
                  .interpolation(3, 0);
            swordBlocks.add(pommel);
            spawnedEntities.add(pommel.entity());
            baseOffsets.add(new double[]{0, 2.7, 0});

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate whole sword around Y axis
            float angle = ticksAlive * 0.04f;
            float bob = (float) Math.sin(ticksAlive * 0.1) * 0.3f;

            for (int i = 0; i < swordBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                swordBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, base[1] + bob, rotZ));
            }

            // Enchantment sparkles along blade
            if (ticksAlive % 3 == 0) {
                double sparkleY = 4.5 + Math.random() * 2;
                DisplayBuilder.dustParticles(c.clone().add(0, sparkleY + bob, 0), 2, 0.15, 80, 220, 240, 0.8f);
            }

            // Sweep sound
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HoveringSword(plugin); }
    }

    // ================================================================
    // 4. CRAFTING TABLE EXPLODED — Crafting table "exploded view",
    //    9 blocks spread out in a 3x3, slowly rotating together
    // ================================================================
    public static class CraftingTableExploded extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gridBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public CraftingTableExploded(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crafting_table_exploded", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3x3 grid spaced apart to show "exploded" crafting grid
            Material[] gridMats = {
                Material.OAK_PLANKS, Material.CRAFTING_TABLE, Material.OAK_PLANKS,
                Material.CRAFTING_TABLE, Material.DIAMOND_BLOCK, Material.CRAFTING_TABLE,
                Material.OAK_PLANKS, Material.CRAFTING_TABLE, Material.OAK_PLANKS,
            };
            int idx = 0;
            for (int row = -1; row <= 1; row++) {
                for (int col = -1; col <= 1; col++) {
                    double x = col * 1.2;
                    double y = row * 1.2;
                    double[] off = {x, 3 + y, 0};
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(off[0], off[1], off[2]), gridMats[idx]);
                    block.scale(0.55f, 0.55f, 0.55f)
                         .glow(180, 140, 80)
                         .interpolation(3, 0);
                    gridBlocks.add(block);
                    spawnedEntities.add(block.entity());
                    baseOffsets.add(off);
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WOOD_PLACE, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.04f;
            // Breathing: blocks expand/contract
            float breathe = 1.0f + (float) Math.sin(ticksAlive * 0.1) * 0.15f;

            for (int i = 0; i < gridBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double bx = base[0] * breathe;
                double by = base[1] + (base[1] - 3) * (breathe - 1.0);
                // Rotate around Y
                double rotX = bx * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = bx * Math.sin(angle) + base[2] * Math.cos(angle);
                gridBlocks.get(i).entity().teleport(c.clone().add(rotX, by, rotZ));
            }

            // Crafting sparkle at center
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 4, 0.3, 180, 140, 80, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CraftingTableExploded(plugin); }
    }

    // ================================================================
    // 5. FURNACE BLUEPRINT — Furnace shape outlined in glowing blocks,
    //    with animated "flame" blocks inside pulsing
    // ================================================================
    public static class FurnaceBlueprint extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> flameBlocks = new ArrayList<>();

        public FurnaceBlueprint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("furnace_blueprint", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Furnace frame outline (cobblestone)
            // Bottom row
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue; // leave center for flame
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.7, 2, z * 0.7), Material.COBBLESTONE);
                    block.scale(0.5f, 0.5f, 0.5f)
                         .glow(140, 140, 140)
                         .interpolation(3, 0);
                    frameBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }
            // Top row
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(x * 0.7, 3.4, z * 0.7), Material.SMOOTH_STONE);
                    block.scale(0.5f, 0.5f, 0.5f)
                         .glow(160, 160, 170)
                         .interpolation(3, 0);
                    frameBlocks.add(block);
                    spawnedEntities.add(block.entity());
                }
            }

            // Flame blocks inside
            for (int i = 0; i < 3; i++) {
                double xOff = (i - 1) * 0.3;
                BlockDisplayHandle flame = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 2, 0), Material.ORANGE_CONCRETE);
                flame.scale(0.3f, 0.3f, 0.3f)
                     .glow(255, 180, 50)
                     .interpolation(3, 0);
                flameBlocks.add(flame);
                spawnedEntities.add(flame.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Flame blocks pulse and flicker
            for (int i = 0; i < flameBlocks.size(); i++) {
                float flicker = 0.3f + (float) Math.sin(ticksAlive * 0.2 + i * 1.5) * 0.15f;
                float yOff = (float) Math.sin(ticksAlive * 0.15 + i * 2.0) * 0.1f;
                flameBlocks.get(i).scale(flicker, flicker + 0.1f, flicker);
                double xOff = (i - 1) * 0.3;
                flameBlocks.get(i).entity().teleport(c.clone().add(xOff, 2 + yOff, 0));
            }

            // Fire particles
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 2.3, 0),
                        3, 0.2, 0.1, 0.2, 0.01);
            }

            // Crackle sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FurnaceBlueprint(plugin); }
    }

    // ================================================================
    // 6. SHIELD EMBLEM — Shield shape: rounded bottom, flat top,
    //    with a cross emblem, slowly rotates on Y axis
    // ================================================================
    public static class ShieldEmblem extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shieldBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public ShieldEmblem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shield_emblem", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shield body (oak planks) — wider at top, narrows at bottom
            double[][] bodyOffsets = {
                // Top row (wide)
                {-1, 5, 0}, {-0.5, 5, 0}, {0, 5, 0}, {0.5, 5, 0}, {1, 5, 0},
                // Upper mid
                {-1, 4.5, 0}, {-0.5, 4.5, 0}, {0, 4.5, 0}, {0.5, 4.5, 0}, {1, 4.5, 0},
                // Mid
                {-0.75, 4, 0}, {-0.25, 4, 0}, {0.25, 4, 0}, {0.75, 4, 0},
                // Lower mid
                {-0.5, 3.5, 0}, {0, 3.5, 0}, {0.5, 3.5, 0},
                // Bottom point
                {0, 3, 0},
            };

            for (double[] off : bodyOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.OAK_PLANKS);
                block.scale(0.45f, 0.45f, 0.2f)
                     .glow(160, 110, 50)
                     .interpolation(3, 0);
                shieldBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Cross emblem (iron blocks)
            double[][] crossOffsets = {
                {0, 5, 0.05}, {0, 4.5, 0.05}, {0, 4, 0.05}, {0, 3.5, 0.05}, // vertical
                {-0.5, 4.5, 0.05}, {0.5, 4.5, 0.05},                          // horizontal
            };
            for (double[] off : crossOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                block.scale(0.2f, 0.35f, 0.1f)
                     .glow(200, 200, 210)
                     .interpolation(3, 0);
                shieldBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_SHIELD_BLOCK, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.04f;

            for (int i = 0; i < shieldBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                shieldBlocks.get(i).entity().teleport(c.clone().add(rotX, base[1], rotZ));
            }

            // Glint particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4.5, 0), 3, 0.4, 200, 200, 210, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShieldEmblem(plugin); }
    }

    // ================================================================
    // 7. BOW AND ARROW — Curved bow shape with an arrow nocked,
    //    string draw animation
    // ================================================================
    public static class BowAndArrow extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bowBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowBlocks = new ArrayList<>();
        private final List<double[]> bowOffsets = new ArrayList<>();

        public BowAndArrow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bow_and_arrow", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Bow curve (dark oak) — semicircle
            int bowSegments = 10;
            for (int i = 0; i < bowSegments; i++) {
                double angle = -Math.PI / 2 + (Math.PI * i / (bowSegments - 1));
                double x = Math.cos(angle) * 1.5;
                double y = Math.sin(angle) * 1.5;
                double[] off = {x, 3.5 + y, 0};
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.DARK_OAK_PLANKS);
                block.scale(0.25f, 0.25f, 0.25f)
                     .glow(100, 65, 30)
                     .interpolation(3, 0);
                bowBlocks.add(block);
                spawnedEntities.add(block.entity());
                bowOffsets.add(off);
            }

            // String (white concrete, thin line connecting bow tips)
            for (int i = 0; i < 6; i++) {
                double y = -1.2 + (2.4 * i / 5.0);
                BlockDisplayHandle string = displayBuilder.spawnBlock(
                        center.clone().add(0, 3.5 + y, 0), Material.WHITE_CONCRETE);
                string.scale(0.08f, 0.35f, 0.08f)
                      .glow(240, 240, 255)
                      .interpolation(3, 0);
                bowBlocks.add(string);
                spawnedEntities.add(string.entity());
                bowOffsets.add(new double[]{0, 3.5 + y, 0});
            }

            // Arrow shaft (birch) + tip (iron)
            for (int i = 0; i < 4; i++) {
                double x = -0.3 + (i * 0.6);
                BlockDisplayHandle shaft = displayBuilder.spawnBlock(
                        center.clone().add(x, 3.5, 0.1), Material.BIRCH_PLANKS);
                shaft.scale(0.12f, 0.12f, 0.5f)
                     .glow(200, 190, 150)
                     .interpolation(3, 0);
                arrowBlocks.add(shaft);
                spawnedEntities.add(shaft.entity());
            }
            // Arrow tip
            BlockDisplayHandle tip = displayBuilder.spawnBlock(
                    center.clone().add(1.5, 3.5, 0.1), Material.IRON_BLOCK);
            tip.scale(0.15f, 0.15f, 0.15f)
               .glow(200, 200, 210)
               .interpolation(3, 0);
            arrowBlocks.add(tip);
            spawnedEntities.add(tip.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Draw animation: string pulls back slightly
            float draw = (float) Math.sin(ticksAlive * 0.08) * 0.3f;

            // Move string blocks back
            int bowCount = 10; // first 10 are bow curve
            for (int i = bowCount; i < bowBlocks.size(); i++) {
                double[] base = bowOffsets.get(i);
                bowBlocks.get(i).entity().teleport(
                        c.clone().add(base[0] - draw, base[1], base[2]));
            }

            // Move arrow with string
            for (BlockDisplayHandle arrow : arrowBlocks) {
                Location loc = arrow.entity().getLocation();
                // Arrow slides with string draw
            }

            // Tension particles on string
            if (ticksAlive % 8 == 0 && draw < -0.1f) {
                DisplayBuilder.dustParticles(c.clone().add(-draw, 3.5, 0), 2, 0.1, 240, 240, 255, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BowAndArrow(plugin); }
    }

    // ================================================================
    // 8. SHOVEL DISPLAY — Large shovel shape, iron head + spruce handle,
    //    gentle rocking motion as if digging
    // ================================================================
    public static class ShovelDisplay extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shovelBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public ShovelDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shovel_display", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shovel head (iron, rounded)
            double[][] headOffsets = {
                {0, 5.5, 0},           // top center
                {-0.3, 5.2, 0}, {0.3, 5.2, 0}, // mid
                {-0.4, 4.9, 0}, {0, 4.9, 0}, {0.4, 4.9, 0}, // wide
            };
            for (double[] off : headOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                block.scale(0.4f, 0.35f, 0.2f)
                     .glow(200, 200, 210)
                     .interpolation(3, 0);
                shovelBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            // Handle (spruce)
            for (int y = 0; y < 5; y++) {
                double[] off = {0, 4.5 - y * 0.5, 0};
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.SPRUCE_PLANKS);
                block.scale(0.2f, 0.45f, 0.2f)
                     .glow(130, 90, 40)
                     .interpolation(3, 0);
                shovelBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_SHOVEL_FLATTEN, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rocking/digging motion around bottom pivot
            float rockAngle = (float) Math.sin(ticksAlive * 0.07) * 0.2f;

            for (int i = 0; i < shovelBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double pivotY = 2.0;
                double relY = base[1] - pivotY;
                double relX = base[0];
                double rotX = relX * Math.cos(rockAngle) - relY * Math.sin(rockAngle);
                double rotY = relX * Math.sin(rockAngle) + relY * Math.cos(rockAngle);
                shovelBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, rotY + pivotY, base[2]));
            }

            // Dirt particles at digging point
            if (ticksAlive % 12 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 2, 0),
                        4, 0.3, 0.1, 0.3, 0.05, Material.DIRT.createBlockData());
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShovelDisplay(plugin); }
    }

    // ================================================================
    // 9. HELMET SHOWCASE — Helmet shape (like a dome) made of iron
    //    blocks, slowly rotates to show all angles
    // ================================================================
    public static class HelmetShowcase extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> helmetBlocks = new ArrayList<>();
        private final List<double[]> baseOffsets = new ArrayList<>();

        public HelmetShowcase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("helmet_showcase", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Helmet dome: half-sphere of iron blocks
            // Top crown
            double[][] domeOffsets = {
                {0, 4.5, 0},          // very top
                {0.4, 4.2, 0}, {-0.4, 4.2, 0}, {0, 4.2, 0.4}, {0, 4.2, -0.4},
                // Middle ring (wider)
                {0.7, 3.8, 0}, {-0.7, 3.8, 0}, {0, 3.8, 0.7}, {0, 3.8, -0.7},
                {0.5, 3.8, 0.5}, {-0.5, 3.8, 0.5}, {0.5, 3.8, -0.5}, {-0.5, 3.8, -0.5},
                // Rim (widest, open at front)
                {0.8, 3.4, 0}, {-0.8, 3.4, 0}, {0, 3.4, 0.8},
                {0.6, 3.4, 0.6}, {-0.6, 3.4, 0.6},
                // Nose guard
                {0, 3.6, -0.8},
            };

            for (double[] off : domeOffsets) {
                BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(off[0], off[1], off[2]), Material.IRON_BLOCK);
                block.scale(0.35f, 0.35f, 0.35f)
                     .glow(200, 200, 210)
                     .interpolation(3, 0);
                helmetBlocks.add(block);
                spawnedEntities.add(block.entity());
                baseOffsets.add(off);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float angle = ticksAlive * 0.04f;
            float bob = (float) Math.sin(ticksAlive * 0.08) * 0.15f;

            for (int i = 0; i < helmetBlocks.size(); i++) {
                double[] base = baseOffsets.get(i);
                double rotX = base[0] * Math.cos(angle) - base[2] * Math.sin(angle);
                double rotZ = base[0] * Math.sin(angle) + base[2] * Math.cos(angle);
                helmetBlocks.get(i).entity().teleport(
                        c.clone().add(rotX, base[1] + bob, rotZ));
            }

            // Metallic glint
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4.5 + bob, 0), 3, 0.3, 220, 220, 230, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HelmetShowcase(plugin); }
    }

    // ================================================================
    // 10. TORCH DISPLAY — Oversized torch: oak stick with glowstone
    //     flame top, fire particles, warm light glow
    // ================================================================
    public static class TorchDisplay extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> torchBlocks = new ArrayList<>();
        private BlockDisplayHandle flameBlock;

        public TorchDisplay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("torch_display", AttackType.BLOCK_DISPLAY, 1, "modes/tutorial/attacks"));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Stick (oak planks, 5 segments)
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle stick = displayBuilder.spawnBlock(
                        center.clone().add(0, 2 + y * 0.6, 0), Material.OAK_PLANKS);
                stick.scale(0.25f, 0.55f, 0.25f)
                     .glow(160, 110, 50)
                     .interpolation(3, 0);
                torchBlocks.add(stick);
                spawnedEntities.add(stick.entity());
            }

            // Flame top (glowstone)
            flameBlock = displayBuilder.spawnBlock(
                    center.clone().add(0, 5, 0), Material.GLOWSTONE);
            flameBlock.scale(0.4f, 0.5f, 0.4f)
                      .glow(255, 180, 50)
                      .brightness(15, 15)
                      .interpolation(3, 0);
            torchBlocks.add(flameBlock);
            spawnedEntities.add(flameBlock.entity());

            // Warm glow accent blocks
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                BlockDisplayHandle glow = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 0.3, 5.2, Math.sin(angle) * 0.3),
                        Material.ORANGE_CONCRETE);
                glow.scale(0.15f, 0.2f, 0.15f)
                    .glow(255, 200, 80)
                    .interpolation(3, 0);
                torchBlocks.add(glow);
                spawnedEntities.add(glow.entity());
            }

            DisplayBuilder.playSound(center, Sound.ITEM_FLINTANDSTEEL_USE, 0.8f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Flame flicker
            float flicker = 0.4f + (float) Math.sin(ticksAlive * 0.25) * 0.1f
                          + (float) Math.sin(ticksAlive * 0.4) * 0.05f;
            flameBlock.scale(flicker, 0.5f + flicker * 0.2f, flicker);

            // Fire particles
            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, c.clone().add(0, 5.4, 0),
                        2, 0.05, 0.1, 0.05, 0.01);
            }

            // Smoke particles
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 5.6, 0),
                        1, 0.05, 0.1, 0.05, 0.005);
            }

            // Warm light dust ring
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 3, 0), 2.0, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(255, 180, 50), 0.6f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TorchDisplay(plugin); }
    }
}
