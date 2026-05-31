package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — ENVIRONMENTAL ATTACKS 46-60 (Category 4: Structural Ambience)
 *
 * === VISIBILITY OVERHAUL ===
 * Player feedback: "too weak — sparse particles, tiny / missing ItemDisplays."
 * This batch is rebuilt to the SAME dense+visible standard as
 * ChainEnvironmental (1-15), ChainEnvironmental2 (16-30) and ChainEnvironmental3
 * (31-45). Structural ambience means real, BIG, built structures that fill the
 * arena: chain pillars, iron arches, barred windows, support cables, floor
 * gratings, forge vents, ceiling nets, machinery, tethers, scaffolding, anchor
 * rings, chain-wall curtains, spike skirting and lock mechanisms.
 *
 *   - Every structural member is now a THICK ItemDisplay (IRON_BARS / CHAIN /
 *     IRON_BLOCK / ANVIL / DARK_OAK_LOG / CHISELED_STONE_BRICKS / NETHERITE_BLOCK)
 *     scaled 0.4 x 1.2 x 0.4 MINIMUM for chains, far chunkier for posts/blocks.
 *     No more 0.06-0.18 specks. ItemDisplays ONLY — never BlockDisplays.
 *   - Heavy multi-layer particles: 3+ layers, each 5-15 particles per tick on
 *     average. CRIT / ELECTRIC_SPARK / SMOKE / SOUL_FIRE_FLAME / DUST /
 *     END_ROD / FALLING_DUST.
 *   - Every display glows from the SHARED palette (RUST / AMETHYST / IRON /
 *     LANTERN_GLOW / DARK_IRON) — same constants as the sibling files.
 *   - Spawn pattern matches the siblings exactly:
 *         spawnItem -> scale(0.001) -> glow -> interpolation(2,0)
 *     then a per-tick animateTo with a fade-driven scale ramp.
 *   - Nothing is static — even structural decor sways / rotates / bobs / pulses
 *     continuously via animateTo.
 *   - spawnedEntities.add(h.entity()) tracking + onCleanup() override.
 *
 * Critical contract: ALL of 46-60 are ambient (damage 0 / radius 0 — pure
 * decor). Long durations (1300-1500t) so the structure persists. Always
 * straight (yaw=0, pitch=0). UNIQUE vs all other modes — iron/chain/netherite.
 *
 * Design type prefix: "Structural ambience — <name> (no damage, decor)".
 */
