package com.blockforge.chaoscraft.modes.freezingice;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * FreezingIce — Global block-friction override.
 *
 * While FreezingIce mode is active, every block type on the server has its
 * NMS Block#friction field temporarily overridden to a configured value
 * (default 0.93, vanilla = 0.6, vanilla ice = 0.98), making the entire world
 * feel slippery. Originals are captured on first apply and restored exactly
 * on revert.
 *
 * Reflection notes:
 *  - Resolves the underlying NMS Block via Bukkit's CraftBlockData wrapper
 *    (reflective Class.forName, no Paper version imports).
 *  - Locates the friction field by Mojang name "friction" with a reobf
 *    fallback (single-letter primitive float field, value matches the
 *    block's known default — 0.6 stone, 0.8 slime, 0.98 ice).
 *  - Writes through MethodHandles.privateLookupIn (Java 17+ safe final-field
 *    bypass); falls back to Field.setAccessible + Field.setFloat on failure.
 *
 * Thread safety: apply() / revert() are synchronized on the class to
 * prevent overlap from the main thread vs. a misbehaving plugin disable
 * hook firing concurrently.
 */
public class FreezingIceFrictionOverride {

    /** Per-Material original friction values, populated on apply(). */
    private static final Map<Material, Float> originals = new EnumMap<>(Material.class);

    /** Whether the override is currently active. */
    private static boolean applied = false;

    /** Cached friction Field, resolved once per JVM (same Block class layout for all blocks). */
    private static Field cachedFrictionField = null;

    /** If the cached field resolution failed, remember so we don't spam attempts. */
    private static boolean frictionFieldResolutionFailed = false;

    private FreezingIceFrictionOverride() {
        // static utility
    }

    /**
     * Apply a uniform friction value to every non-exempt block type.
     *
     * @param plugin       plugin (for logging)
     * @param newFriction  new friction value (typical 0.85–0.97)
     * @param exempt       set of Materials to skip
     * @return true if applied, false if already applied or fatal failure
     */
    public static synchronized boolean apply(ChaosCraftPlugin plugin, float newFriction, Set<Material> exempt) {
        if (applied) {
            plugin.getLogger().info("[FreezingIce] Friction override already active — skipping re-apply.");
            return false;
        }
        if (exempt == null) exempt = java.util.Collections.emptySet();

        int modified = 0;
        int skippedExempt = 0;
        int skippedNonBlock = 0;
        int errors = 0;

        for (Material m : Material.values()) {
            if (m == Material.AIR || !m.isBlock()) {
                skippedNonBlock++;
                continue;
            }
            if (exempt.contains(m)) {
                skippedExempt++;
                plugin.debug("[FreezingIce] friction: skipping exempt block " + m.name());
                continue;
            }
            try {
                Object nmsBlock = resolveNmsBlock(m);
                if (nmsBlock == null) {
                    skippedNonBlock++;
                    continue;
                }
                Field field = resolveFrictionField(nmsBlock);
                if (field == null) {
                    errors++;
                    continue;
                }
                float orig = field.getFloat(nmsBlock);
                originals.put(m, orig);
                writeFloat(field, nmsBlock, newFriction);
                modified++;
            } catch (Throwable t) {
                errors++;
                plugin.debug("[FreezingIce] friction apply failed for " + m.name() + ": " + t.getMessage());
            }
        }

        applied = true;
        plugin.getLogger().info("[FreezingIce] Applied friction override (value=" + newFriction
                + ", modified " + modified + " blocks, exempt " + skippedExempt
                + ", non-block " + skippedNonBlock
                + (errors > 0 ? ", errors " + errors : "") + ")");
        return true;
    }

    /**
     * Restore all originally captured friction values. Safe to call even if
     * apply() was never called (no-ops in that case).
     */
    public static synchronized boolean revert(ChaosCraftPlugin plugin) {
        if (!applied) {
            return false;
        }
        int restored = 0;
        int errors = 0;
        for (Map.Entry<Material, Float> entry : originals.entrySet()) {
            Material m = entry.getKey();
            float orig = entry.getValue();
            try {
                Object nmsBlock = resolveNmsBlock(m);
                if (nmsBlock == null) {
                    errors++;
                    continue;
                }
                Field field = resolveFrictionField(nmsBlock);
                if (field == null) {
                    errors++;
                    continue;
                }
                writeFloat(field, nmsBlock, orig);
                restored++;
            } catch (Throwable t) {
                errors++;
                if (plugin != null) {
                    plugin.getLogger().log(Level.WARNING,
                            "[FreezingIce] Failed to revert friction for " + m.name() + ": " + t.getMessage());
                }
            }
        }
        originals.clear();
        applied = false;
        if (plugin != null) {
            plugin.getLogger().info("[FreezingIce] Reverted friction on " + restored + " blocks"
                    + (errors > 0 ? " (" + errors + " errors)" : ""));
        }
        return true;
    }

    public static boolean isApplied() {
        return applied;
    }

    public static int getModifiedCount() {
        return originals.size();
    }

    // ====================================================================
    // Internals
    // ====================================================================

    /**
     * Resolve the underlying NMS Block instance for a Bukkit Material.
     * Uses Bukkit's CraftBlockData wrapper, which exposes the BlockState via
     * #getState() — reflected to keep this class free of version-specific
     * Paper / CraftBukkit imports.
     */
    private static Object resolveNmsBlock(Material m) {
        try {
            BlockData data = m.createBlockData();
            if (data == null) return null;
            // Walk class chain to find a getState() method (defined on CraftBlockData)
            Class<?> c = data.getClass();
            java.lang.reflect.Method getState = null;
            while (c != null) {
                try {
                    getState = c.getDeclaredMethod("getState");
                    break;
                } catch (NoSuchMethodException ignored) {
                    c = c.getSuperclass();
                }
            }
            if (getState == null) return null;
            getState.setAccessible(true);
            Object blockState = getState.invoke(data); // net.minecraft.world.level.block.state.BlockState
            if (blockState == null) return null;
            // BlockState#getBlock() returns net.minecraft.world.level.block.Block
            java.lang.reflect.Method getBlock = null;
            Class<?> bsc = blockState.getClass();
            while (bsc != null) {
                try {
                    getBlock = bsc.getMethod("getBlock");
                    break;
                } catch (NoSuchMethodException ignored) {
                    bsc = bsc.getSuperclass();
                }
            }
            if (getBlock == null) return null;
            getBlock.setAccessible(true);
            return getBlock.invoke(blockState);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Locate the friction field on the NMS Block class. Caches the resolved
     * Field (the layout is identical across all Block subclasses, since the
     * field lives on the base Block class).
     */
    private static Field resolveFrictionField(Object nmsBlock) {
        if (cachedFrictionField != null) return cachedFrictionField;
        if (frictionFieldResolutionFailed) return null;

        Class<?> c = nmsBlock.getClass();
        // First pass: walk up the class hierarchy and look for a float field
        // named exactly "friction" (Mojang-mapped name).
        Class<?> walk = c;
        while (walk != null && walk != Object.class) {
            for (Field f : walk.getDeclaredFields()) {
                if (f.getType() == float.class && "friction".equals(f.getName())) {
                    try {
                        f.setAccessible(true);
                        cachedFrictionField = f;
                        return f;
                    } catch (Throwable ignored) {
                    }
                }
            }
            walk = walk.getSuperclass();
        }

        // Reobf fallback: look for any single-character-named float instance
        // field whose initial value is one of the known friction defaults
        // (0.6 / 0.8 / 0.98). If we find exactly one, that's it.
        walk = c;
        while (walk != null && walk != Object.class) {
            for (Field f : walk.getDeclaredFields()) {
                if (f.getType() != float.class) continue;
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    float val = f.getFloat(nmsBlock);
                    if (val == 0.6f || val == 0.8f || val == 0.98f) {
                        cachedFrictionField = f;
                        return f;
                    }
                } catch (Throwable ignored) {
                }
            }
            walk = walk.getSuperclass();
        }

        frictionFieldResolutionFailed = true;
        return null;
    }

    /**
     * Write a float to a (possibly final) field. Prefers MethodHandles
     * (Java 17+ safe final-field bypass); falls back to direct
     * Field.setFloat on failure.
     */
    private static void writeFloat(Field field, Object target, float value) throws Throwable {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    field.getDeclaringClass(), MethodHandles.lookup());
            MethodHandle setter = lookup.unreflectSetter(field);
            setter.invoke(target, value);
        } catch (Throwable t1) {
            try {
                field.setAccessible(true);
                field.setFloat(target, value);
            } catch (Throwable t2) {
                // Combine the failure messages for diagnostic clarity
                IllegalStateException ex = new IllegalStateException(
                        "Both MethodHandles and direct reflection failed to write friction field "
                                + field.getDeclaringClass().getName() + "#" + field.getName()
                                + ": MH=" + t1.getMessage() + " | direct=" + t2.getMessage());
                ex.addSuppressed(t1);
                ex.addSuppressed(t2);
                throw ex;
            }
        }
    }
}
