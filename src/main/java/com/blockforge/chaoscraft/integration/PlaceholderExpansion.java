package com.blockforge.chaoscraft.integration;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.CalamityMode;
import com.blockforge.chaoscraft.modes.calamity.boss.DoGManager;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenSession;
import com.blockforge.chaoscraft.services.titlescreen.PingTrackerService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * PlaceholderAPI expansion for ChaosCraft.
 *
 * Placeholders:
 *   %chaoscraft_timer_hh_mm_ss%
 *   %chaoscraft_timer_mm_ss%
 *   %chaoscraft_timer_m_ss%
 *   %chaoscraft_timer_ss_ms%
 *   %chaoscraft_timer_raw%
 *   %chaoscraft_mode%             - active mode name or "none"
 *   %chaoscraft_mode_active%      - "true" or "false"
 *   %chaoscraft_mode_timer_active% - "true"/"false" — BetterHud visibility
 *   %chaoscraft_mode_timer_flash%  - "true"/"false" — 10-tick alternation when low, permanent at 0:00
 *   %chaoscraft_survived%         - "true"/"false" for the requesting player
 *   %chaoscraft_gems%             - global gem count (Calamity specific)
 *   %chaoscraft_gems_deposited%   - deposited gem count (Calamity specific)
 *   %chaoscraft_gems_required%    - current boss gem requirement (Calamity specific)
 *   %chaoscraft_phase%            - current Calamity phase (1-5)
 *   %chaoscraft_dog_alive%        - "true"/"false"
 *   %chaoscraft_dog_health%       - DoG current HP (raw number)
 *   %chaoscraft_dog_health_max%   - DoG max HP
 *   %chaoscraft_dog_health_pct%   - DoG health percentage (0.0-100.0)
 *   %chaoscraft_dog_phase2%       - "true"/"false" (Universal Collapse active)
 */
