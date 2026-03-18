package com.blockforge.chaoscraft.services.placeholders;

import com.blockforge.chaoscraft.services.codes.CodesService;
import com.blockforge.chaoscraft.services.play.PlayService;
import com.blockforge.chaoscraft.services.settings.SettingsService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenService;
import com.blockforge.chaoscraft.services.titlescreen.TitleScreenSession;
import com.blockforge.chaoscraft.services.useragreement.UserAgreementService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * PlaceholderAPI expansion under the <code>%cc_*%</code> identifier.
 * Provides cross-service placeholders: title-screen state, codes status,
 * settings status, and play status.
 */
public class CCPlaceholders extends PlaceholderExpansion {

    private final TitleScreenService titleScreenService;
    private final CodesService codesService;
    private final SettingsService settingsService;
    private final UserAgreementService userAgreementService;
    private final PlayService playService;

    public CCPlaceholders(TitleScreenService titleScreenService,
                          CodesService codesService,
                          SettingsService settingsService,
                          UserAgreementService userAgreementService,
                          PlayService playService) {
        this.titleScreenService = titleScreenService;
        this.codesService = codesService;
        this.settingsService = settingsService;
        this.userAgreementService = userAgreementService;
        this.playService = playService;
    }

    @Override public @NotNull String getIdentifier() { return "cc"; }
    @Override public @NotNull String getAuthor() { return "BlockForge Studios"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        Optional<TitleScreenSession> sessionOpt = titleScreenService != null
                ? titleScreenService.getSession(player.getUniqueId())
                : Optional.empty();

        return switch (identifier) {
            case "isintitlescreen" -> String.valueOf(
                    sessionOpt.map(TitleScreenSession::isInTitleScreen).orElse(false));

            case "verificationcode" -> sessionOpt.map(TitleScreenSession::getVerificationCode).orElse("");

            case "tickstill20" -> {
                if (sessionOpt.isEmpty()) yield "0";
                TitleScreenSession session = sessionOpt.get();
                if (!session.isInTitleScreen()) yield "0";
                long currentTick = titleScreenService.getPlugin().getServer().getCurrentTick();
                long ticksSinceJoin = currentTick - session.getJoinTick();
                long countdown = 20L - (ticksSinceJoin % 20L);
                yield String.valueOf(countdown);
            }

            case "ping" -> {
                if (titleScreenService == null) yield "0";
                int ping = titleScreenService.getPingTracker().getPing(player);
                yield ping >= 0 ? String.valueOf(ping) : "0";
            }

            case "codestatus" -> codesService != null ? codesService.getStatusText(player.getUniqueId()) : "";

            case "settings_status" -> settingsService != null ? settingsService.getSettingsStatus(player.getUniqueId()) : "";

            case "play_status" -> {
                if (userAgreementService == null || playService == null) yield "";
                if (!userAgreementService.hasAccepted(player.getUniqueId()))
                    yield "You must accept the user agreement to play the server.";
                yield playService.isPlaying(player.getUniqueId())
                        ? playService.getPlayStatusMessage(player.getUniqueId(), player.getName())
                        : "";
            }

            default -> null;
        };
    }
}
