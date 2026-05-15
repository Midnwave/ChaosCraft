package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for FreezingIce mode.
 *
 * <p>Identifier: {@code chaoscraft_freezingice}
 *
 * <p>Placeholders:
 * <ul>
 *   <li>{@code %chaoscraft_freezingice_frostbite%} — current frostbite % (int 0-100)</li>
 * </ul>
 */
public class FreezingIcePlaceholders extends PlaceholderExpansion {

    private final ChaosCraftPlugin plugin;

    public FreezingIcePlaceholders(ChaosCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "chaoscraft_freezingice"; }
    @Override public @NotNull String getAuthor() { return "BlockForge Studios"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        var modeObj = plugin.getModeManager().getMode("freezingice");
        if (!(modeObj instanceof FreezingIceMode fi) || fi.getFrostbiteTracker() == null) {
            return "0";
        }
        if ("frostbite".equalsIgnoreCase(params)) {
            return String.valueOf(fi.getFrostbiteTracker().getFrostbite(player));
        }
        return null;
    }
}
