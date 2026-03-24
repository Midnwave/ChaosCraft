package com.blockforge.chaoscraft.modes.tutorial;

import org.bukkit.Material;

import java.util.*;

import static com.blockforge.chaoscraft.modes.tutorial.TutorialStep.*;
import static com.blockforge.chaoscraft.modes.tutorial.TutorialStepType.*;

/**
 * A tutorial design — a named sequence of steps.
 * All 20 designs are registered via createAllDesigns().
 */
public class TutorialDesign {

    private final String designId;
    private final String displayName;
    private final List<TutorialStep> steps;
    private final Material iconMaterial;

    public TutorialDesign(String designId, String displayName, Material icon, List<TutorialStep> steps) {
        this.designId = designId;
        this.displayName = displayName;
        this.iconMaterial = icon;
        this.steps = List.copyOf(steps);
    }

    public String getDesignId() { return designId; }
    public String getDisplayName() { return displayName; }
    public List<TutorialStep> getSteps() { return steps; }
    public Material getIconMaterial() { return iconMaterial; }
    public int getStepCount() { return steps.size(); }

    // ========================
    // All 20 Designs
    // ========================

    private static Map<String, TutorialDesign> ALL_DESIGNS;

    public static Map<String, TutorialDesign> getAllDesigns() {
        if (ALL_DESIGNS == null) ALL_DESIGNS = createAllDesigns();
        return ALL_DESIGNS;
    }

    public static TutorialDesign getRandom(Random random) {
        var designs = new ArrayList<>(getAllDesigns().values());
        return designs.get(random.nextInt(designs.size()));
    }

    public static TutorialDesign getByName(String name) {
        return getAllDesigns().get(name.toLowerCase());
    }

