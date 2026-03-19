package com.blockforge.chaoscraft.api.points;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-session Mode Points for all players during an active mode.
 * Handles point awarding, threshold rewards, and debug notifications.
 */
public class ModePointsTracker {

    private final ChaosCraftPlugin plugin;
    private final ModePointsConfig config;

    // Per-player session points
    private final Map<UUID, Integer> sessionPoints = new ConcurrentHashMap<>();
    // Per-player tracking of which reward thresholds have been awarded
    private final Map<UUID, Set<Integer>> awardedThresholds = new ConcurrentHashMap<>();
    // Per-player tracking counters for milestone actions
    private final Map<UUID, Map<String, Integer>> milestoneCounters = new ConcurrentHashMap<>();
    // Per-player dodge tracking (locations in attack radius)
    private final Map<UUID, Set<Integer>> playersInAttackRadius = new ConcurrentHashMap<>();
    // Per-player damage zone timer (ticks spent in zone without damage)
    private final Map<UUID, Map<Integer, Integer>> damageZoneTicks = new ConcurrentHashMap<>();
    // Per-player active attack count (for overwhelm tracking)
    private final Map<UUID, Integer> activeAttackCounts = new ConcurrentHashMap<>();
    // Per-player minute tracking
    private final Map<UUID, Integer> minutesSurvived = new ConcurrentHashMap<>();

    private boolean active = false;

    public ModePointsTracker(ChaosCraftPlugin plugin, ModePointsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ========================
    // Session lifecycle
    // ========================

    public void startSession() {
        sessionPoints.clear();
        awardedThresholds.clear();
        milestoneCounters.clear();
        playersInAttackRadius.clear();
        damageZoneTicks.clear();
        activeAttackCounts.clear();
        minutesSurvived.clear();
        active = true;
    }

    public void endSession() {
        active = false;
        // Award end-of-session bonuses (survive full timer handled by mode)
    }

    public boolean isActive() { return active; }

    // ========================
    // Core point awarding
    // ========================

    /**
     * Award points for a specific action to a player.
     * Checks if action is enabled, applies configured point value,
     * fires debug alert, and checks reward thresholds.
     */
    public void awardPoints(Player player, PointAction action) {
        if (!active) return;
        if (!config.isEnabled(action)) return;

        int points = config.getPoints(action);
        if (points <= 0) return;

        UUID uuid = player.getUniqueId();
        int oldTotal = sessionPoints.getOrDefault(uuid, 0);
        int newTotal = oldTotal + points;
        sessionPoints.put(uuid, newTotal);

        // Debug alert
        if (config.isDebug()) {
            player.sendMessage(Component.text("[MP Debug] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("+" + points, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" " + action.getDescription(), NamedTextColor.GRAY))
                    .append(Component.text(" (Total: " + newTotal + ")", NamedTextColor.DARK_GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
            plugin.debug("[ModePoints] " + player.getName() + " +" + points + " for " + action.getConfigKey() + " (total: " + newTotal + ")");
        }

        // Check reward thresholds
        checkThresholds(player, oldTotal, newTotal);
    }

    /**
     * Award points with a custom amount (for configurable bonuses).
     */
    public void awardPoints(Player player, PointAction action, int customPoints) {
        if (!active) return;
        if (!config.isEnabled(action)) return;
        if (customPoints <= 0) return;

        UUID uuid = player.getUniqueId();
        int oldTotal = sessionPoints.getOrDefault(uuid, 0);
        int newTotal = oldTotal + customPoints;
        sessionPoints.put(uuid, newTotal);

        if (config.isDebug()) {
            player.sendMessage(Component.text("[MP Debug] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("+" + customPoints, NamedTextColor.GREEN, TextDecoration.BOLD))
                    .append(Component.text(" " + action.getDescription(), NamedTextColor.GRAY))
                    .append(Component.text(" (Total: " + newTotal + ")", NamedTextColor.DARK_GRAY)));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
        }

        checkThresholds(player, oldTotal, newTotal);
    }

    // ========================
    // Threshold rewards
    // ========================

    private void checkThresholds(Player player, int oldTotal, int newTotal) {
        UUID uuid = player.getUniqueId();
        Set<Integer> awarded = awardedThresholds.computeIfAbsent(uuid, k -> new HashSet<>());

        for (var entry : config.getRewardThresholds().entrySet()) {
            int threshold = entry.getKey();
            if (oldTotal < threshold && newTotal >= threshold && !awarded.contains(threshold)) {
                awarded.add(threshold);
                // Execute reward commands
                for (String cmd : entry.getValue()) {
                    String resolved = cmd.replace("%player%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                }
                // Notify player
                player.sendMessage(Component.text(">> Mode Points Reward! ", NamedTextColor.GOLD, TextDecoration.BOLD)
                        .append(Component.text("Reached " + threshold + " points!", NamedTextColor.YELLOW)));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                if (config.isDebug()) {
                    plugin.debug("[ModePoints] " + player.getName() + " reached threshold " + threshold + ", executing " + entry.getValue().size() + " reward commands.");
                }
            }
        }
    }

    // ========================
    // Milestone counter helpers
    // ========================

    /**
     * Increment a named milestone counter for a player.
     * Returns the new count.
     */
    public int incrementMilestone(UUID player, String milestoneName) {
        Map<String, Integer> counters = milestoneCounters.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        int newCount = counters.merge(milestoneName, 1, Integer::sum);
        return newCount;
    }

    public int getMilestoneCount(UUID player, String milestoneName) {
        Map<String, Integer> counters = milestoneCounters.get(player);
        if (counters == null) return 0;
        return counters.getOrDefault(milestoneName, 0);
    }

    // ========================
    // Dodge tracking
    // ========================

    /**
     * Mark that a player entered an attack's damage radius.
     */
    public void playerEnteredAttackRadius(UUID player, int attackId) {
        playersInAttackRadius.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(attackId);
    }

    /**
     * Mark that a player left an attack's damage radius without taking damage.
     * This is a dodge!
     */
    public void playerDodgedAttack(UUID player, int attackId) {
        Set<Integer> attacks = playersInAttackRadius.get(player);
        if (attacks != null) {
            attacks.remove(attackId);
        }
    }

    /**
     * Player took damage from an attack — NOT a dodge.
     */
    public void playerHitByAttack(UUID player, int attackId) {
        Set<Integer> attacks = playersInAttackRadius.get(player);
        if (attacks != null) {
            attacks.remove(attackId);
        }
    }

    // ========================
    // Active attack count (overwhelm tracking)
    // ========================

    public void setActiveAttackCount(UUID player, int count) {
        activeAttackCounts.put(player, count);
    }

    public int getActiveAttackCount(UUID player) {
        return activeAttackCounts.getOrDefault(player, 0);
    }

    // ========================
    // Minute survival tracking
    // ========================

    public void incrementMinute(UUID player) {
        minutesSurvived.merge(player, 1, Integer::sum);
    }

    // ========================
    // Queries
    // ========================

    public int getPoints(UUID player) {
        return sessionPoints.getOrDefault(player, 0);
    }

    public int getPoints(Player player) {
        return getPoints(player.getUniqueId());
    }

    public Map<UUID, Integer> getAllPoints() {
        return Collections.unmodifiableMap(sessionPoints);
    }

    /**
     * Get top N players by points.
     */
    public List<Map.Entry<UUID, Integer>> getTopPlayers(int limit) {
        return sessionPoints.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }

    public int getTotalPointsAwarded() {
        return sessionPoints.values().stream().mapToInt(Integer::intValue).sum();
    }
}
