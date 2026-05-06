package com.blockforge.chaoscraft.modes.fluffy.attacks;

import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Decorative animation helper for FluffyMode attacks.
 *
 * Strategy: per-tick auto-detect-and-animate.
 *
 * Many FluffyMode attacks spawn "cosmetic" ItemDisplay/BlockDisplay handles
 * with a long fade-in interpolation (interpolation(40, 0)) and never animate
 * them after that — they sit at a fixed position until cleanup, looking
 * stuck.
 *
 * Rather than refactor every spawn site, this utility sweeps the active
 * Fluffy world each tick and animates anything tagged "cc:fluffy" that
 * has interpolationDuration >= 30 (the unanimated decorative threshold).
 * The first time we see such an entity we cache its base translation/scale,
 * override the interpolation to 6 ticks, and apply a continuous BOB
 * (rotation + Y bob + scale pulse) for ItemDisplays or a gentle SWAY
 * (sway tilt + small pulse) for BlockDisplays.
 *
 * Active attack entities use < 30 tick interpolation and are skipped.
 *
 * Usage from FluffyMode.onTick():
 *   FluffyDecor.SWEEPER.tickWorld(world, tickCounter);
 */
public final class FluffyDecor {

    /** Singleton sweeper instance — used by FluffyMode.onTick. */
    public static final FluffyDecor SWEEPER = new FluffyDecor();

    /** Cached base state for a tracked entity. */
    private static final class Base {
        final float bx, by, bz;
        final float sx, sy, sz;
        final float seed;
        Base(float bx, float by, float bz, float sx, float sy, float sz, float seed) {
            this.bx = bx; this.by = by; this.bz = bz;
            this.sx = sx; this.sy = sy; this.sz = sz;
            this.seed = seed;
        }
    }

    /** Threshold above which an entity is considered "decorative" (unanimated). */
    private static final int DECOR_THRESHOLD = 30;

    /** Limit per-tick number of new entities adopted, to avoid spike on mass spawn. */
    private static final int ADOPT_PER_TICK = 64;

    /**
     * Minimum entity ticks-lived before we consider adopting it. Prevents racing
     * with attack onSpawn/onTick — short-lived attack-driven displays will have
     * dropped their interpolation below the threshold by then.
     */
    private static final int MIN_AGE_TICKS = 12;

    /** Don't adopt block displays — too risky (existing attacks animate them). */
    private static final boolean ADOPT_BLOCK_DISPLAYS = false;

    private final Map<UUID, Base> cache = new HashMap<>();
    /** Skip-set: entities we've decided NOT to adopt. Prevents repeated checks. */
    private final java.util.Set<UUID> skipSet = new java.util.HashSet<>();

    private FluffyDecor() {}

    /**
     * Sweep all "cc:fluffy"-tagged Display entities in the given world. Animate
     * any whose interpolation duration is >= DECOR_THRESHOLD with a continuous
     * BOB pattern (or SWAY for BlockDisplays).
     */
    public void tickWorld(World world, int tick) {
        if (world == null) return;
        int adoptedThisTick = 0;
        for (Entity e : world.getEntities()) {
            if (!(e instanceof Display)) continue;
            if (!e.getScoreboardTags().contains("cc:fluffy")) continue;
            if (!e.isValid()) continue;
            UUID id = e.getUniqueId();
            if (skipSet.contains(id)) continue;
            Display d = (Display) e;

            // Only adopt ItemDisplays; BlockDisplays are too often animated by
            // their own attack and adopting them risks fighting the attack's
            // onTick transformations.
            if (!ADOPT_BLOCK_DISPLAYS && d instanceof BlockDisplay) {
                skipSet.add(id);
                continue;
            }

            Base base = cache.get(id);
            if (base == null) {
                if (adoptedThisTick >= ADOPT_PER_TICK) continue;
                // Wait for the entity to settle past its initial fade-in before
                // checking. If after MIN_AGE_TICKS the interpolation is still
                // long, it really is a static decorative item.
                if (e.getTicksLived() < MIN_AGE_TICKS) continue;
                if (d.getInterpolationDuration() < DECOR_THRESHOLD) {
                    // Permanently skip — attack has taken over the animation.
                    skipSet.add(id);
                    continue;
                }

                Transformation t = d.getTransformation();
                Vector3f tr = t.getTranslation();
                Vector3f sc = t.getScale();
                base = new Base(tr.x, tr.y, tr.z, sc.x, sc.y, sc.z,
                        (float) (Math.random() * Math.PI * 2));
                cache.put(id, base);
                d.setInterpolationDuration(6);
                adoptedThisTick++;
            }

            // Apply animation
            double phase = tick * 0.1 + base.seed;

            if (d instanceof ItemDisplay) {
                float bobY = (float) (Math.sin(phase) * 0.18);
                float pulse = 1.0f + (float) (Math.sin(phase * 0.5) * 0.07);
                float rot = (float) (tick * 0.05 + base.seed);
                d.setInterpolationDelay(0);
                d.setInterpolationDuration(6);
                d.setTransformation(new Transformation(
                        new Vector3f(base.bx, base.by + bobY, base.bz),
                        new AxisAngle4f(rot, 0, 1, 0),
                        new Vector3f(base.sx * pulse, base.sy * pulse, base.sz * pulse),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        // Periodic cleanup: prune dead UUIDs from cache + skipSet. Every 256 ticks.
        if ((tick & 0xFF) == 0) {
            if (!cache.isEmpty()) {
                cache.entrySet().removeIf(entry -> {
                    Entity e = world.getEntity(entry.getKey());
                    return e == null || !e.isValid();
                });
            }
            if (!skipSet.isEmpty()) {
                skipSet.removeIf(uuid -> {
                    Entity e = world.getEntity(uuid);
                    return e == null || !e.isValid();
                });
            }
        }
    }

    public int trackedCount() { return cache.size(); }

    public void clear() {
        cache.clear();
        skipSet.clear();
    }
}