    private static Map<String, TutorialDesign> createAllDesigns() {
        Map<String, TutorialDesign> map = new LinkedHashMap<>();

        // 1. CLASSIC
        map.put("classic", new TutorialDesign("classic", "The Classic Path", Material.OAK_LOG, List.of(
                step("break_log", "Break a log", BLOCK_BREAK).materials(ANY_LOG).build(),
                step("craft_planks", "Craft planks", CRAFT_ITEM).materials(ANY_PLANKS).count(4).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("place_table", "Place the crafting table", BLOCK_PLACE).materials(Material.CRAFTING_TABLE).build(),
                step("craft_sticks", "Craft sticks", CRAFT_ITEM).materials(Material.STICK).count(4).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 3 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(3).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_iron", "Mine iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 2. TOOLSMITH
        map.put("toolsmith", new TutorialDesign("toolsmith", "The Toolsmith", Material.IRON_PICKAXE, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_planks", "Craft planks", CRAFT_ITEM).materials(ANY_PLANKS).count(12).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_sticks", "Craft sticks", CRAFT_ITEM).materials(Material.STICK).count(8).build(),
                step("craft_wood_sword", "Craft a wooden sword", CRAFT_ITEM).materials(Material.WOODEN_SWORD).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("craft_wood_axe", "Craft a wooden axe", CRAFT_ITEM).materials(Material.WOODEN_AXE).build(),
                step("mine_cobble", "Mine 9 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(9).build(),
                step("craft_stone_sword", "Craft a stone sword", CRAFT_ITEM).materials(Material.STONE_SWORD).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 3. BUILDER
        map.put("builder", new TutorialDesign("builder", "The Builder", Material.BRICKS, List.of(
                step("break_logs", "Break 5 logs", BLOCK_BREAK).materials(ANY_LOG).count(5).build(),
                step("craft_planks", "Craft 20 planks", CRAFT_ITEM).materials(ANY_PLANKS).count(20).build(),
                step("place_blocks", "Place 10 blocks", BLOCK_PLACE).count(10).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("build_pillar", "Place 3 more blocks", BLOCK_PLACE).count(3).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 8 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(8).build(),
                step("place_cobble", "Place 8 cobblestone", BLOCK_PLACE).materials(Material.COBBLESTONE).count(8).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 4. FARMER
        map.put("farmer", new TutorialDesign("farmer", "The Farmer", Material.WHEAT, List.of(
                step("break_log", "Break a log", BLOCK_BREAK).materials(ANY_LOG).build(),
                step("craft_sticks", "Craft sticks", CRAFT_ITEM).materials(Material.STICK).count(4).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_hoe", "Craft a wooden hoe", CRAFT_ITEM).materials(Material.WOODEN_HOE).build(),
                step("till_dirt", "Till 4 dirt blocks", PLAYER_INTERACT).materials(Material.FARMLAND).count(4).build(),
                step("plant_seeds", "Plant 4 seeds", BLOCK_PLACE).materials(Material.WHEAT).count(4).reward(Material.WHEAT_SEEDS, 4).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 3 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(3).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_iron", "Mine iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 5. EXPLORER
        map.put("explorer", new TutorialDesign("explorer", "The Explorer", Material.COMPASS, List.of(
                step("walk_50", "Walk 50 blocks", PLAYER_MOVE).moveDistance(50).build(),
                step("break_logs", "Break 2 logs", BLOCK_BREAK).materials(ANY_LOG).count(2).build(),
                step("craft_planks", "Craft planks", CRAFT_ITEM).materials(ANY_PLANKS).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("walk_more", "Walk another 50 blocks", PLAYER_MOVE).moveDistance(50).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_stone", "Mine 5 stone", BLOCK_BREAK).materials(Material.STONE).count(5).build(),
                step("climb_high", "Reach Y=100", PLAYER_MOVE).moveMinY(100).build(),
                step("mine_iron", "Mine iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 6. LUMBERJACK
        map.put("lumberjack", new TutorialDesign("lumberjack", "The Lumberjack", Material.OAK_LOG, List.of(
                step("break_logs", "Break 10 logs", BLOCK_BREAK).materials(ANY_LOG).count(10).build(),
                step("craft_planks", "Craft 40 planks", CRAFT_ITEM).materials(ANY_PLANKS).count(40).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_sticks", "Craft 16 sticks", CRAFT_ITEM).materials(Material.STICK).count(16).build(),
                step("craft_wood_axe", "Craft a wooden axe", CRAFT_ITEM).materials(Material.WOODEN_AXE).build(),
                step("break_more_logs", "Break 5 more logs", BLOCK_BREAK).materials(ANY_LOG).count(5).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 3 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(3).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 7. MINER
        map.put("miner", new TutorialDesign("miner", "The Miner", Material.STONE_PICKAXE, List.of(
                step("break_log", "Break a log", BLOCK_BREAK).materials(ANY_LOG).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_stone", "Mine 10 stone", BLOCK_BREAK).materials(Material.STONE).count(10).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_more_stone", "Mine 20 stone", BLOCK_BREAK).materials(Material.STONE).count(20).build(),
                step("mine_coal", "Mine 3 coal ore", BLOCK_BREAK).materials(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE).count(3).build(),
                step("mine_iron", "Mine 3 iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).count(3).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("smelt_iron", "Smelt 3 iron ingots", FURNACE_EXTRACT).materials(Material.IRON_INGOT).count(3).reward(Material.IRON_INGOT, 5).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 8. WARRIOR
        map.put("warrior", new TutorialDesign("warrior", "The Warrior", Material.STONE_SWORD, List.of(
                step("break_logs", "Break 2 logs", BLOCK_BREAK).materials(ANY_LOG).count(2).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_sword", "Craft a wooden sword", CRAFT_ITEM).materials(Material.WOODEN_SWORD).build(),
                step("kill_mobs", "Kill 2 mobs", ENTITY_KILL).count(2).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 5 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(5).build(),
                step("craft_stone_sword", "Craft a stone sword", CRAFT_ITEM).materials(Material.STONE_SWORD).build(),
                step("kill_more", "Kill 3 more mobs", ENTITY_KILL).count(3).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 9. SURVIVALIST
        map.put("survivalist", new TutorialDesign("survivalist", "The Survivalist", Material.COOKED_BEEF, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_planks", "Craft planks", CRAFT_ITEM).materials(ANY_PLANKS).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("kill_animal", "Kill an animal", ENTITY_KILL).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("cook_food", "Cook food", FURNACE_EXTRACT).materials(ANY_COOKED_MEAT).build(),
                step("eat_food", "Eat a food item", CONSUME_FOOD).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 5 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(5).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 10. ARMORSMITH
        map.put("armorsmith", new TutorialDesign("armorsmith", "The Armorsmith", Material.IRON_CHESTPLATE, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 8 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(8).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_iron", "Mine 5 iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).count(5).build(),
                step("smelt_iron", "Smelt 5 iron ingots", FURNACE_EXTRACT).materials(Material.IRON_INGOT).count(5).reward(Material.IRON_INGOT, 3).build(),
                step("craft_helmet", "Craft an iron helmet", CRAFT_ITEM).materials(Material.IRON_HELMET).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 11. ENCHANTER
        map.put("enchanter", new TutorialDesign("enchanter", "The Enchanter", Material.ENCHANTING_TABLE, List.of(
                step("break_logs", "Break 4 logs", BLOCK_BREAK).materials(ANY_LOG).count(4).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 4 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(4).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_obsidian", "Mine 4 obsidian", BLOCK_BREAK).materials(Material.OBSIDIAN).count(4).reward(Material.OBSIDIAN, 4).build(),
                step("craft_books", "Craft 3 books", CRAFT_ITEM).materials(Material.BOOK).count(3).reward(Material.BOOK, 3).build(),
                step("mine_diamonds", "Mine 2 diamond ore", BLOCK_BREAK).materials(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE).count(2).reward(Material.DIAMOND, 2).build(),
                step("craft_ench_table", "Craft an enchanting table", CRAFT_ITEM).materials(Material.ENCHANTING_TABLE).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).reward(Material.IRON_INGOT, 8).build()
        )));

        // 12. SMELTER
        map.put("smelter", new TutorialDesign("smelter", "The Smelter", Material.BLAST_FURNACE, List.of(
                step("break_logs", "Break 5 logs", BLOCK_BREAK).materials(ANY_LOG).count(5).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 8 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(8).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("mine_coal", "Mine 5 coal ore", BLOCK_BREAK).materials(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE).count(5).build(),
                step("smelt_stone", "Smelt 4 smooth stone", FURNACE_EXTRACT).materials(Material.STONE).count(4).build(),
                step("smelt_iron", "Smelt 3 iron ingots", FURNACE_EXTRACT).materials(Material.IRON_INGOT).count(3).build(),
                step("craft_blast", "Craft a blast furnace", CRAFT_ITEM).materials(Material.BLAST_FURNACE).reward(Material.IRON_INGOT, 5).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 13. FLETCHER
        map.put("fletcher", new TutorialDesign("fletcher", "The Fletcher", Material.BOW, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_sticks", "Craft 8 sticks", CRAFT_ITEM).materials(Material.STICK).count(8).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("kill_chickens", "Kill 3 chickens", ENTITY_KILL).count(3).reward(Material.FEATHER, 3).build(),
                step("mine_gravel", "Mine 3 gravel", BLOCK_BREAK).materials(Material.GRAVEL).count(3).reward(Material.FLINT, 3).build(),
                step("craft_arrows", "Craft 12 arrows", CRAFT_ITEM).materials(Material.ARROW).count(12).build(),
                step("craft_bow", "Craft a bow", CRAFT_ITEM).materials(Material.BOW).reward(Material.STRING, 3).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 14. DECORATOR
        map.put("decorator", new TutorialDesign("decorator", "The Decorator", Material.FLOWER_POT, List.of(
                step("break_logs", "Break 6 logs", BLOCK_BREAK).materials(ANY_LOG).count(6).build(),
                step("craft_planks", "Craft 24 planks", CRAFT_ITEM).materials(ANY_PLANKS).count(24).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_stairs", "Craft 4 stairs", CRAFT_ITEM).materials(Material.OAK_STAIRS).count(4).build(),
                step("craft_slabs", "Craft 6 slabs", CRAFT_ITEM).materials(Material.OAK_SLAB).count(6).build(),
                step("craft_fences", "Craft 3 fences", CRAFT_ITEM).materials(Material.OAK_FENCE).count(3).build(),
                step("place_deco", "Place 10 decorative blocks", BLOCK_PLACE).count(10).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 15. SHEPHERD
        map.put("shepherd", new TutorialDesign("shepherd", "The Shepherd", Material.WHITE_WOOL, List.of(
                step("break_logs", "Break 2 logs", BLOCK_BREAK).materials(ANY_LOG).count(2).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_shears", "Craft shears", CRAFT_ITEM).materials(Material.SHEARS).reward(Material.IRON_INGOT, 2).build(),
                step("shear_sheep", "Shear 4 sheep", PLAYER_INTERACT).count(4).build(),
                step("craft_bed", "Craft a bed", CRAFT_ITEM).materials(ANY_BED).build(),
                step("place_bed", "Place and use the bed", BLOCK_PLACE).materials(ANY_BED).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 3 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(3).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 16. FISHERMAN
        map.put("fisherman", new TutorialDesign("fisherman", "The Fisherman", Material.FISHING_ROD, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_rod", "Craft a fishing rod", CRAFT_ITEM).materials(Material.FISHING_ROD).reward(Material.STRING, 2).build(),
                step("catch_fish", "Catch 3 fish", PLAYER_FISH).count(3).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("cook_fish", "Cook 3 fish", FURNACE_EXTRACT).materials(Material.COOKED_COD, Material.COOKED_SALMON).count(3).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 17. REDSTONE ROOKIE
        map.put("redstone_rookie", new TutorialDesign("redstone_rookie", "Redstone Rookie", Material.REDSTONE, List.of(
                step("break_logs", "Break 3 logs", BLOCK_BREAK).materials(ANY_LOG).count(3).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 5 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(5).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_redstone", "Mine 4 redstone ore", BLOCK_BREAK).materials(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE).count(4).reward(Material.REDSTONE, 4).build(),
                step("craft_lever", "Craft a lever", CRAFT_ITEM).materials(Material.LEVER).build(),
                step("craft_piston", "Craft a piston", CRAFT_ITEM).materials(Material.PISTON).reward(Material.IRON_INGOT, 1).build(),
                step("place_redstone", "Place the piston and lever", BLOCK_PLACE).materials(Material.PISTON, Material.LEVER).count(2).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 18. POTIONEER
        map.put("potioneer", new TutorialDesign("potioneer", "The Potioneer", Material.BREWING_STAND, List.of(
                step("break_logs", "Break 2 logs", BLOCK_BREAK).materials(ANY_LOG).count(2).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 5 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(5).build(),
                step("smelt_glass", "Smelt 3 glass", FURNACE_EXTRACT).materials(Material.GLASS).count(3).reward(Material.SAND, 3).build(),
                step("craft_bottles", "Craft 3 glass bottles", CRAFT_ITEM).materials(Material.GLASS_BOTTLE).count(3).build(),
                step("craft_brewing", "Craft a brewing stand", CRAFT_ITEM).materials(Material.BREWING_STAND).reward(Material.BLAZE_ROD, 1).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 19. CRAFTER SUPREME
        map.put("crafter_supreme", new TutorialDesign("crafter_supreme", "Crafter Supreme", Material.CRAFTING_TABLE, List.of(
                step("break_logs", "Break 5 logs", BLOCK_BREAK).materials(ANY_LOG).count(5).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("craft_shovel", "Craft a wooden shovel", CRAFT_ITEM).materials(Material.WOODEN_SHOVEL).build(),
                step("craft_chest", "Craft a chest", CRAFT_ITEM).materials(Material.CHEST).build(),
                step("place_chest", "Place the chest", BLOCK_PLACE).materials(Material.CHEST).build(),
                step("craft_boat", "Craft a boat", CRAFT_ITEM).materials(ANY_BOAT).build(),
                step("mine_cobble", "Mine 8 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(8).build(),
                step("craft_furnace", "Craft a furnace", CRAFT_ITEM).materials(Material.FURNACE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        // 20. SPEEDRUNNER
        map.put("speedrunner", new TutorialDesign("speedrunner", "The Speedrunner", Material.DIAMOND, List.of(
                step("break_logs", "Break 2 logs", BLOCK_BREAK).materials(ANY_LOG).count(2).build(),
                step("craft_table", "Craft a crafting table", CRAFT_ITEM).materials(Material.CRAFTING_TABLE).build(),
                step("craft_wood_pick", "Craft a wooden pickaxe", CRAFT_ITEM).materials(Material.WOODEN_PICKAXE).build(),
                step("mine_cobble", "Mine 3 cobblestone", BLOCK_BREAK).materials(Material.COBBLESTONE).count(3).build(),
                step("craft_stone_pick", "Craft a stone pickaxe", CRAFT_ITEM).materials(Material.STONE_PICKAXE).build(),
                step("mine_iron", "Mine iron ore", BLOCK_BREAK).materials(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE).build(),
                step("smelt_iron", "Smelt an iron ingot", FURNACE_EXTRACT).materials(Material.IRON_INGOT).reward(Material.IRON_INGOT, 7).build(),
                step("craft_diamond", "Craft the Tutorial Diamond", CRAFT_ITEM).materials(Material.DIAMOND).build()
        )));

        return map;
    }

    // ---- Helper for concise step construction ----

    private static TutorialStep.Builder step(String id, String desc, TutorialStepType type) {
        return TutorialStep.builder(id, desc, type);
    }

    private static TutorialStep.Builder step(String id, String desc, TutorialStepType type, Material... mats) {
        return TutorialStep.builder(id, desc, type).materials(mats);
    }
}
