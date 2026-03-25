package com.blockforge.chaoscraft.modes.tutorial;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player tutorial progress through their assigned design.
 */
public class TutorialTracker {

    private final ChaosCraftPlugin plugin;
    private final TutorialConfig config;
    private final Map<UUID, PlayerProgress> progress = new ConcurrentHashMap<>();

    public TutorialTracker(ChaosCraftPlugin plugin, TutorialConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================
    // Lifecycle
    // ========================

    public void startTutorial(Player player, TutorialDesign design) {
        PlayerProgress pp = new PlayerProgress(design);
        progress.put(player.getUniqueId(), pp);
        showCurrentStep(player, pp);
        plugin.debug("[Tutorial] " + player.getName() + " started design: " + design.getDisplayName()
                + " (" + design.getStepCount() + " steps)");
    }

    public void cleanup() {
        progress.clear();
    }

    // ========================
    // Progress tracking
    // ========================

    public TutorialStep getCurrentStep(Player player) {
        PlayerProgress pp = progress.get(player.getUniqueId());
        if (pp == null || pp.completed) return null;
        return pp.design.getSteps().get(pp.currentStepIndex);
    }

    public boolean isCompleted(Player player) {
        PlayerProgress pp = progress.get(player.getUniqueId());
        return pp != null && pp.completed;
    }

    public boolean isTracking(Player player) {
        return progress.containsKey(player.getUniqueId());
    }

    /**
     * Increment progress on the current step. Returns true if the step was completed.
     */
    public boolean incrementProgress(Player player, TutorialStepType type, Material material) {
        PlayerProgress pp = progress.get(player.getUniqueId());
        if (pp == null || pp.completed) return false;

        TutorialStep step = pp.design.getSteps().get(pp.currentStepIndex);
        if (step.getType() != type) return false;
        if (!step.matchesMaterial(material)) return false;

        pp.currentStepProgress++;
        plugin.debug("[Tutorial] " + player.getName() + " progress: " + step.getStepId()
                + " " + pp.currentStepProgress + "/" + step.getRequiredCount());

        if (pp.currentStepProgress >= step.getRequiredCount()) {
            advanceStep(player, pp);
            return true;
        }

        // Update actionbar with progress
        if (config.showStepActionbar()) {
            showActionbarProgress(player, step, pp.currentStepProgress);
        }
        return false;
    }

    /**
     * Increment progress for PLAYER_MOVE steps using distance.
     */
    public boolean incrementMoveProgress(Player player, Location from, Location to) {
        PlayerProgress pp = progress.get(player.getUniqueId());
        if (pp == null || pp.completed) return false;

        TutorialStep step = pp.design.getSteps().get(pp.currentStepIndex);
        if (step.getType() != TutorialStepType.PLAYER_MOVE) return false;

        // Y threshold check
        if (step.getMoveMinY() > 0) {
            if (to.getBlockY() >= step.getMoveMinY()) {
                pp.currentStepProgress = step.getRequiredCount();
                advanceStep(player, pp);
                return true;
            }
            return false;
        }

        // Distance check
        if (step.getMoveDistance() > 0) {
            double dist = from.distance(to);
            pp.moveDistanceAccumulated += dist;
            if (pp.moveDistanceAccumulated >= step.getMoveDistance()) {
                pp.currentStepProgress = step.getRequiredCount();
                advanceStep(player, pp);
                return true;
            }
            if (config.showStepActionbar()) {
                int pct = (int) ((pp.moveDistanceAccumulated / step.getMoveDistance()) * 100);
                player.sendActionBar(Component.text("Walk: " + pct + "% complete", NamedTextColor.YELLOW));
            }
        }
        return false;
    }

    // ========================
    // Step advancement
    // ========================

    private void advanceStep(Player player, PlayerProgress pp) {
        TutorialStep completedStep = pp.design.getSteps().get(pp.currentStepIndex);

        // Give reward item if configured
        if (completedStep.getReward() != null && config.giveItemsOnStep()) {
            player.getInventory().addItem(completedStep.getReward().clone());
        }

        // Play completion sound
        String soundId = config.getStepCompleteSound();
        if (!soundId.isEmpty()) {
            player.playSound(player.getLocation(), soundId, 1.0f, 1.2f);
        }

        int stepNum = pp.currentStepIndex + 1;
        int totalSteps = pp.design.getStepCount();

        // Check if this was the final step
        pp.currentStepIndex++;
        pp.currentStepProgress = 0;
        pp.moveDistanceAccumulated = 0;

        if (pp.currentStepIndex >= pp.design.getStepCount()) {
            // Tutorial completed!
            pp.completed = true;
            onTutorialComplete(player, pp);
            return;
        }

        // Show step completion title
        if (config.showStepTitles()) {
            player.showTitle(Title.title(
                    Component.text("Step " + stepNum + "/" + totalSteps + " Complete!", NamedTextColor.GOLD),
                    Component.text(completedStep.getDescription(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(200))
            ));
        }

        // Show next step
        showCurrentStep(player, pp);
        plugin.debug("[Tutorial] " + player.getName() + " completed step " + stepNum + "/" + totalSteps);
    }

    private void onTutorialComplete(Player player, PlayerProgress pp) {
        // Celebration title
        player.showTitle(Title.title(
                Component.text("TUTORIAL COMPLETE!", NamedTextColor.GREEN, TextDecoration.BOLD),
                Component.text(pp.design.getDisplayName(), NamedTextColor.GOLD),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(4), Duration.ofMillis(500))
        ));

        // Celebration sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Firework particles
        player.getWorld().spawnParticle(org.bukkit.Particle.FIREWORK, player.getLocation().clone().add(0, 2, 0),
                30, 1, 1, 1, 0.1);

        // Tutorial completion bonus is now granted as part of mode end rewards
        // via ModeResultsService.grantBonus("tutorial-complete") — see TutorialMode / ModeManager

        plugin.getLogger().info("[Tutorial] " + player.getName() + " completed the tutorial! ("
                + pp.design.getDisplayName() + ")");
    }

    // ========================
    // Display
    // ========================

    private void showCurrentStep(Player player, PlayerProgress pp) {
        if (pp.completed) return;
        TutorialStep step = pp.design.getSteps().get(pp.currentStepIndex);
        int stepNum = pp.currentStepIndex + 1;
        int total = pp.design.getStepCount();

        if (config.showStepActionbar()) {
            showActionbarProgress(player, step, 0);
        }

        // Always send a chat message showing the new current step
        String progressSuffix = step.getRequiredCount() > 1
                ? " (0/" + step.getRequiredCount() + ")"
                : "";
        player.sendMessage(
                Component.text("[Tutorial] ", NamedTextColor.YELLOW)
                        .append(Component.text("Step " + stepNum + "/" + total + ": ", NamedTextColor.WHITE))
                        .append(Component.text(step.getDescription() + progressSuffix, NamedTextColor.GREEN))
        );

        if (config.showStepTitles() && pp.currentStepIndex > 0) {
            // Don't show title for first step (it was just shown by startTutorial)
        } else if (pp.currentStepIndex == 0 && config.showStepTitles()) {
            player.showTitle(Title.title(
                    Component.text(pp.design.getDisplayName(), NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("Step 1/" + total + ": " + step.getDescription(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            ));
        }
    }

    private void showActionbarProgress(Player player, TutorialStep step, int currentProgress) {
        PlayerProgress pp = progress.get(player.getUniqueId());
        if (pp == null) return;
        int stepNum = pp.currentStepIndex + 1;
        int total = pp.design.getStepCount();
        String progressText = step.getRequiredCount() > 1
                ? " (" + currentProgress + "/" + step.getRequiredCount() + ")"
                : "";
        player.sendActionBar(Component.text(
                "Step " + stepNum + "/" + total + ": " + step.getDescription() + progressText,
                NamedTextColor.GOLD));
    }

    /**
     * Called every second to refresh actionbar for all tracked players.
     */
    public void tickActionbar() {
        if (!config.showStepActionbar()) return;
        for (Map.Entry<UUID, PlayerProgress> entry : progress.entrySet()) {
            PlayerProgress pp = entry.getValue();
            if (pp.completed) continue;
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            TutorialStep step = pp.design.getSteps().get(pp.currentStepIndex);
            showActionbarProgress(player, step, pp.currentStepProgress);
        }
    }

    /**
     * Sends the current tutorial step as a chat message for all tracked players.
     * Called every 200 ticks (10 seconds) from TutorialMode.onTick().
     */
    public void tickChatReminder() {
        for (Map.Entry<UUID, PlayerProgress> entry : progress.entrySet()) {
            PlayerProgress pp = entry.getValue();
            if (pp.completed) continue;
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            TutorialStep step = pp.design.getSteps().get(pp.currentStepIndex);
            int stepNum = pp.currentStepIndex + 1;
            int total = pp.design.getStepCount();
            String progressSuffix = step.getRequiredCount() > 1
                    ? " (" + pp.currentStepProgress + "/" + step.getRequiredCount() + ")"
                    : "";
            player.sendMessage(
                    Component.text("[Tutorial] ", NamedTextColor.YELLOW)
                            .append(Component.text("Step " + stepNum + "/" + total + ": ", NamedTextColor.WHITE))
                            .append(Component.text(step.getDescription() + progressSuffix, NamedTextColor.GREEN))
            );
        }
    }

    public Set<UUID> getCompletedPlayers() {
        Set<UUID> completed = new HashSet<>();
        for (Map.Entry<UUID, PlayerProgress> entry : progress.entrySet()) {
            if (entry.getValue().completed) completed.add(entry.getKey());
        }
        return completed;
    }

    public PlayerProgress getProgress(UUID uuid) {
        return progress.get(uuid);
    }

    // ========================
    // Inner class
    // ========================

    public static class PlayerProgress {
        final TutorialDesign design;
        int currentStepIndex = 0;
        int currentStepProgress = 0;
        boolean completed = false;
        double moveDistanceAccumulated = 0;

        PlayerProgress(TutorialDesign design) {
            this.design = design;
        }

        public TutorialDesign getDesign() { return design; }
        public int getCurrentStepIndex() { return currentStepIndex; }
        public int getCurrentStepProgress() { return currentStepProgress; }
        public boolean isCompleted() { return completed; }
    }
}