public class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final ChaosCraftPlugin plugin;

    public PlaceholderExpansion(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chaoscraft";
    }

    @Override
    public @NotNull String getAuthor() {
        return "BlockForgeStudios";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        var timer = plugin.getModeTimer();
        var manager = plugin.getModeManager();

        return switch (identifier.toLowerCase()) {
            // Timer formats
            case "timer_hh_mm_ss" -> timer.formatHhMmSs();
            case "timer_mm_ss" -> timer.formatMmSs();
            case "timer_m_ss" -> {
                // If timer HUD is active with a delay, return blank during delay
                var timerHud = plugin.getModeTimerHud();
                if (timerHud != null && timerHud.isActive() && timerHud.isDelaying()) {
                    yield "";
                }
                yield timer.formatMSs();
            }
            case "timer_ss_ms" -> timer.formatSsMs();
            case "timer_raw" -> timer.formatRaw();

            // Mode info
            case "mode" -> manager.getActiveModeName();
            case "mode_active" -> String.valueOf(manager.isAnyModeActive());

            // Player survival
            case "survived" -> {
                if (player == null || !manager.isAnyModeActive()) yield "false";
                yield String.valueOf(manager.getActiveMode().hasSurvived(player));
            }

            // Calamity gem count
            case "gems" -> {
                var mode = manager.getMode("calamity");
                if (mode instanceof CalamityMode calamity) {
                    yield String.valueOf(calamity.getGemCount());
                }
                yield "0";
            }

            // Calamity deposited gems
            case "gems_deposited" -> {
                var mode = manager.getMode("calamity");
                if (mode instanceof CalamityMode calamity) {
                    yield String.valueOf(calamity.getGemManager().getDepositedGems());
                }
                yield "0";
            }

            // Calamity gem requirement for current boss
            case "gems_required" -> {
                var mode = manager.getMode("calamity");
                if (mode instanceof CalamityMode calamity) {
                    yield String.valueOf(calamity.getGemManager().getCurrentGemRequirement());
                }
                yield "0";
            }

            // Calamity phase
            case "phase" -> {
                var mode = manager.getMode("calamity");
                if (mode instanceof CalamityMode calamity) {
                    yield String.valueOf(calamity.getCurrentPhase());
                }
                yield "0";
            }

            // DoG bossbar placeholders
            case "dog_alive" -> {
                var dogMode = manager.getMode("calamity");
                if (dogMode instanceof CalamityMode cm) {
                    DoGManager dog = cm.getDoGManager();
                    yield String.valueOf(dog.isAlive());
                }
                yield "false";
            }
            case "dog_health" -> {
                var dogMode = manager.getMode("calamity");
                if (dogMode instanceof CalamityMode cm) {
                    yield String.format("%.0f", cm.getDoGManager().getCurrentHealth());
                }
                yield "0";
            }
            case "dog_health_max" -> {
                var dogMode = manager.getMode("calamity");
                if (dogMode instanceof CalamityMode cm) {
                    yield String.format("%.0f", cm.getDoGManager().getMaxHealth());
                }
                yield "0";
            }
            case "dog_health_pct" -> {
                var dogMode = manager.getMode("calamity");
                if (dogMode instanceof CalamityMode cm) {
                    yield String.format("%.1f", cm.getDoGManager().getHealthPercent());
                }
                yield "0.0";
            }
            case "dog_phase2" -> {
                var dogMode = manager.getMode("calamity");
                if (dogMode instanceof CalamityMode cm) {
                    yield String.valueOf(cm.getDoGManager().isPhase2());
                }
                yield "false";
            }

            // Mode Points
            case "mode_points" -> {
                if (player == null) yield "0";
                var pts = plugin.getModePointsService();
                yield pts != null ? String.valueOf(pts.getPoints(player)) : "0";
            }
            case "mode_points_session" -> {
                var pts = plugin.getModePointsService();
                yield pts != null && pts.isSessionActive() ? "true" : "false";
            }

            // Per-player join ticks (0→20, for BetterHud animation sync)
            case "join_ticks" -> {
                if (player == null) yield "20";
                yield String.valueOf(plugin.getPlayerJoinTicks(player));
            }

            // Mode Timer HUD placeholders
            case "mode_timer_active" -> {
                var hud = plugin.getModeTimerHud();
                yield hud != null ? String.valueOf(hud.isActive()) : "false";
            }
            case "mode_timer_flash" -> {
                var hud = plugin.getModeTimerHud();
                yield hud != null ? String.valueOf(hud.isFlashing()) : "false";
            }
            // Slide animation now uses join_ticks placeholder instead
            case "mode_color" -> {
                var hud = plugin.getModeTimerHud();
                yield hud != null ? hud.getColor() : "white";
            }
            case "mode_display_name" -> {
                var hud = plugin.getModeTimerHud();
                yield hud != null ? hud.getDisplayName() : "";
            }

            // ── Title Screen placeholders (merged from TitleScreenPlaceholders) ──
            case "loadingstatus" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "";
                Optional<TitleScreenSession> session = tsService.getSession(player.getUniqueId());
                yield session.filter(TitleScreenSession::isLoading)
                        .map(s -> s.getCurrentStage().getDisplayName()).orElse("");
            }
            case "loadingprogress" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "0";
                yield tsService.getSession(player.getUniqueId())
                        .filter(TitleScreenSession::isLoading)
                        .map(s -> String.valueOf(s.getProgress())).orElse("0");
            }
            case "isloading" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "false";
                yield String.valueOf(tsService.getSession(player.getUniqueId())
                        .map(TitleScreenSession::isLoading).orElse(false));
            }
            case "verificationcode" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "";
                yield tsService.getSession(player.getUniqueId())
                        .map(TitleScreenSession::getVerificationCode).orElse("");
            }
            case "awaitingverification" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "false";
                yield String.valueOf(tsService.getSession(player.getUniqueId())
                        .map(TitleScreenSession::isAwaitingVerification).orElse(false));
            }
            case "tickstill20" -> {
                var tsService = plugin.getTitleScreenService();
                if (tsService == null || player == null) yield "0";
                Optional<TitleScreenSession> session = tsService.getSession(player.getUniqueId());
                if (session.isEmpty() || !session.get().isInTitleScreen()) yield "0";
                long currentTick = plugin.getServer().getCurrentTick();
                long ticksSinceJoin = currentTick - session.get().getJoinTick();
                yield String.valueOf(20L - (ticksSinceJoin % 20L));
            }
            case "ping" -> {
                if (player == null) yield "0";
                var tsService = plugin.getTitleScreenService();
                if (tsService == null) yield String.valueOf(player.getPing());
                var pingTracker = tsService.getPingTracker();
                int ping = pingTracker.getPing(player);
                yield ping >= 0 ? String.valueOf(ping) : "0";
            }

            // Player Stats (kills, s-kills, mode survivals)
            case "kills" -> {
                if (player == null) yield "0";
                var stats = plugin.getPlayerStatsService();
                yield stats != null ? String.valueOf(stats.getKills(player.getUniqueId())) : "0";
            }
            case "s_kills" -> {
                if (player == null) yield "0";
                var stats = plugin.getPlayerStatsService();
                yield stats != null ? String.valueOf(stats.getSKills(player.getUniqueId())) : "0";
            }
            case "mode_survivals" -> {
                if (player == null) yield "0";
                var stats = plugin.getPlayerStatsService();
                yield stats != null ? String.valueOf(stats.getModeSurvivals(player.getUniqueId())) : "0";
            }

            default -> {
                // Badge ownership check (%chaoscraft_has_badge_<id>%)
                if (identifier.startsWith("has_badge_")) {
                    String badgeId = identifier.substring("has_badge_".length());
                    var badgeSvc = plugin.getBadgeService();
                    if (badgeSvc != null && player != null) {
                        yield String.valueOf(badgeSvc.hasBadge(player.getUniqueId(), badgeId));
                    }
                    yield "false";
                }
                yield null;
            }
        };
    }
}
