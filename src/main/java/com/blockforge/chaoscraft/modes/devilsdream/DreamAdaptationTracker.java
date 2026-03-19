package com.blockforge.chaoscraft.modes.devilsdream;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Tracks player behavior and escalates dream responses.
 * Each player has independent scores per DreamAction that decay over time.
 * When a score crosses a threshold, the dream "responds" with targeted attacks.
 */
public class DreamAdaptationTracker {

    private final ChaosCraftPlugin plugin;
    private final DevilsDreamConfig config;

    // Per-player adaptation scores
    private final Map<UUID, EnumMap<DreamAction, Integer>> playerScores = new HashMap<>();

    // Per-player last known state for detecting actions
    private final Map<UUID, Location> lastPositions = new HashMap<>();
    private final Map<UUID, Float> lastYaws = new HashMap<>();
    private final Map<UUID, Boolean> lastSprinting = new HashMap<>();
    private final Map<UUID, Boolean> lastSneaking = new HashMap<>();
    private final Map<UUID, Integer> stillnessTicks = new HashMap<>();

    // Decay counter — scores decay every N ticks
    private int decayCounter = 0;

    public DreamAdaptationTracker(ChaosCraftPlugin plugin, DevilsDreamConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /**
     * Called every server tick from DevilsDreamMode.onTick().
     * Analyzes all tracked players' behavior and updates scores.
     */
    public void tick(Collection<? extends Player> players) {
        for (Player player : players) {
            if (!player.isOnline()) continue;
            UUID id = player.getUniqueId();

            EnumMap<DreamAction, Integer> scores = playerScores.computeIfAbsent(id,
                    k -> new EnumMap<>(DreamAction.class));

            Location currentPos = player.getLocation();
            float currentYaw = currentPos.getYaw();

            Location lastPos = lastPositions.get(id);
            Float lastYaw = lastYaws.get(id);

            // --- Detect sprinting ---
            if (player.isSprinting()) {
                addScore(scores, DreamAction.MOVEMENT, config.getScorePerAction());
            }

            // --- Detect sneaking ---
            if (player.isSneaking()) {
                addScore(scores, DreamAction.FEAR, config.getScorePerAction());
            }

            // --- Detect standing still ---
            if (lastPos != null && lastPos.getWorld() == currentPos.getWorld()) {
                double distSq = lastPos.distanceSquared(currentPos);
                if (distSq < 0.01) {
                    int ticks = stillnessTicks.getOrDefault(id, 0) + 1;
                    stillnessTicks.put(id, ticks);
                    if (ticks % 20 == 0) { // Every second of standing still
                        addScore(scores, DreamAction.STILLNESS, config.getScorePerAction());
                    }
                } else {
                    stillnessTicks.put(id, 0);
                }
            }

            // --- Detect jumping ---
            if (lastPos != null && lastPos.getWorld() == currentPos.getWorld()) {
                double dy = currentPos.getY() - lastPos.getY();
                if (dy > 0.3 && !player.isFlying() && !player.isGliding()) {
                    addScore(scores, DreamAction.FLIGHT, config.getScorePerAction());
                }
            }

            // --- Detect rapid looking (paranoia) ---
            if (lastYaw != null) {
                float yawDelta = Math.abs(currentYaw - lastYaw);
                if (yawDelta > 180) yawDelta = 360 - yawDelta;
                if (yawDelta > 30) { // Fast head movement
                    addScore(scores, DreamAction.PARANOIA, config.getScorePerAction());
                }
            }

            // Update last known state
            lastPositions.put(id, currentPos.clone());
            lastYaws.put(id, currentYaw);
            lastSprinting.put(id, player.isSprinting());
            lastSneaking.put(id, player.isSneaking());
        }

        // Decay all scores periodically
        decayCounter++;
        int decayInterval = config.getDecayIntervalTicks();
        if (decayInterval > 0 && decayCounter >= decayInterval) {
            decayCounter = 0;
            decayAllScores();
        }
    }

    /**
     * Record a specific action for a player (called from event listeners).
     * Used for block break, block place, entity attack events.
     */
    public void recordAction(UUID playerId, DreamAction action) {
        EnumMap<DreamAction, Integer> scores = playerScores.computeIfAbsent(playerId,
                k -> new EnumMap<>(DreamAction.class));
        addScore(scores, action, config.getScorePerAction());
    }

    /**
     * Get a player's score for a specific action.
     */
    public int getScore(UUID playerId, DreamAction action) {
        EnumMap<DreamAction, Integer> scores = playerScores.get(playerId);
        if (scores == null) return 0;
        return scores.getOrDefault(action, 0);
    }

    /**
     * Get the highest scoring action for a player.
     */
    public DreamAction getDominantAction(UUID playerId) {
        EnumMap<DreamAction, Integer> scores = playerScores.get(playerId);
        if (scores == null || scores.isEmpty()) return DreamAction.STILLNESS;

        DreamAction dominant = DreamAction.STILLNESS;
        int maxScore = 0;
        for (var entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    /**
     * Get the total combined score across all actions for a player.
     */
    public int getTotalScore(UUID playerId) {
        EnumMap<DreamAction, Integer> scores = playerScores.get(playerId);
        if (scores == null) return 0;
        return scores.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Check if a specific action score is above the response threshold.
     */
    public boolean isAboveThreshold(UUID playerId, DreamAction action) {
        return getScore(playerId, action) >= config.getAdaptationThreshold();
    }

    /**
     * Get spawn rate multiplier for a given attack type based on adaptation.
     * Returns 1.0 normally, higher if the matching adaptation score is high.
     */
    public double getSpawnMultiplier(UUID playerId, DreamAction relevantAction) {
        int score = getScore(playerId, relevantAction);
        int threshold = config.getAdaptationThreshold();
        if (score < threshold) return 1.0;
        // Linear scale: at threshold = 1.5x, at 2x threshold = 2.0x, capped at 3.0x
        double multiplier = 1.0 + ((double)(score - threshold) / threshold) * 0.5;
        return Math.min(multiplier, config.getMaxAdaptationMultiplier());
    }

    /**
     * Reset all tracking for a player.
     */
    public void resetPlayer(UUID playerId) {
        playerScores.remove(playerId);
        lastPositions.remove(playerId);
        lastYaws.remove(playerId);
        lastSprinting.remove(playerId);
        lastSneaking.remove(playerId);
        stillnessTicks.remove(playerId);
    }

    /**
     * Reset all tracking.
     */
    public void resetAll() {
        playerScores.clear();
        lastPositions.clear();
        lastYaws.clear();
        lastSprinting.clear();
        lastSneaking.clear();
        stillnessTicks.clear();
        decayCounter = 0;
    }

    // ========================
    // Internal
    // ========================

    private void addScore(EnumMap<DreamAction, Integer> scores, DreamAction action, int amount) {
        int current = scores.getOrDefault(action, 0);
        int max = config.getMaxScore();
        scores.put(action, Math.min(current + amount, max));
    }

    private void decayAllScores() {
        int decayAmount = config.getDecayAmount();
        for (EnumMap<DreamAction, Integer> scores : playerScores.values()) {
            for (DreamAction action : DreamAction.values()) {
                int current = scores.getOrDefault(action, 0);
                if (current > 0) {
                    scores.put(action, Math.max(0, current - decayAmount));
                }
            }
        }
    }
}
