package com.blockforge.chaoscraft.modes.chain;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for Chain mode.
 *
 * <p>Identifier: {@code chaoscraft_chain}
 *
 * <p>Placeholders:
 * <ul>
 *   <li>{@code %chaoscraft_chain_lastchain%} — seconds since the player was
 *       last chained by a mob (int), or {@code 0} if never chained.</li>
 * </ul>
 *
 * <p>Mirrors {@link com.blockforge.chaoscraft.modes.freezingice.FreezingIcePlaceholders}
 * structurally — registered on mode start and unregistered on mode end so the
 * expansion only exists while Chain mode is active.
 */
public class ChainPlaceholders extends PlaceholderExpansion {

    private final ChaosCraftPlugin plugin;

    public ChainPlaceholders(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "chaoscraft_chain"; }
    @Override public @NotNull String getAuthor() { return "BlockForge Studios"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        var modeObj = plugin.getModeManager().getMode("chain");
        if (!(modeObj instanceof ChainMode chainMode)
                || chainMode.getChainAttackSystem() == null) {
            return "0";
        }
        if ("lastchain".equalsIgnoreCase(params)) {
            return String.valueOf(chainMode.getChainAttackSystem().getSecondsSinceLastChain(player));
        }
        return null;
    }
}
