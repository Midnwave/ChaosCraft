package com.blockforge.chaoscraft.services.placeholders;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.services.titlescreen.PingTrackerService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenSession;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * PlaceholderAPI expansion under the <code>%chaoscraft_*%</code> identifier.
 * Provides title-screen-specific placeholders: loading status, progress,
 * verification state, tick countdowns, and ping.
 * <p>
 * NOTE: This expansion focuses on title-screen data only. It coexists with
 * the main {@code PlaceholderExpansion} in the {@code integration} package
 * which handles game-mode / timer placeholders.
 */
public class TitleScreenPlaceholders extends PlaceholderExpansion {

    private final ChaosCraftPlugin plugin;
    private final TitleScreenService service;
    private final PingTrackerService pingTracker;

    public TitleScreenPlaceholders(ChaosCraftPlugin plugin,
                                   TitleScreenService service,
                                   PingTrackerService pingTracker) {
        this.plugin = plugin;
        this.service = service;
        this.pingTracker = pingTracker;
    }

    @Override public @NotNull String getIdentifier() { return "cctitlescreen"; }
    @Override public @NotNull String getAuthor() { return "BlockForge Studios"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        Optional<TitleScreenSession> sessionOpt = service.getSession(player.getUniqueId());

        return switch (identifier) {
            case "loadingstatus" -> sessionOpt
                    .filter(TitleScreenSession::isLoading)
                    .map(s -> s.getCurrentStage().getDisplayName())
                    .orElse("");

            case "loadingprogress" -> sessionOpt
                    .filter(TitleScreenSession::isLoading)
                    .map(s -> String.valueOf(s.getProgress()))
                    .orElse("0");

            case "isloading" -> String.valueOf(
                    sessionOpt.map(TitleScreenSession::isLoading).orElse(false));

            case "verificationcode" -> sessionOpt
                    .map(TitleScreenSession::getVerificationCode)
                    .orElse("");

            case "awaitingverification" -> String.valueOf(
                    sessionOpt.map(TitleScreenSession::isAwaitingVerification).orElse(false));

            case "tickstill20" -> {
                if (sessionOpt.isEmpty()) yield "0";
                TitleScreenSession session = sessionOpt.get();
                if (!session.isInTitleScreen()) yield "0";
                long currentTick = plugin.getServer().getCurrentTick();
                long ticksSinceJoin = currentTick - session.getJoinTick();
                long countdown = 20L - (ticksSinceJoin % 20L);
                yield String.valueOf(countdown);
            }

            case "ping" -> {
                int ping = pingTracker.getPing(player);
                yield ping >= 0 ? String.valueOf(ping) : "0";
            }

            default -> null;
        };
    }
}