public final class ChainEnvironmental4 {
    private ChainEnvironmental4() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    // Glow palettes (visibility) — shared with ChainEnvironmental (1-15) + (16-30) + (31-45)
    private static final int[] RUST = {200, 110, 40};       // industrial rust-orange
    private static final int[] IRON = {210, 210, 230};      // bright iron
    private static final int[] AMETHYST = {170, 80, 230};   // energy purple
    private static final int[] LANTERN_GLOW = {255, 180, 80};
    private static final int[] DARK_IRON = {120, 120, 150};

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainPillarWrap(plugin));
        registry.register(new IronArchSpan(plugin));
        registry.register(new BarredWindowEffect(plugin));
        registry.register(new ChainSupportCables(plugin));
        registry.register(new IronFloorGrating(plugin));
        registry.register(new ForgeGlowVents(plugin));
        registry.register(new ChainNetOverhead(plugin));
        registry.register(new WreckingBallDentMarks(plugin));
        registry.register(new IronMachineryAmbient(plugin));
        registry.register(new ChainTetherVisual(plugin));
        registry.register(new IronScaffoldingWall(plugin));
        registry.register(new AnchorChainFloorRing(plugin));
        registry.register(new ChainWallSection(plugin));
        registry.register(new IronSpikeFloorStrip(plugin));
        registry.register(new ChainLockMechanism(plugin));
    }

    // ============================================================
    // Shared helpers for structural attacks
    // ============================================================

    /** Apply a default ambient config: zero damage, long duration, low cooldown chance. */
    private static void configureAmbient(AttackConfig c, int durationTicks, int cooldownTicks) {
        c.setDamage(0.0);
        c.setDamageRadius(0.0);
        c.setTicksBetweenDamage(20);
        c.setDamageDelayTicks(0);
        c.setDamageOnImpactOnly(false);
        c.setImpactDamage(0.0);
        c.setImpactRadius(0.0);
        c.setTracksPlayer(false);
        c.setFollowAiEnabled(false);
        c.setChance(10);
        c.setDurationTicks(durationTicks);
        c.setCooldownTicks(cooldownTicks);
        c.setEnabled(true);
    }

    /**
     * Compute fade scale in 0..1 for spawn-in / hold / fade-out windows. Used to
     * drive the per-tick animateTo scale ramp (the siblings use the same idea via
     * Math.min ramps; this keeps the same look while letting structures hold then
     * fade gracefully).
     */
    private static float lifecycleScale(int tick, int duration, int spawnLen, int fadeLen) {
        if (tick < spawnLen) {
            return tick / (float) spawnLen;
        }
        int fadeStart = duration - fadeLen;
        if (tick >= fadeStart) {
            float t = 1f - ((tick - fadeStart) / (float) Math.max(1, fadeLen));
            return Math.max(0f, t);
        }
        return 1f;
    }

    /**
     * HEAVY layered ambient particle pass for a structural member — bright iron
     * dust + industrial smoke + crit shimmer + electric sparks. 3-4 layers,
     * several particles each (NOT ~1/tick). {@code col} picks the dust hue so
     * iron themes glow iron, forge themes glow rust/lantern, etc.
     */
    private static void ambientParticles(World w, Location p, int tick, int[] col) {
        DisplayBuilder.dustParticles(p, 5, 0.3, col[0], col[1], col[2], 1.3f);
        w.spawnParticle(Particle.SMOKE, p, 4, 0.25, 0.3, 0.25, 0.005);
        if (tick % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 4, 0.2, 0.25, 0.2, 0.03);
        if (tick % 3 == 0) w.spawnParticle(Particle.CRIT, p, 3, 0.2, 0.25, 0.2, 0.04);
        if (tick % 4 == 0) w.spawnParticle(Particle.FALLING_DUST, p, 2, 0.15, 0.2, 0.15, 0.0,
                Material.IRON_BLOCK.createBlockData());
    }

    /** Rust drip beneath a hanging chain/iron member — the "everything is rusting" look. */
    private static void rustDrip(World w, Location p, int tick) {
        DisplayBuilder.dustParticles(p.clone().add(0, -0.4, 0), 4, 0.18, 190, 100, 35, 1.3f);
        w.spawnParticle(Particle.SMOKE, p, 2, 0.12, 0.2, 0.12, 0.005);
        if (tick % 3 == 0) w.spawnParticle(Particle.FALLING_DUST, p.clone().add(0, -0.5, 0), 1, 0.08, 0.12, 0.08, 0.0,
                Material.CHAIN.createBlockData());
        if (tick % 6 == 0) w.spawnParticle(Particle.CRIT, p, 3, 0.1, 0.15, 0.1, 0.03);
    }

    // ================================================================
    // 46. CHAIN PILLAR WRAP — "The Column" — AMBIENT (no damage)
    //     4 BIG arena pillars (thick CHISELED_STONE_BRICKS shafts) with heavy
    //     helical CHAIN wraps that slowly rotate around them, rust drip beneath.
    //     Design type: Structural ambience — chain pillar wrap (no damage, decor)
    // ================================================================
    public static class ChainPillarWrap extends EnvironmentalAttack {
        private static final int PILLARS = 4;
        private static final int LINKS_PER = 10;
        private static final int SHAFT_SEGS = 5;
        private static final double PILLAR_R = 7.0;        // arena radius for pillar placement
        private static final double WRAP_RADIUS = 0.85;
        private static final double HELIX_HEIGHT = 5.0;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final List<ItemDisplayHandle> shafts = new ArrayList<>();
        private final double[] pillarX = new double[PILLARS];
        private final double[] pillarZ = new double[PILLARS];
        private static final int SPAWN_LEN = 26;
        private static final int FADE_LEN = 40;

        public ChainPillarWrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_pillar_wrap", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 160);
            config.setDesignType("Structural ambience — chain pillar wrap (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.6f);
            for (int p = 0; p < PILLARS; p++) {
                double a = Math.PI * 2 * p / PILLARS + Math.PI / 4;
                pillarX[p] = Math.cos(a) * PILLAR_R;
                pillarZ[p] = Math.sin(a) * PILLAR_R;
                // Thick stone shaft segments
                for (int s = 0; s < SHAFT_SEGS; s++) {
                    double y = (s + 0.5) * (HELIX_HEIGHT / SHAFT_SEGS);
                    ItemDisplayHandle h = displayBuilder.spawnItem(
                            c.clone().add(pillarX[p], y, pillarZ[p]), new ItemStack(Material.CHISELED_STONE_BRICKS));
                    h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                    shafts.add(h); spawnedEntities.add(h.entity());
                }
                // Helical chain wrap
                for (int i = 0; i < LINKS_PER; i++) {
                    double t = i / (double) (LINKS_PER - 1);
                    double y = t * HELIX_HEIGHT;
                    double helixA = t * Math.PI * 2 * 2.0;
                    double x = pillarX[p] + Math.cos(helixA) * WRAP_RADIUS;
                    double z = pillarZ[p] + Math.sin(helixA) * WRAP_RADIUS;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    links.add(h); spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            float pulse = (float)(1.0 + Math.sin(tick * 0.06) * 0.05);

            // Stone shafts — big, very slow breathing rotation
            for (int p = 0; p < PILLARS; p++) {
                for (int s = 0; s < SHAFT_SEGS; s++) {
                    int idx = p * SHAFT_SEGS + s;
                    if (idx >= shafts.size()) break;
                    double y = (s + 0.5) * (HELIX_HEIGHT / SHAFT_SEGS);
                    float sxz = 0.95f * fade;
                    float syy = (float)(HELIX_HEIGHT / SHAFT_SEGS) * fade;
                    shafts.get(idx).animateTo(
                            new Vector3f((float) pillarX[p] - sxz / 2, (float) y - syy / 2, (float) pillarZ[p] - sxz / 2),
                            new AxisAngle4f((float)(tick * 0.004), 0, 1, 0),
                            new Vector3f(sxz, syy, sxz), 6);
                }
            }
            // Helical chains — 2 deg/tick rotation, thick links
            float linkXZ = 0.45f * fade * pulse;
            float linkY = 1.2f * fade * pulse;
            for (int p = 0; p < PILLARS; p++) {
                for (int i = 0; i < LINKS_PER; i++) {
                    int idx = p * LINKS_PER + i;
                    if (idx >= links.size()) break;
                    double t = i / (double) (LINKS_PER - 1);
                    double y = t * HELIX_HEIGHT;
                    double helixA = t * Math.PI * 2 * 2.0 + Math.toRadians(2.0) * tick;
                    double x = pillarX[p] + Math.cos(helixA) * WRAP_RADIUS;
                    double z = pillarZ[p] + Math.sin(helixA) * WRAP_RADIUS;
                    links.get(idx).animateTo(
                            new Vector3f((float) x - linkXZ / 2, (float) y - linkY / 2, (float) z - linkXZ / 2),
                            new AxisAngle4f((float) helixA, 0, 1, 0),
                            new Vector3f(linkXZ, linkY, linkXZ), 4);
                }
                Location pcenter = c.clone().add(pillarX[p], HELIX_HEIGHT * 0.5, pillarZ[p]);
                ambientParticles(w, pcenter, tick + p, RUST);
                if (tick % 12 == 0) rustDrip(w, c.clone().add(pillarX[p], HELIX_HEIGHT, pillarZ[p]), tick);
            }
            if (tick % 120 == 60) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainPillarWrap(plugin); }
    }

    // ================================================================
    // 47. IRON ARCH SPAN — "The Gate" — AMBIENT (no damage)
    //     A BIG half-circle gate: 7 chunky IRON_BLOCK voussoirs + ANVIL keystone
    //     + 4 NETHERITE_BLOCK footing blocks + 3 hanging CHAIN keystone links.
    //     Keystone sparks; chains sway. Fills a whole wall.
    //     Design type: Structural ambience — iron arch span (no damage, decor)
    // ================================================================
    public static class IronArchSpan extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> arch = new ArrayList<>();
        private final List<ItemDisplayHandle> corners = new ArrayList<>();
        private final List<ItemDisplayHandle> keyChains = new ArrayList<>();
        private ItemDisplayHandle keystone;
        private static final int VOUSSOIRS = 7;
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 50;
        private static final double ARCH_R = 5.0;
        private static final double ARCH_X = 8.0;

        public IronArchSpan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_arch_span", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — iron arch span (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.4f);
            for (int i = 0; i < VOUSSOIRS; i++) {
                double a = Math.PI * (i + 0.5) / VOUSSOIRS;
                double y = Math.sin(a) * ARCH_R + 0.5;
                double zRel = Math.cos(a) * ARCH_R;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(ARCH_X, y, zRel), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                arch.add(h); spawnedEntities.add(h.entity());
            }
            // ANVIL keystone at apex
            keystone = displayBuilder.spawnItem(c.clone().add(ARCH_X, ARCH_R + 0.9, 0), new ItemStack(Material.ANVIL));
            keystone.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(keystone.entity());
            // 4 NETHERITE footing blocks
            double[][] cornerOffsets = {
                    {ARCH_X - 0.3, 0.5, -ARCH_R - 0.3}, {ARCH_X - 0.3, 0.5, ARCH_R + 0.3},
                    {ARCH_X + 0.3, 0.5, -ARCH_R - 0.3}, {ARCH_X + 0.3, 0.5, ARCH_R + 0.3}
            };
            for (double[] o : cornerOffsets) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(o[0], o[1], o[2]), new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                corners.add(h); spawnedEntities.add(h.entity());
            }
            // 3 hanging keystone chains
            for (int i = 0; i < 3; i++) {
                double zOff = (i - 1) * 0.35;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(ARCH_X, ARCH_R - 0.2, zOff), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                keyChains.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            float breathe = (float)(1.0 + Math.sin(tick * 0.03) * 0.02);

            for (int i = 0; i < arch.size(); i++) {
                double a = Math.PI * (i + 0.5) / VOUSSOIRS;
                double y = Math.sin(a) * ARCH_R + 0.5;
                double zRel = Math.cos(a) * ARCH_R;
                float s = 1.0f * fade * breathe;
                arch.get(i).animateTo(
                        new Vector3f((float) ARCH_X - s / 2, (float) y - s / 2, (float) zRel - s / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            if (keystone != null) {
                float s = 0.9f * fade * breathe;
                keystone.animateTo(
                        new Vector3f((float) ARCH_X - s / 2, (float)(ARCH_R + 0.9) - s / 2, 0f - s / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            for (int i = 0; i < corners.size(); i++) {
                float s = 0.6f * fade;
                double[] o = {(i < 2 ? ARCH_X - 0.3 : ARCH_X + 0.3), 0.5, ((i % 2 == 0) ? -ARCH_R - 0.3 : ARCH_R + 0.3)};
                corners.get(i).animateTo(
                        new Vector3f((float) o[0] - s / 2, (float) o[1] - s / 2, (float) o[2] - s / 2),
                        new AxisAngle4f((float)(tick * 0.005), 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            float sway = (float)(Math.sin(tick * 0.04) * 0.12);
            for (int i = 0; i < keyChains.size(); i++) {
                float sxz = 0.42f * fade;
                float sy = 1.2f * fade;
                double zOff = (i - 1) * 0.35;
                keyChains.get(i).animateTo(
                        new Vector3f((float) ARCH_X - sxz / 2, (float)(ARCH_R - 0.2 + sway) - sy / 2, (float) zOff - sxz / 2),
                        new AxisAngle4f(sway, 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            Location apex = c.clone().add(ARCH_X, ARCH_R + 0.9, 0);
            ambientParticles(w, apex, tick, IRON);
            if (tick % 6 == 0) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, apex, 5, 0.4, 0.2, 0.4, 0.02);
                rustDrip(w, apex.clone().add(0, -0.4, 0), tick);
            }
            if (tick % 200 == 80) DisplayBuilder.playSound(apex, Sound.BLOCK_ANVIL_PLACE, 0.25f, 0.4f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronArchSpan(plugin); }
    }

    // ================================================================
    // 48. BARRED WINDOW EFFECT — "The Cell" — AMBIENT (no damage)
    //     2 BIG barred windows: each = IRON_BLOCK frame (4 edges) + 4 thick
    //     IRON_BARS verticals + 2 horizontals. Heavy SMOKE pours inward through
    //     the bars; bars rattle subtly.
    //     Design type: Structural ambience — barred window (no damage, decor)
    // ================================================================
    public static class BarredWindowEffect extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> verticals = new ArrayList<>();
        private final List<ItemDisplayHandle> horizontals = new ArrayList<>();
        private final List<ItemDisplayHandle> frame = new ArrayList<>();
        private static final int SPAWN_LEN = 28;
        private static final int FADE_LEN = 40;
        private static final double WIN_H = 3.2;
        private static final double WIN_W = 2.6;
        private static final double[][] WIN_CENTERS = {
                {-7.6, 2.8, 1.5}, { 7.6, 2.8,-1.5}
        };

        public BarredWindowEffect(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("barred_window_effect", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1300, 150);
            config.setDesignType("Structural ambience — barred window (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
            for (double[] center : WIN_CENTERS) {
                // Frame: top + bottom (horizontal IRON_BLOCK), left + right (vertical IRON_BLOCK)
                double[][] frameOffsets = {
                        {0,  WIN_H / 2, 0}, {0, -WIN_H / 2, 0},  // top, bottom
                        {0, 0, -WIN_W / 2}, {0, 0,  WIN_W / 2}   // left, right
                };
                for (double[] o : frameOffsets) {
                    Location loc = c.clone().add(center[0] + o[0], center[1] + o[1], center[2] + o[2]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BLOCK));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    frame.add(h); spawnedEntities.add(h.entity());
                }
                // 4 vertical bars
                for (int i = 0; i < 4; i++) {
                    double zOff = (i - 1.5) * (WIN_W / 4.5);
                    Location loc = c.clone().add(center[0], center[1], center[2] + zOff);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BARS));
                    h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                    verticals.add(h); spawnedEntities.add(h.entity());
                }
                // 2 horizontal bars
                for (int i = 0; i < 2; i++) {
                    double yOff = (i == 0 ? -0.7 : 0.7);
                    Location loc = c.clone().add(center[0], center[1] + yOff, center[2]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BARS));
                    h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                    horizontals.add(h); spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            float rattle = (float)(Math.sin(tick * 0.5) * 0.02);

            int fi = 0, vi = 0, hi = 0;
            for (double[] center : WIN_CENTERS) {
                double[][] frameOffsets = {
                        {0,  WIN_H / 2, 0}, {0, -WIN_H / 2, 0},
                        {0, 0, -WIN_W / 2}, {0, 0,  WIN_W / 2}
                };
                for (int k = 0; k < frameOffsets.length; k++) {
                    if (fi >= frame.size()) break;
                    double[] o = frameOffsets[k];
                    boolean horiz = (k < 2);
                    float sx = 0.16f * fade;
                    float sy = horiz ? 0.16f * fade : (float) WIN_H * fade;
                    float sz = horiz ? (float) WIN_W * fade : 0.16f * fade;
                    frame.get(fi).animateTo(
                            new Vector3f((float)(center[0] + o[0]) - sx / 2, (float)(center[1] + o[1]) - sy / 2, (float)(center[2] + o[2]) - sz / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sx, sy, sz), 6);
                    fi++;
                }
                // verticals (thick IRON_BARS)
                for (int i = 0; i < 4; i++) {
                    if (vi >= verticals.size()) break;
                    double zOff = (i - 1.5) * (WIN_W / 4.5);
                    float sx = 0.42f * fade;
                    float sy = (float)(WIN_H - 0.3) * fade;
                    float sz = 0.42f * fade;
                    verticals.get(vi).animateTo(
                            new Vector3f((float) center[0] - sx / 2, (float)(center[1] + rattle) - sy / 2, (float)(center[2] + zOff) - sz / 2),
                            new AxisAngle4f(rattle, 0, 0, 1),
                            new Vector3f(sx, sy, sz), 5);
                    vi++;
                }
                for (int i = 0; i < 2; i++) {
                    if (hi >= horizontals.size()) break;
                    double yOff = (i == 0 ? -0.7 : 0.7);
                    float sx = 0.42f * fade;
                    float sy = 0.42f * fade;
                    float sz = (float)(WIN_W - 0.3) * fade;
                    horizontals.get(hi).animateTo(
                            new Vector3f((float) center[0] - sx / 2, (float)(center[1] + yOff) - sy / 2, (float) center[2] - sz / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sx, sy, sz), 6);
                    hi++;
                }

                // Heavy SMOKE drift inward through the bars (3-layer)
                Location wc = c.clone().add(center[0], center[1], center[2]);
                double dirX = -Math.signum(center[0]);
                w.spawnParticle(Particle.SMOKE, wc, 6, 0.1, 0.7, 0.7, 0.0);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, wc.clone().add(dirX * 0.4, 0, 0), 3, 0.15, 0.6, 0.6, 0.01);
                DisplayBuilder.dustParticles(wc, 4, 0.6, DARK_IRON[0], DARK_IRON[1], DARK_IRON[2], 1.2f);
                if (tick % 3 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, wc, 3, 0.4, 0.6, 0.5, 0.02);
            }
            if (tick % 300 == 120) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.25f, 0.45f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BarredWindowEffect(plugin); }
    }

    // ================================================================
    // 49. CHAIN SUPPORT CABLES — "The Infrastructure" — AMBIENT (no damage)
    //     4 heavy diagonal CHAIN cables from ceiling corners to wall anchors,
    //     each 10 thick links with a real catenary sag + sag oscillation, plus
    //     an IRON_BLOCK anchor plate at each end. Heavy iron-dust trails.
    //     Design type: Structural ambience — chain support cables (no damage, decor)
    // ================================================================
    public static class ChainSupportCables extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cableLinks = new ArrayList<>();
        private final List<ItemDisplayHandle> anchors = new ArrayList<>();
        private final List<double[]> linkInfo = new ArrayList<>(); // {baseX, baseY, baseZ, sagFactor}
        private static final int LINKS_PER = 10;
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 45;
        private static final double[][] STARTS = {
                {-7.0, 9.0, -7.0}, { 7.0, 9.0, -7.0}, { 7.0, 9.0,  7.0}, {-7.0, 9.0,  7.0}
        };
        private static final double[][] ENDS = {
                {-6.0, 3.0,  3.0}, { 6.0, 3.0,  3.0}, { 6.0, 3.0, -3.0}, {-6.0, 3.0, -3.0}
        };

        public ChainSupportCables(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_support_cables", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 170);
            config.setDesignType("Structural ambience — chain support cables (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.4f);
            for (int cable = 0; cable < 4; cable++) {
                for (int i = 0; i < LINKS_PER; i++) {
                    double t = i / (double) (LINKS_PER - 1);
                    double x = STARTS[cable][0] + (ENDS[cable][0] - STARTS[cable][0]) * t;
                    double y = STARTS[cable][1] + (ENDS[cable][1] - STARTS[cable][1]) * t;
                    double z = STARTS[cable][2] + (ENDS[cable][2] - STARTS[cable][2]) * t;
                    double sagFactor = Math.sin(t * Math.PI);
                    y -= sagFactor * 1.2; // real catenary droop
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    cableLinks.add(h); spawnedEntities.add(h.entity());
                    linkInfo.add(new double[]{x, y, z, sagFactor});
                }
                // Anchor plates at both ends
                for (double[] end : new double[][]{STARTS[cable], ENDS[cable]}) {
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(end[0], end[1], end[2]), new ItemStack(Material.IRON_BLOCK));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    anchors.add(h); spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            double sagOsc = Math.sin(tick * (Math.PI * 2 / 80.0)) * 0.06;

            float sxz = 0.42f * fade;
            float sy = 1.2f * fade;
            for (int i = 0; i < cableLinks.size(); i++) {
                double[] info = linkInfo.get(i);
                double x = info[0];
                double y = info[1] + sagOsc * info[3];
                double z = info[2];
                cableLinks.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, (float) y - sy / 2, (float) z - sxz / 2),
                        new AxisAngle4f(0.6f + (float) Math.toRadians(tick * 0.4), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            for (int i = 0; i < anchors.size(); i++) {
                int cable = i / 2;
                double[] end = (i % 2 == 0) ? STARTS[cable] : ENDS[cable];
                float s = 0.55f * fade;
                anchors.get(i).animateTo(
                        new Vector3f((float) end[0] - s / 2, (float) end[1] - s / 2, (float) end[2] - s / 2),
                        new AxisAngle4f((float)(tick * 0.004), 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }

            for (int cable = 0; cable < 4; cable++) {
                double mx = (STARTS[cable][0] + ENDS[cable][0]) / 2.0;
                double my = (STARTS[cable][1] + ENDS[cable][1]) / 2.0 - 1.2;
                double mz = (STARTS[cable][2] + ENDS[cable][2]) / 2.0;
                Location p = c.clone().add(mx, my, mz);
                ambientParticles(w, p, tick + cable, IRON);
                if (tick % 6 == 0) rustDrip(w, p, tick);
            }
            if (tick % 180 == 90) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.25f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainSupportCables(plugin); }
    }

    // ================================================================
    // 50. IRON FLOOR GRATING OVERLAY — "The Grid" — AMBIENT (no damage)
    //     A big 4x4m floor grate: 6 thick IRON_BARS along X + 6 along Z + 4
    //     IRON_BLOCK corner anchors + ANVIL center boss. ELECTRIC_SPARK + flame
    //     glow rises through the grid (forge below). Slow corner spin.
    //     Design type: Structural ambience — iron floor grating (no damage, decor)
    // ================================================================
    public static class IronFloorGrating extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bars = new ArrayList<>();
        private final List<ItemDisplayHandle> cornerBlocks = new ArrayList<>();
        private ItemDisplayHandle boss;
        private static final int SPAWN_LEN = 26;
        private static final int FADE_LEN = 40;
        private static final double GRATE_CX = 6.0;
        private static final double GRATE_CZ = 0.0;
        private static final double GRATE_SIZE = 4.0;

        public IronFloorGrating(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_floor_grating", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 160);
            config.setDesignType("Structural ambience — iron floor grating (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.7f);
            for (int i = 0; i < 6; i++) { // along X
                double zOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX, 0.08, GRATE_CZ + zOff), new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                bars.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) { // along Z
                double xOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX + xOff, 0.08, GRATE_CZ), new ItemStack(Material.IRON_BARS));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                bars.add(h); spawnedEntities.add(h.entity());
            }
            double half = GRATE_SIZE / 2;
            double[][] corners = {{-half,-half},{half,-half},{half,half},{-half,half}};
            for (double[] o : corners) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX + o[0], 0.12, GRATE_CZ + o[1]), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                cornerBlocks.add(h); spawnedEntities.add(h.entity());
            }
            boss = displayBuilder.spawnItem(c.clone().add(GRATE_CX, 0.12, GRATE_CZ), new ItemStack(Material.ANVIL));
            boss.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
            spawnedEntities.add(boss.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);

            for (int i = 0; i < 6; i++) {
                if (i >= bars.size()) break;
                double zOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                float sx = (float) GRATE_SIZE * fade;
                float sy = 0.42f * fade;
                float sz = 0.42f * fade;
                bars.get(i).animateTo(
                        new Vector3f((float) GRATE_CX - sx / 2, 0.08f, (float)(GRATE_CZ + zOff) - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < 6; i++) {
                int idx = 6 + i;
                if (idx >= bars.size()) break;
                double xOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                float sx = 0.42f * fade;
                float sy = 0.42f * fade;
                float sz = (float) GRATE_SIZE * fade;
                bars.get(idx).animateTo(
                        new Vector3f((float)(GRATE_CX + xOff) - sx / 2, 0.08f, (float) GRATE_CZ - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            double half = GRATE_SIZE / 2;
            double[][] corners = {{-half,-half},{half,-half},{half,half},{-half,half}};
            for (int i = 0; i < cornerBlocks.size(); i++) {
                float s = 0.5f * fade;
                cornerBlocks.get(i).animateTo(
                        new Vector3f((float)(GRATE_CX + corners[i][0]) - s / 2, 0.12f, (float)(GRATE_CZ + corners[i][1]) - s / 2),
                        new AxisAngle4f(tick * 0.01f, 0, 1, 0),
                        new Vector3f(s, s, s), 8);
            }
            if (boss != null) {
                float s = 0.6f * fade;
                boss.animateTo(
                        new Vector3f((float) GRATE_CX - s / 2, 0.12f, (float) GRATE_CZ - s / 2),
                        new AxisAngle4f((float)(tick * 0.02), 0, 1, 0),
                        new Vector3f(s, s * 0.5f, s), 6);
            }

            // ELECTRIC_SPARK + forge flame glow rising through the grid
            Location gc = c.clone().add(GRATE_CX, 0.1, GRATE_CZ);
            ambientParticles(w, gc, tick, LANTERN_GLOW);
            w.spawnParticle(Particle.ELECTRIC_SPARK, gc, 6, GRATE_SIZE / 2, 0.05, GRATE_SIZE / 2, 0.02);
            if (tick % 3 == 0) w.spawnParticle(Particle.FLAME, gc, 3, GRATE_SIZE / 2.5, 0.05, GRATE_SIZE / 2.5, 0.01);
            if (tick % 4 == 0) DisplayBuilder.dustParticles(gc.clone().add(0, 0.5, 0), 4, GRATE_SIZE / 3, 255, 130, 60, 1.2f);
            if (tick % 240 == 80) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.6f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronFloorGrating(plugin); }
    }

    // ================================================================
    // 51. FORGE GLOW VENTS — "The Below" — AMBIENT (no damage)
    //     4 big floor vent assemblies: IRON_BLOCK vent plate ringed by 4 thick
    //     IRON_BARS grilles, FLAME + SOUL_FIRE_FLAME columns roaring up, heat
    //     vibration on the plate. Forge ambient roar.
    //     Design type: Structural ambience — forge glow vents (no damage, decor)
    // ================================================================
    public static class ForgeGlowVents extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> plates = new ArrayList<>();
        private final List<ItemDisplayHandle> grilles = new ArrayList<>();
        private static final int SPAWN_LEN = 22;
        private static final int FADE_LEN = 35;
        private static final double[][] VENT_POS = {
                {-3.5, 0, -3.5}, { 3.5, 0, -3.5}, {-3.5, 0,  3.5}, { 3.5, 0,  3.5}
        };

        public ForgeGlowVents(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forge_glow_vents", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 170);
            config.setDesignType("Structural ambience — forge glow vents (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.5f);
            for (double[] p : VENT_POS) {
                ItemDisplayHandle plate = displayBuilder.spawnItem(c.clone().add(p[0], 0.08, p[2]), new ItemStack(Material.IRON_BLOCK));
                plate.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
                plates.add(plate); spawnedEntities.add(plate.entity());
                // 4 grille bars around each vent
                double[][] gOff = {{-0.5,0},{0.5,0},{0,-0.5},{0,0.5}};
                for (double[] g : gOff) {
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(p[0] + g[0], 0.12, p[2] + g[1]), new ItemStack(Material.IRON_BARS));
                    h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                    grilles.add(h); spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            float heat = (float)(1.0 + Math.sin(tick * 0.25) * 0.18); // heat vibration

            for (int i = 0; i < plates.size(); i++) {
                double[] p = VENT_POS[i];
                float sx = 0.9f * fade;
                float sy = 0.18f * fade * heat;
                float sz = 0.9f * fade;
                plates.get(i).animateTo(
                        new Vector3f((float) p[0] - sx / 2, 0.08f, (float) p[2] - sz / 2),
                        new AxisAngle4f((float)(tick * 0.005 + i), 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }
            double[][] gOff = {{-0.5,0},{0.5,0},{0,-0.5},{0,0.5}};
            for (int i = 0; i < grilles.size(); i++) {
                int vent = i / 4;
                double[] g = gOff[i % 4];
                double[] p = VENT_POS[vent];
                float sx = 0.4f * fade;
                float sy = 0.42f * fade;
                float sz = 0.4f * fade;
                grilles.get(i).animateTo(
                        new Vector3f((float)(p[0] + g[0]) - sx / 2, 0.12f, (float)(p[2] + g[1]) - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }

            // Roaring fire columns through each vent (heavy, multi-layer)
            for (double[] p : VENT_POS) {
                Location vc = c.clone().add(p[0], 0.2, p[2]);
                w.spawnParticle(Particle.FLAME, vc, 6, 0.25, 0.2, 0.25, 0.06);
                if (tick % 2 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, vc, 4, 0.2, 0.2, 0.2, 0.04);
                if (tick % 3 == 0) DisplayBuilder.dustParticles(vc.clone().add(0, 0.4, 0), 4, 0.3, 255, 120, 30, 1.4f);
                if (tick % 4 == 0) w.spawnParticle(Particle.SMOKE, vc.clone().add(0, 1.0, 0), 3, 0.3, 0.4, 0.3, 0.02);
                if (tick % 6 == 0) w.spawnParticle(Particle.LAVA, vc, 1, 0.2, 0.1, 0.2, 0.0);
            }
            if (tick % 60 == 30) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.25f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ForgeGlowVents(plugin); }
    }

    // ================================================================
    // 52. CHAIN NET OVERHEAD — "The Ceiling Trap" — AMBIENT (no damage)
    //     A big 6m chain net at Y=5: 6x6 grid of thick CHAIN links + 4 IRON_BLOCK
    //     corner anchors, sagging toward the center, sway oscillation. Debris
    //     and sparks rain from the net.
    //     Design type: Structural ambience — chain net overhead (no damage, decor)
    // ================================================================
    public static class ChainNetOverhead extends EnvironmentalAttack {
        private static final int GRID = 6;
        private final List<ItemDisplayHandle> netLinks = new ArrayList<>();
        private final List<ItemDisplayHandle> anchors = new ArrayList<>();
        private final double[][] linkBase = new double[GRID * GRID][2];
        private static final int SPAWN_LEN = 28;
        private static final int FADE_LEN = 40;
        private static final double NET_Y = 5.0;
        private static final double NET_SIZE = 6.0;

        public ChainNetOverhead(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_net_overhead", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 165);
            config.setDesignType("Structural ambience — chain net overhead (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.45f);
            int idx = 0;
            for (int row = 0; row < GRID; row++) {
                for (int col = 0; col < GRID; col++) {
                    double x = -NET_SIZE / 2 + col * (NET_SIZE / (GRID - 1));
                    double z = -NET_SIZE / 2 + row * (NET_SIZE / (GRID - 1));
                    linkBase[idx][0] = x; linkBase[idx][1] = z;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, NET_Y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    netLinks.add(h); spawnedEntities.add(h.entity());
                    idx++;
                }
            }
            double half = NET_SIZE / 2;
            for (double[] o : new double[][]{{-half,-half},{half,-half},{half,half},{-half,half}}) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(o[0], NET_Y + 0.2, o[1]), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                anchors.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            double sway = Math.sin(tick * (Math.PI * 2 / 80.0)) * 0.08;

            float sxz = 0.42f * fade;
            float sy = 1.2f * fade;
            for (int i = 0; i < netLinks.size(); i++) {
                double bx = linkBase[i][0];
                double bz = linkBase[i][1];
                // Sag toward center + sway near edges
                double centerSag = -((NET_SIZE / 2) - (Math.abs(bx) + Math.abs(bz)) / 2.0) * 0.12;
                double edgeWeight = (Math.abs(bx) + Math.abs(bz)) / NET_SIZE;
                double dy = centerSag + sway * edgeWeight;
                netLinks.get(i).animateTo(
                        new Vector3f((float) bx - sxz / 2, (float)(NET_Y + dy) - sy / 2, (float) bz - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i * 0.13), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }
            double half = NET_SIZE / 2;
            double[][] aPos = {{-half,-half},{half,-half},{half,half},{-half,half}};
            for (int i = 0; i < anchors.size(); i++) {
                float s = 0.5f * fade;
                anchors.get(i).animateTo(
                        new Vector3f((float) aPos[i][0] - s / 2, (float)(NET_Y + 0.2) - s / 2, (float) aPos[i][1] - s / 2),
                        new AxisAngle4f((float)(tick * 0.004), 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }

            // Heavy debris + sparks raining from the net
            for (int k = 0; k < 4; k++) {
                double rx = (Math.random() - 0.5) * NET_SIZE;
                double rz = (Math.random() - 0.5) * NET_SIZE;
                Location drop = c.clone().add(rx, NET_Y - 0.3, rz);
                w.spawnParticle(Particle.FALLING_DUST, drop, 2, 0.15, 0.3, 0.15, 0.0, Material.CHAIN.createBlockData());
                w.spawnParticle(Particle.SMOKE, drop, 2, 0.12, 0.2, 0.12, 0.0);
                if (k % 2 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, drop, 2, 0.1, 0.2, 0.1, 0.02);
            }
            DisplayBuilder.dustParticles(c.clone().add(0, NET_Y - 0.2, 0), 6, NET_SIZE / 2.5, IRON[0], IRON[1], IRON[2], 1.2f);
            if (tick % 200 == 60) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainNetOverhead(plugin); }
    }

    // ================================================================
    // 53. WRECKING BALL DENT MARKS — "The History" — AMBIENT (no damage)
    //     6 big sunken impact craters: each = flat GRAY_CONCRETE_POWDER disc +
    //     a half-buried IRON_BLOCK chunk + ring of 4 small CHAIN shards. Smoke
    //     curls from each. Marks the arena's violent history.
    //     Design type: Structural ambience — wrecking ball dent marks (no damage, decor)
    // ================================================================
    public static class WreckingBallDentMarks extends EnvironmentalAttack {
        private static final int DENTS = 6;
        private final List<ItemDisplayHandle> discs = new ArrayList<>();
        private final List<ItemDisplayHandle> chunks = new ArrayList<>();
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[][] dentPos = new double[DENTS][2];
        private static final int SPAWN_LEN = 32;
        private static final int FADE_LEN = 55;

        public WreckingBallDentMarks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_dent_marks", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — wrecking ball dent marks (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.4f);
            for (int i = 0; i < DENTS; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 4.5;
                dentPos[i][0] = Math.cos(a) * r;
                dentPos[i][1] = Math.sin(a) * r;
                Location base = c.clone().add(dentPos[i][0], 0.03, dentPos[i][1]);
                ItemDisplayHandle disc = displayBuilder.spawnItem(base, new ItemStack(Material.GRAY_CONCRETE_POWDER));
                disc.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                discs.add(disc); spawnedEntities.add(disc.entity());
                ItemDisplayHandle chunk = displayBuilder.spawnItem(base.clone().add(0, 0.05, 0), new ItemStack(Material.IRON_BLOCK));
                chunk.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                chunks.add(chunk); spawnedEntities.add(chunk.entity());
                for (int k = 0; k < 4; k++) {
                    double sa = Math.PI * 2 * k / 4;
                    Location sp = base.clone().add(Math.cos(sa) * 0.7, 0.04, Math.sin(sa) * 0.7);
                    ItemDisplayHandle sh = displayBuilder.spawnItem(sp, new ItemStack(Material.CHAIN));
                    sh.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    shards.add(sh); spawnedEntities.add(sh.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);

            for (int i = 0; i < discs.size(); i++) {
                float sxz = (float)(1.8 + Math.sin(tick * 0.02 + i) * 0.1) * fade;
                discs.get(i).animateTo(
                        new Vector3f((float) dentPos[i][0] - sxz / 2, 0.03f, (float) dentPos[i][1] - sxz / 2),
                        new AxisAngle4f((float)(i * 0.3 + tick * 0.002), 0, 1, 0),
                        new Vector3f(sxz, 0.03f * fade, sxz), 10);
            }
            for (int i = 0; i < chunks.size(); i++) {
                float s = 0.6f * fade;
                chunks.get(i).animateTo(
                        new Vector3f((float) dentPos[i][0] - s / 2, 0.08f - s / 2, (float) dentPos[i][1] - s / 2),
                        new AxisAngle4f((float)(i + tick * 0.004), 0.3f, 1, 0.2f),
                        new Vector3f(s, s, s), 8);
            }
            for (int i = 0; i < shards.size(); i++) {
                int dent = i / 4;
                double sa = Math.PI * 2 * (i % 4) / 4;
                float sxz = 0.42f * fade;
                float sy = 0.6f * fade;
                shards.get(i).animateTo(
                        new Vector3f((float)(dentPos[dent][0] + Math.cos(sa) * 0.7) - sxz / 2, 0.04f, (float)(dentPos[dent][1] + Math.sin(sa) * 0.7) - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            for (int i = 0; i < DENTS; i++) {
                Location dl = c.clone().add(dentPos[i][0], 0.15, dentPos[i][1]);
                if ((tick + i) % 2 == 0) w.spawnParticle(Particle.SMOKE, dl, 3, 0.3, 0.1, 0.3, 0.005);
                if ((tick + i) % 3 == 0) DisplayBuilder.dustParticles(dl, 4, 0.4, 80, 80, 85, 1.2f);
                if ((tick + i) % 5 == 0) w.spawnParticle(Particle.FALLING_DUST, dl, 2, 0.3, 0.1, 0.3, 0.0, Material.GRAY_CONCRETE_POWDER.createBlockData());
                if ((tick + i) % 8 == 0) w.spawnParticle(Particle.CRIT, dl, 2, 0.2, 0.2, 0.2, 0.03);
            }
            if (tick % 260 == 100) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.3f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new WreckingBallDentMarks(plugin); }
    }

    // ================================================================
    // 54. IRON MACHINERY AMBIENT — "The Workshop" — AMBIENT (no damage)
    //     A big wall-mounted machine: 2 large IRON_BLOCK gears spinning at
    //     independent rates, 2 DARK_OAK_LOG axles, an ANVIL housing, and a
    //     6-link CHAIN drive belt that shifts. Sparks + smoke + grind sound.
    //     Design type: Structural ambience — iron machinery (no damage, decor)
    // ================================================================
    public static class IronMachineryAmbient extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> gears = new ArrayList<>();
        private final List<ItemDisplayHandle> axles = new ArrayList<>();
        private final List<ItemDisplayHandle> driveLinks = new ArrayList<>();
        private ItemDisplayHandle housing;
        private static final int DRIVE_LINKS = 6;
        private static final int SPAWN_LEN = 28;
        private static final int FADE_LEN = 45;
        private static final double WALL_X = -7.8;

        public IronMachineryAmbient(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_machinery_ambient", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 175);
            config.setDesignType("Structural ambience — iron machinery (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.3f, 0.4f);
            housing = displayBuilder.spawnItem(c.clone().add(WALL_X, 3.5, 0), new ItemStack(Material.ANVIL));
            housing.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(housing.entity());
            double[][] gearPos = {{WALL_X, 3.5, -1.6}, {WALL_X, 3.5, 1.6}};
            for (double[] p : gearPos) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(p[0], p[1], p[2]), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                gears.add(h); spawnedEntities.add(h.entity());
                ItemDisplayHandle ax = displayBuilder.spawnItem(c.clone().add(p[0] + 0.25, p[1], p[2]), new ItemStack(Material.DARK_OAK_LOG));
                ax.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                axles.add(ax); spawnedEntities.add(ax.entity());
            }
            for (int i = 0; i < DRIVE_LINKS; i++) {
                double t = i / (double)(DRIVE_LINKS - 1);
                double zPos = -1.6 + t * 3.2;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(WALL_X + 0.15, 2.8, zPos), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                driveLinks.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);

            if (housing != null) {
                float s = 0.9f * fade;
                housing.animateTo(
                        new Vector3f((float) WALL_X - s / 2, 3.5f - s / 2, 0f - s / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            for (int i = 0; i < gears.size(); i++) {
                float rotSpeed = (i == 0) ? 0.05f : -0.07f;
                float s = 1.1f * fade;
                double z = (i == 0) ? -1.6 : 1.6;
                gears.get(i).animateTo(
                        new Vector3f((float) WALL_X - s / 2, 3.5f - s / 2, (float) z - s / 2),
                        new AxisAngle4f(tick * rotSpeed, 1, 0, 0),
                        new Vector3f(s, s, s), 4);
            }
            for (int i = 0; i < axles.size(); i++) {
                float sxz = 0.45f * fade;
                float sy = 0.6f * fade;
                double z = (i == 0) ? -1.6 : 1.6;
                axles.get(i).animateTo(
                        new Vector3f((float)(WALL_X + 0.25) - sxz / 2, 3.5f - sy / 2, (float) z - sxz / 2),
                        new AxisAngle4f((float)(Math.PI / 2), 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 4);
            }
            float dXZ = 0.42f * fade;
            float dY = 1.0f * fade;
            for (int i = 0; i < driveLinks.size(); i++) {
                double t = i / (double)(DRIVE_LINKS - 1);
                double zOff = Math.sin(tick * 0.05 + i) * 0.06;
                double zPos = -1.6 + t * 3.2 + zOff;
                driveLinks.get(i).animateTo(
                        new Vector3f((float)(WALL_X + 0.15) - dXZ / 2, 2.8f - dY / 2, (float) zPos - dXZ / 2),
                        new AxisAngle4f((float)(tick * 0.06), 0, 1, 0),
                        new Vector3f(dXZ, dY, dXZ), 4);
            }

            Location wc = c.clone().add(WALL_X + 0.5, 3.5, 0);
            ambientParticles(w, wc, tick, IRON);
            if (tick % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(WALL_X + 0.4, 3.5, (tick % 8 < 4 ? -1.6 : 1.6)), 4, 0.3, 0.3, 0.3, 0.04);
            if (tick % 80 == 40) DisplayBuilder.playSound(wc, Sound.BLOCK_GRINDSTONE_USE, 0.18f, 0.55f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronMachineryAmbient(plugin); }
    }

    // ================================================================
    // 55. CHAIN TETHER VISUAL — "The Connection" — AMBIENT (no damage)
    //     4 heavy CHAIN tethers (N/S/E/W) from wall anchors up to a single
    //     central NETHERITE_BLOCK ceiling boss, 8 thick links each with sag
    //     oscillation. The boss slowly rotates; rust drips down each tether.
    //     Design type: Structural ambience — chain tether visual (no damage, decor)
    // ================================================================
    public static class ChainTetherVisual extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tetherLinks = new ArrayList<>();
        private final List<ItemDisplayHandle> wallAnchors = new ArrayList<>();
        private final List<double[]> tetherInfo = new ArrayList<>(); // {x, y, z, sagFactor}
        private ItemDisplayHandle ceilingBoss;
        private static final int LINKS_PER = 8;
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 45;
        private static final double[][] WALL_POS = {
                {0, 3.5,  7.5}, {0, 3.5, -7.5}, { 7.5, 3.5, 0}, {-7.5, 3.5, 0}
        };
        private static final double[] CEILING = {0, 8.5, 0};

        public ChainTetherVisual(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tether_visual", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 170);
            config.setDesignType("Structural ambience — chain tether visual (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f);
            ceilingBoss = displayBuilder.spawnItem(c.clone().add(CEILING[0], CEILING[1], CEILING[2]), new ItemStack(Material.NETHERITE_BLOCK));
            ceilingBoss.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(ceilingBoss.entity());
            for (double[] wall : WALL_POS) {
                ItemDisplayHandle anc = displayBuilder.spawnItem(c.clone().add(wall[0], wall[1], wall[2]), new ItemStack(Material.IRON_BLOCK));
                anc.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                wallAnchors.add(anc); spawnedEntities.add(anc.entity());
                for (int i = 0; i < LINKS_PER; i++) {
                    double t = i / (double)(LINKS_PER - 1);
                    double x = wall[0] + (CEILING[0] - wall[0]) * t;
                    double y = wall[1] + (CEILING[1] - wall[1]) * t;
                    double z = wall[2] + (CEILING[2] - wall[2]) * t;
                    double sagFactor = Math.sin(t * Math.PI);
                    y -= sagFactor * 0.8;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    tetherLinks.add(h); spawnedEntities.add(h.entity());
                    tetherInfo.add(new double[]{x, y, z, sagFactor});
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            double oscill = Math.sin(tick * (Math.PI * 2 / 90.0)) * 0.06;

            if (ceilingBoss != null) {
                float s = 1.0f * fade;
                ceilingBoss.animateTo(
                        new Vector3f((float) CEILING[0] - s / 2, (float) CEILING[1] - s / 2, (float) CEILING[2] - s / 2),
                        new AxisAngle4f((float)(tick * 0.01), 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            for (int i = 0; i < wallAnchors.size(); i++) {
                double[] wall = WALL_POS[i];
                float s = 0.55f * fade;
                wallAnchors.get(i).animateTo(
                        new Vector3f((float) wall[0] - s / 2, (float) wall[1] - s / 2, (float) wall[2] - s / 2),
                        new AxisAngle4f((float)(tick * 0.004), 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            float sxz = 0.42f * fade;
            float sy = 1.2f * fade;
            for (int i = 0; i < tetherLinks.size(); i++) {
                double[] info = tetherInfo.get(i);
                double dy = oscill * info[3];
                tetherLinks.get(i).animateTo(
                        new Vector3f((float) info[0] - sxz / 2, (float)(info[1] + dy) - sy / 2, (float) info[2] - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.005 + i * 0.2), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            Location anchor = c.clone().add(CEILING[0], CEILING[1], CEILING[2]);
            ambientParticles(w, anchor, tick, DARK_IRON);
            for (double[] wall : WALL_POS) {
                double mx = (wall[0] + CEILING[0]) / 2.0;
                double my = (wall[1] + CEILING[1]) / 2.0 - 0.8;
                double mz = (wall[2] + CEILING[2]) / 2.0;
                if (tick % 4 == 0) rustDrip(w, c.clone().add(mx, my, mz), tick);
            }
            if (tick % 180 == 70) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.45f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainTetherVisual(plugin); }
    }

    // ================================================================
    // 56. IRON SCAFFOLDING WALL — "The Construction" — AMBIENT (no damage)
    //     A full scaffold against one wall: 3 tall DARK_OAK_LOG posts + 5
    //     DARK_OAK_LOG cross-platforms + 6 thick CHAIN railings + 2 ANVIL
    //     toolboxes on platforms. Smoke + sparks from base; rails sway.
    //     Design type: Structural ambience — iron scaffolding wall (no damage, decor)
    // ================================================================
    public static class IronScaffoldingWall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> verticals = new ArrayList<>();
        private final List<ItemDisplayHandle> horizontals = new ArrayList<>();
        private final List<ItemDisplayHandle> rails = new ArrayList<>();
        private final List<ItemDisplayHandle> boxes = new ArrayList<>();
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 50;
        private static final double WALL_Z = -7.6;

        public IronScaffoldingWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_scaffolding_wall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — iron scaffolding wall (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
            for (int i = 0; i < 3; i++) {
                double x = (i - 1) * 2.0;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 2.0, WALL_Z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                verticals.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 5; i++) {
                double y = 0.8 + i * 0.9;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(0, y, WALL_Z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                horizontals.add(h); spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double y = 0.6 + (i / 3) * 1.8;
                double x = -2.0 + (i % 3) * 2.0;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, WALL_Z + 0.3), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                rails.add(h); spawnedEntities.add(h.entity());
            }
            double[][] boxPos = {{-1.6, 1.7}, {1.6, 3.5}};
            for (double[] b : boxPos) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(b[0], b[1], WALL_Z + 0.2), new ItemStack(Material.ANVIL));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                boxes.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);

            for (int i = 0; i < verticals.size(); i++) {
                double x = (i - 1) * 2.0;
                float sx = 0.42f * fade;
                float sy = 4.0f * fade;
                float sz = 0.42f * fade;
                verticals.get(i).animateTo(
                        new Vector3f((float) x - sx / 2, 2.0f - sy / 2, (float) WALL_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < horizontals.size(); i++) {
                double y = 0.8 + i * 0.9;
                float sx = 4.2f * fade;
                float sy = 0.4f * fade;
                float sz = 0.4f * fade;
                horizontals.get(i).animateTo(
                        new Vector3f(0f - sx / 2, (float) y - sy / 2, (float) WALL_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < rails.size(); i++) {
                double y = 0.6 + (i / 3) * 1.8;
                double x = -2.0 + (i % 3) * 2.0;
                float sxz = 0.42f * fade;
                float sy = 0.6f * fade;
                rails.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, (float) y - sy / 2, (float)(WALL_Z + 0.3) - sxz / 2),
                        new AxisAngle4f((float)(Math.toRadians(2.0) * Math.sin(tick * 0.04 + i)), 1, 0, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            double[][] boxPos = {{-1.6, 1.7}, {1.6, 3.5}};
            for (int i = 0; i < boxes.size(); i++) {
                float s = 0.55f * fade;
                boxes.get(i).animateTo(
                        new Vector3f((float) boxPos[i][0] - s / 2, (float) boxPos[i][1] - s / 2, (float)(WALL_Z + 0.2) - s / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }

            Location base = c.clone().add(0, 0.3, WALL_Z + 0.5);
            ambientParticles(w, base, tick, RUST);
            if (tick % 4 == 0) w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, base, 3, 1.5, 0.1, 0.3, 0.01);
            if (tick % 220 == 90) DisplayBuilder.playSound(c.clone().add(0, 1.5, WALL_Z), Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronScaffoldingWall(plugin); }
    }

    // ================================================================
    // 57. ANCHOR CHAIN FLOOR RING — "The Mooring" — AMBIENT (no damage)
    //     A big mooring ring: 12 thick CHAIN links in a circle r=1.6 + a central
    //     ANVIL bollard + an IRON_BLOCK mooring bolt. Ring rotates slowly; rust
    //     drips from links; sparks pop at the bollard.
    //     Design type: Structural ambience — anchor chain floor ring (no damage, decor)
    // ================================================================
    public static class AnchorChainFloorRing extends EnvironmentalAttack {
        private static final int RING_LINKS = 12;
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private ItemDisplayHandle bollard;
        private ItemDisplayHandle bolt;
        private static final int SPAWN_LEN = 26;
        private static final int FADE_LEN = 40;
        private static final double RING_R = 1.6;
        private static final double RING_X = -4.5;
        private static final double RING_Z = 4.5;

        public AnchorChainFloorRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_chain_floor_ring", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 165);
            config.setDesignType("Structural ambience — anchor chain floor ring (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.4f);
            for (int i = 0; i < RING_LINKS; i++) {
                double a = Math.PI * 2 * i / RING_LINKS;
                double x = RING_X + Math.cos(a) * RING_R;
                double z = RING_Z + Math.sin(a) * RING_R;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.1, z), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                ring.add(h); spawnedEntities.add(h.entity());
            }
            bollard = displayBuilder.spawnItem(c.clone().add(RING_X, 0.1, RING_Z), new ItemStack(Material.ANVIL));
            bollard.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
            spawnedEntities.add(bollard.entity());
            bolt = displayBuilder.spawnItem(c.clone().add(RING_X + RING_R + 0.3, 0.1, RING_Z), new ItemStack(Material.IRON_BLOCK));
            bolt.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
            spawnedEntities.add(bolt.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            double rot = Math.toRadians(0.6) * tick;
            float sxz = 0.42f * fade;
            float sy = 0.6f * fade;
            for (int i = 0; i < ring.size(); i++) {
                double a = Math.PI * 2 * i / RING_LINKS + rot;
                double x = RING_X + Math.cos(a) * RING_R;
                double z = RING_Z + Math.sin(a) * RING_R;
                ring.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, 0.1f, (float) z - sxz / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (bollard != null) {
                float s = 0.7f * fade;
                bollard.animateTo(
                        new Vector3f((float) RING_X - s / 2, 0.1f, (float) RING_Z - s / 2),
                        new AxisAngle4f((float)(tick * 0.01), 0, 1, 0),
                        new Vector3f(s, s, s), 8);
            }
            if (bolt != null) {
                float s = 0.45f * fade;
                bolt.animateTo(
                        new Vector3f((float)(RING_X + RING_R + 0.3) - s / 2, 0.1f, (float) RING_Z - s / 2),
                        new AxisAngle4f((float)(tick * 0.005), 0, 1, 0),
                        new Vector3f(s, s, s), 8);
            }
            Location rc = c.clone().add(RING_X, 0.2, RING_Z);
            ambientParticles(w, rc, tick, IRON);
            if (tick % 4 == 0) w.spawnParticle(Particle.ELECTRIC_SPARK, rc, 4, 0.5, 0.2, 0.5, 0.02);
            if (tick % 6 == 0) {
                for (int i = 0; i < RING_LINKS; i += 3) {
                    double a = Math.PI * 2 * i / RING_LINKS + rot;
                    rustDrip(w, c.clone().add(RING_X + Math.cos(a) * RING_R, 0.2, RING_Z + Math.sin(a) * RING_R), tick);
                }
            }
            if (tick % 260 == 100) DisplayBuilder.playSound(rc, Sound.BLOCK_CHAIN_FALL, 0.25f, 0.45f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnchorChainFloorRing(plugin); }
    }

    // ================================================================
    // 58. CHAIN WALL SECTION — "The Barrier" — AMBIENT (no damage)
    //     A full chain-curtain wall: 6 columns x 5 rows of thick CHAIN links +
    //     2 DARK_OAK_LOG side posts + a DARK_OAK_LOG top beam. Each column sways
    //     independently. Heavy debris + sparks shed from the curtain.
    //     Design type: Structural ambience — chain wall section (no damage, decor)
    // ================================================================
    public static class ChainWallSection extends EnvironmentalAttack {
        private static final int COLS = 6;
        private static final int ROWS = 5;
        private final List<ItemDisplayHandle> wallLinks = new ArrayList<>();
        private final List<ItemDisplayHandle> posts = new ArrayList<>();
        private ItemDisplayHandle beam;
        private final int[] colIndex = new int[COLS * ROWS];
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 50;
        private static final double WALL_X = 7.6;
        private static final double COL_STEP = 0.5;
        private static final double ROW_STEP = 0.5;

        public ChainWallSection(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_wall_section", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 175);
            config.setDesignType("Structural ambience — chain wall section (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.45f);
            int idx = 0;
            for (int col = 0; col < COLS; col++) {
                for (int row = 0; row < ROWS; row++) {
                    double z = -(COLS - 1) * COL_STEP / 2 + col * COL_STEP;
                    double y = 0.6 + row * ROW_STEP;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(WALL_X, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                    wallLinks.add(h); spawnedEntities.add(h.entity());
                    colIndex[idx++] = col;
                }
            }
            // 2 side posts + top beam
            double zSpan = (COLS - 1) * COL_STEP / 2 + 0.4;
            for (double z : new double[]{-zSpan, zSpan}) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(WALL_X, 1.8, z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                posts.add(h); spawnedEntities.add(h.entity());
            }
            beam = displayBuilder.spawnItem(c.clone().add(WALL_X, 0.6 + ROWS * ROW_STEP, 0), new ItemStack(Material.DARK_OAK_LOG));
            beam.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
            spawnedEntities.add(beam.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);

            float sxz = 0.42f * fade;
            float sy = 0.6f * fade;
            for (int i = 0; i < wallLinks.size(); i++) {
                int col = colIndex[i];
                int row = i % ROWS;
                double z = -(COLS - 1) * COL_STEP / 2 + col * COL_STEP;
                double y = 0.6 + row * ROW_STEP;
                float swayAngle = (float) Math.toRadians(4.0 * Math.sin((tick - col * 20) * 0.05));
                wallLinks.get(i).animateTo(
                        new Vector3f((float) WALL_X - sxz / 2, (float) y - sy / 2, (float) z - sxz / 2),
                        new AxisAngle4f(swayAngle, 1, 0, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }
            double zSpan = (COLS - 1) * COL_STEP / 2 + 0.4;
            double[] postZ = {-zSpan, zSpan};
            for (int i = 0; i < posts.size(); i++) {
                float px = 0.42f * fade;
                float py = 3.4f * fade;
                posts.get(i).animateTo(
                        new Vector3f((float) WALL_X - px / 2, 1.8f - py / 2, (float) postZ[i] - px / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(px, py, px), 6);
            }
            if (beam != null) {
                float bx = 0.45f * fade;
                float bz = (float)((COLS - 1) * COL_STEP + 0.9) * fade;
                beam.animateTo(
                        new Vector3f((float) WALL_X - bx / 2, (float)(0.6 + ROWS * ROW_STEP) - bx / 2, 0f - bz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(bx, bx, bz), 6);
            }

            Location wc = c.clone().add(WALL_X - 0.3, 1.6, 0);
            ambientParticles(w, wc, tick, IRON);
            if (tick % 4 == 0) w.spawnParticle(Particle.FALLING_DUST, wc, 3, 0.1, 1.0, 1.5, 0.0, Material.CHAIN.createBlockData());
            if (tick % 200 == 80) DisplayBuilder.playSound(wc, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainWallSection(plugin); }
    }

    // ================================================================
    // 59. IRON SPIKE FLOOR STRIP — "The Edge" — AMBIENT (no damage)
    //     A long barricade skirt along a wall base: 10 big IRON_BLOCK spikes
    //     leaning outward + a DARK_OAK_LOG base rail + CHAIN links draped
    //     between spikes. Spikes shimmer; sparks arc along the tips.
    //     Design type: Structural ambience — iron spike floor strip (no damage, decor)
    // ================================================================
    public static class IronSpikeFloorStrip extends EnvironmentalAttack {
        private static final int SPIKES = 10;
        private final List<ItemDisplayHandle> spikes = new ArrayList<>();
        private final List<ItemDisplayHandle> drapes = new ArrayList<>();
        private final List<ItemDisplayHandle> rail = new ArrayList<>();
        private static final int SPAWN_LEN = 26;
        private static final int FADE_LEN = 40;
        private static final double STRIP_Z = -7.4;
        private static final double SPACING = 1.0;

        public IronSpikeFloorStrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_spike_floor_strip", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 160);
            config.setDesignType("Structural ambience — iron spike floor strip (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.6f);
            double startX = -(SPIKES - 1) * SPACING / 2;
            for (int i = 0; i < SPIKES; i++) {
                double x = startX + i * SPACING;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.4, STRIP_Z), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
                spikes.add(h); spawnedEntities.add(h.entity());
            }
            // 2-segment base rail
            for (int i = 0; i < 2; i++) {
                double x = startX + (SPIKES - 1) * SPACING * (i == 0 ? 0.25 : 0.75);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.15, STRIP_Z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                rail.add(h); spawnedEntities.add(h.entity());
            }
            // CHAIN drape between spikes
            for (int i = 0; i < SPIKES - 1; i++) {
                double x = startX + (i + 0.5) * SPACING;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.35, STRIP_Z + 0.1), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(DARK_IRON[0], DARK_IRON[1], DARK_IRON[2]).interpolation(2, 0);
                drapes.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            double startX = -(SPIKES - 1) * SPACING / 2;

            for (int i = 0; i < spikes.size(); i++) {
                double x = startX + i * SPACING;
                float sxz = 0.42f * fade;
                float sy = 1.2f * fade;
                float lean = (float) Math.toRadians(12.0 + Math.sin(tick * 0.04 + i) * 2.0);
                spikes.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, 0.4f, (float) STRIP_Z - sxz / 2),
                        new AxisAngle4f(lean, 1, 0, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }
            for (int i = 0; i < rail.size(); i++) {
                double x = startX + (SPIKES - 1) * SPACING * (i == 0 ? 0.25 : 0.75);
                float sx = (float)(SPIKES * SPACING / 2) * fade;
                float sy = 0.35f * fade;
                float sz = 0.35f * fade;
                rail.get(i).animateTo(
                        new Vector3f((float) x - sx / 2, 0.15f - sy / 2, (float) STRIP_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < drapes.size(); i++) {
                double x = startX + (i + 0.5) * SPACING;
                double bob = Math.sin(tick * 0.05 + i) * 0.05;
                float sxz = 0.42f * fade;
                float sy = 0.45f * fade;
                drapes.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, (float)(0.3 + bob), (float)(STRIP_Z + 0.1) - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            Location strip = c.clone().add(0, 0.6, STRIP_Z);
            ambientParticles(w, strip, tick, IRON);
            if (tick % 3 == 0) {
                int i = (tick / 3) % SPIKES;
                Location tip = c.clone().add(startX + i * SPACING, 1.0, STRIP_Z);
                w.spawnParticle(Particle.ELECTRIC_SPARK, tip, 4, 0.1, 0.1, 0.1, 0.04);
                DisplayBuilder.dustParticles(tip, 3, 0.15, IRON[0], IRON[1], IRON[2], 1.1f);
            }
            if (tick % 240 == 110) DisplayBuilder.playSound(strip, Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.55f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronSpikeFloorStrip(plugin); }
    }

    // ================================================================
    // 60. CHAIN LOCK MECHANISM — "The Seal" — AMBIENT (no damage)
    //     A massive padlock sealing a doorway: big NETHERITE_BLOCK lock body,
    //     thick CHAIN shackle arc (2 links), IRON_BLOCK keyhole boss, and 2
    //     heavy CHAIN binding strands draped across the door. Pulses with
    //     AMETHYST sparks; the keyhole rotates as if being picked.
    //     Design type: Structural ambience — chain lock mechanism (no damage, decor)
    // ================================================================
    public static class ChainLockMechanism extends EnvironmentalAttack {
        private ItemDisplayHandle body;
        private ItemDisplayHandle shackleLeft;
        private ItemDisplayHandle shackleRight;
        private ItemDisplayHandle keyhole;
        private final List<ItemDisplayHandle> bindings = new ArrayList<>();
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 50;
        private static final double DOOR_X = 0;
        private static final double DOOR_Y = 2.8;
        private static final double DOOR_Z = 7.5;

        public ChainLockMechanism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_lock_mechanism", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — chain lock mechanism (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.8f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.45f);
            body = displayBuilder.spawnItem(c.clone().add(DOOR_X, DOOR_Y, DOOR_Z), new ItemStack(Material.NETHERITE_BLOCK));
            body.scale(0.001f, 0.001f, 0.001f).glow(AMETHYST[0], AMETHYST[1], AMETHYST[2]).interpolation(2, 0);
            spawnedEntities.add(body.entity());
            shackleLeft = displayBuilder.spawnItem(c.clone().add(DOOR_X - 0.5, DOOR_Y + 0.8, DOOR_Z - 0.1), new ItemStack(Material.CHAIN));
            shackleLeft.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
            spawnedEntities.add(shackleLeft.entity());
            shackleRight = displayBuilder.spawnItem(c.clone().add(DOOR_X + 0.5, DOOR_Y + 0.8, DOOR_Z - 0.1), new ItemStack(Material.CHAIN));
            shackleRight.scale(0.001f, 0.001f, 0.001f).glow(IRON[0], IRON[1], IRON[2]).interpolation(2, 0);
            spawnedEntities.add(shackleRight.entity());
            keyhole = displayBuilder.spawnItem(c.clone().add(DOOR_X, DOOR_Y, DOOR_Z - 0.45), new ItemStack(Material.IRON_BLOCK));
            keyhole.scale(0.001f, 0.001f, 0.001f).glow(LANTERN_GLOW[0], LANTERN_GLOW[1], LANTERN_GLOW[2]).interpolation(2, 0);
            spawnedEntities.add(keyhole.entity());
            // 2 binding strands draped diagonally across the door
            for (int i = 0; i < 2; i++) {
                double yOff = (i == 0 ? -1.2 : 1.2);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(DOOR_X, DOOR_Y + yOff, DOOR_Z - 0.05), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(RUST[0], RUST[1], RUST[2]).interpolation(2, 0);
                bindings.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float fade = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN);
            float pulse = (float)(1.0 + Math.sin(tick * 0.04) * 0.03);

            if (body != null) {
                float sx = 1.1f * fade * pulse;
                float sy = 1.1f * fade * pulse;
                float sz = 0.7f * fade * pulse;
                body.animateTo(
                        new Vector3f((float) DOOR_X - sx / 2, (float) DOOR_Y - sy / 2, (float) DOOR_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 8);
            }
            if (shackleLeft != null) {
                float sxz = 0.42f * fade;
                float sy = 1.0f * fade;
                shackleLeft.animateTo(
                        new Vector3f((float)(DOOR_X - 0.5) - sxz / 2, (float)(DOOR_Y + 0.8) - sy / 2, (float)(DOOR_Z - 0.1) - sxz / 2),
                        new AxisAngle4f((float) Math.toRadians(-25.0), 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (shackleRight != null) {
                float sxz = 0.42f * fade;
                float sy = 1.0f * fade;
                shackleRight.animateTo(
                        new Vector3f((float)(DOOR_X + 0.5) - sxz / 2, (float)(DOOR_Y + 0.8) - sy / 2, (float)(DOOR_Z - 0.1) - sxz / 2),
                        new AxisAngle4f((float) Math.toRadians(25.0), 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (keyhole != null) {
                float s = 0.42f * fade;
                keyhole.animateTo(
                        new Vector3f((float) DOOR_X - s / 2, (float) DOOR_Y - s / 2, (float)(DOOR_Z - 0.45) - s / 2),
                        new AxisAngle4f((float)(tick * 0.04), 0, 0, 1),
                        new Vector3f(s, s, s * 0.6f), 6);
            }
            for (int i = 0; i < bindings.size(); i++) {
                double yOff = (i == 0 ? -1.2 : 1.2);
                float sxz = 0.42f * fade;
                float sz = 2.6f * fade;
                // Drape horizontally across the door (long along Z plane via wide X)
                float swayAngle = (float) Math.toRadians(8.0 * Math.sin(tick * 0.05 + i));
                bindings.get(i).animateTo(
                        new Vector3f((float) DOOR_X - sz / 2, (float)(DOOR_Y + yOff) - sxz / 2, (float)(DOOR_Z - 0.05) - sxz / 2),
                        new AxisAngle4f(swayAngle, 0, 0, 1),
                        new Vector3f(sz, sxz, sxz), 8);
            }

            Location lc = c.clone().add(DOOR_X, DOOR_Y, DOOR_Z - 0.3);
            ambientParticles(w, lc, tick, AMETHYST);
            if (tick % 30 == 0) {
                w.spawnParticle(Particle.ELECTRIC_SPARK, lc, 8, 0.6, 0.6, 0.2, 0.03);
                w.spawnParticle(Particle.WITCH, lc, 4, 0.4, 0.4, 0.2, 0.01);
                DisplayBuilder.dustParticles(lc, 5, 0.5, AMETHYST[0], AMETHYST[1], AMETHYST[2], 1.3f);
            }
            if (tick % 240 == 60) DisplayBuilder.playSound(lc, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.4f);
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainLockMechanism(plugin); }
    }
}
