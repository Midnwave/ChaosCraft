package com.blockforge.chaoscraft.modes.fluffy;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility for FluffyMode version-aware behavior.
 *
 * Some flora blocks (WILDFLOWERS, BUSH, FIREFLY_BUSH, LEAF_LITTER, SHORT_DRY_GRASS,
 * EYEBLOSSOM) only exist on 1.21.5+. This utility detects the running server
 * version and returns an appropriate flora list. All Material lookups use
 * Material.matchMaterial() so the class compiles regardless of the runtime
 * version of Paper / Bukkit.
 */
public final class FluffyVersionUtil {

    private FluffyVersionUtil() {}

    private static Boolean cached1215Plus = null;

    /**
     * Returns true if the server is running Minecraft 1.21.5 or newer.
     * Result is cached statically — the first call parses
     * {@link Bukkit#getMinecraftVersion()}.
     */
    public static boolean is1215Plus() {
        if (cached1215Plus != null) return cached1215Plus;
        try {
            String version = Bukkit.getMinecraftVersion(); // e.g. "1.21.5"
            String[] parts = version.split("\\.");
            if (parts.length >= 3) {
                // Strip any trailing "-pre1" or "-rc1"
                String patchStr = parts[2].split("[^0-9]")[0];
                int patch = Integer.parseInt(patchStr);
                int minor = Integer.parseInt(parts[1].split("[^0-9]")[0]);
                int major = Integer.parseInt(parts[0].split("[^0-9]")[0]);
                cached1215Plus = (major > 1)
                        || (major == 1 && minor > 21)
                        || (major == 1 && minor == 21 && patch >= 5);
            } else {
                cached1215Plus = false;
            }
        } catch (Throwable t) {
            cached1215Plus = false;
        }
        return cached1215Plus;
    }

    /**
     * Returns the list of flora Materials available on this server version.
     *
     * Always-available flora is included unconditionally. 1.21.5+ flora is
     * only included if the server runs that version or newer. Each Material
     * is resolved via {@link Material#matchMaterial(String)} — null returns
     * are skipped to keep the call safe across versions.
     */
    public static List<Material> getFlora() {
        List<Material> flora = new ArrayList<>();
        addIfPresent(flora, "DANDELION");
        addIfPresent(flora, "POPPY");
        addIfPresent(flora, "BLUE_ORCHID");
        addIfPresent(flora, "ALLIUM");
        addIfPresent(flora, "AZURE_BLUET");
        addIfPresent(flora, "CORNFLOWER");
        addIfPresent(flora, "LILY_OF_THE_VALLEY");
        addIfPresent(flora, "OXEYE_DAISY");
        addIfPresent(flora, "PALE_MOSS_CARPET");

        // EYEBLOSSOM only added if it resolves at runtime
        addIfPresent(flora, "OPEN_EYEBLOSSOM");
        addIfPresent(flora, "CLOSED_EYEBLOSSOM");
        addIfPresent(flora, "EYEBLOSSOM");

        if (is1215Plus()) {
            addIfPresent(flora, "WILDFLOWERS");
            addIfPresent(flora, "BUSH");
            addIfPresent(flora, "FIREFLY_BUSH");
            addIfPresent(flora, "LEAF_LITTER");
            addIfPresent(flora, "SHORT_DRY_GRASS");
        }
        return flora;
    }

    private static void addIfPresent(List<Material> list, String name) {
        Material m = Material.matchMaterial(name);
        if (m != null) list.add(m);
    }
}
