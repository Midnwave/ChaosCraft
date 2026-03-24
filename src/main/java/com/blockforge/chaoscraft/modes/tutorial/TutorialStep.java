package com.blockforge.chaoscraft.modes.tutorial;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * A single step in a tutorial design.
 */
public class TutorialStep {

    private final String stepId;
    private final String description;
    private final TutorialStepType type;
    private final Set<Material> acceptedMaterials;
    private final int requiredCount;
    private final ItemStack reward; // nullable
    private final double moveDistance; // for PLAYER_MOVE type
    private final int moveMinY; // for PLAYER_MOVE type (reach Y level)

    private TutorialStep(Builder builder) {
        this.stepId = builder.stepId;
        this.description = builder.description;
        this.type = builder.type;
        this.acceptedMaterials = builder.acceptedMaterials;
        this.requiredCount = builder.requiredCount;
        this.reward = builder.reward;
        this.moveDistance = builder.moveDistance;
        this.moveMinY = builder.moveMinY;
    }

    public String getStepId() { return stepId; }
    public String getDescription() { return description; }
    public TutorialStepType getType() { return type; }
    public Set<Material> getAcceptedMaterials() { return acceptedMaterials; }
    public int getRequiredCount() { return requiredCount; }
    public ItemStack getReward() { return reward; }
    public double getMoveDistance() { return moveDistance; }
    public int getMoveMinY() { return moveMinY; }

    public boolean matchesMaterial(Material mat) {
        if (acceptedMaterials.isEmpty()) return true; // accept any
        return acceptedMaterials.contains(mat);
    }

    // ---- Builder ----

    public static Builder builder(String stepId, String description, TutorialStepType type) {
        return new Builder(stepId, description, type);
    }

    public static class Builder {
        private final String stepId;
        private final String description;
        private final TutorialStepType type;
        private Set<Material> acceptedMaterials = EnumSet.noneOf(Material.class);
        private int requiredCount = 1;
        private ItemStack reward = null;
        private double moveDistance = 0;
        private int moveMinY = -1;

        private Builder(String stepId, String description, TutorialStepType type) {
            this.stepId = stepId;
            this.description = description;
            this.type = type;
        }

        public Builder materials(Material... mats) {
            this.acceptedMaterials = EnumSet.noneOf(Material.class);
            for (Material m : mats) acceptedMaterials.add(m);
            return this;
        }

        public Builder materials(Set<Material> mats) {
            this.acceptedMaterials = EnumSet.copyOf(mats);
            return this;
        }

        public Builder count(int count) {
            this.requiredCount = count;
            return this;
        }

        public Builder reward(Material mat, int amount) {
            this.reward = new ItemStack(mat, amount);
            return this;
        }

        public Builder moveDistance(double dist) {
            this.moveDistance = dist;
            return this;
        }

        public Builder moveMinY(int y) {
            this.moveMinY = y;
            return this;
        }

        public TutorialStep build() {
            return new TutorialStep(this);
        }
    }

    // ---- Convenience: common material sets ----

    public static final Set<Material> ANY_LOG = EnumSet.of(
            Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
            Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
            Material.CRIMSON_STEM, Material.WARPED_STEM);

    public static final Set<Material> ANY_PLANKS = EnumSet.of(
            Material.OAK_PLANKS, Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS, Material.JUNGLE_PLANKS,
            Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS, Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS,
            Material.BAMBOO_PLANKS, Material.CRIMSON_PLANKS, Material.WARPED_PLANKS);

    public static final Set<Material> ANY_COOKED_MEAT = EnumSet.of(
            Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD, Material.COOKED_SALMON);

    public static final Set<Material> ANY_BED = EnumSet.of(
            Material.WHITE_BED, Material.RED_BED, Material.BLUE_BED, Material.BLACK_BED,
            Material.GREEN_BED, Material.YELLOW_BED, Material.ORANGE_BED, Material.PURPLE_BED,
            Material.CYAN_BED, Material.BROWN_BED, Material.GRAY_BED, Material.LIGHT_BLUE_BED,
            Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED, Material.PINK_BED);

    public static final Set<Material> ANY_BOAT = EnumSet.of(
            Material.OAK_BOAT, Material.BIRCH_BOAT, Material.SPRUCE_BOAT, Material.JUNGLE_BOAT,
            Material.ACACIA_BOAT, Material.DARK_OAK_BOAT, Material.MANGROVE_BOAT, Material.CHERRY_BOAT,
            Material.BAMBOO_RAFT);
}
