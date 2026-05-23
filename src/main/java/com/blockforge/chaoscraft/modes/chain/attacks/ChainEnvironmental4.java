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
 * Chain Mode — ENVIRONMENTAL ATTACKS 46-60 (Structural Ambience)
 *
 * Pure architectural ambience pieces that "live" in the arena: chain pillar wraps,
 * iron arches, barred windows, support cables, floor grates, forge vents, ceiling
 * nets, dent marks, machinery, tether visuals, scaffolding walls, anchor rings,
 * chain wall curtains, iron spike skirting, lock mechanisms.
 *
 * Critical contract for every attack here:
 *   - ItemDisplays only (NEVER BlockDisplays).
 *   - ALL damage = 0 / radius = 0. Pure decor.
 *   - Long durations (600-1500t) so the structure persists as ambience.
 *   - Three-phase choreography: spawn(build/lay) -> idle hold -> fade.
 *   - Subtle ambient animation only (slow rotation, gentle sway, slow pulse).
 *   - 3+ layered particle types per active tick on average.
 *   - Always straight (yaw=0, pitch=0).
 *   - Distinct from every other ChaosCraft mode in concept and shape.
 *
 * Design type prefix: "Structural ambience — &lt;name&gt; (no damage, decor)".
 */
public final class ChainEnvironmental4 {
    private ChainEnvironmental4() {}

    private static final String MODE_PATH = "modes/chain/attacks";

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
        c.setChance(10);
        c.setDurationTicks(durationTicks);
        c.setCooldownTicks(cooldownTicks);
        c.setEnabled(true);
    }

    /** Compute fade scale in 0..1 for spawn-in / hold / fade-out windows. */
    private static float lifecycleScale(int tick, int duration, int spawnLen, int fadeLen, float maxScale) {
        if (tick < spawnLen) {
            return maxScale * (tick / (float) spawnLen);
        }
        int fadeStart = duration - fadeLen;
        if (tick >= fadeStart) {
            float t = 1f - ((tick - fadeStart) / (float) Math.max(1, fadeLen));
            return maxScale * Math.max(0f, t);
        }
        return maxScale;
    }

    // ================================================================
    // 46. CHAIN PILLAR WRAP — "The Column"
    //     4 arena pillars with helical chain wraps slowly rotating around them.
    //     Spawn: links materialise progressively up each pillar.
    //     Hold:  helix angle advances ~2 deg/tick (slow rotation), gentle dust glow.
    //     Fade:  scale collapses, smoke wisp drifts off.
    // ================================================================
    public static class ChainPillarWrap extends EnvironmentalAttack {
        private static final int PILLARS = 4;
        private static final int LINKS_PER = 8;
        private static final double PILLAR_R = 6.5;       // arena radius for pillar placement
        private static final double WRAP_RADIUS = 0.55;
        private static final double HELIX_HEIGHT = 3.0;
        private final List<ItemDisplayHandle> links = new ArrayList<>();
        private final double[] pillarX = new double[PILLARS];
        private final double[] pillarZ = new double[PILLARS];
        private static final int SPAWN_LEN = 30;
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
                for (int i = 0; i < LINKS_PER; i++) {
                    double t = i / (double) (LINKS_PER - 1);
                    double y = t * HELIX_HEIGHT;
                    double helixA = t * Math.PI * 2 * 1.5;
                    double x = pillarX[p] + Math.cos(helixA) * WRAP_RADIUS;
                    double z = pillarZ[p] + Math.sin(helixA) * WRAP_RADIUS;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(110, 110, 130).interpolation(SPAWN_LEN, 0);
                    links.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 0.22f);
            float pulse = (float)(1.0 + Math.sin(tick * 0.06) * 0.04);
            float ySc = baseScale * 0.55f * pulse;
            float xSc = baseScale * pulse;

            for (int p = 0; p < PILLARS; p++) {
                for (int i = 0; i < LINKS_PER; i++) {
                    int idx = p * LINKS_PER + i;
                    if (idx >= links.size()) break;
                    double t = i / (double) (LINKS_PER - 1);
                    double y = t * HELIX_HEIGHT;
                    double helixA = t * Math.PI * 2 * 1.5 + Math.toRadians(2.0) * tick; // 2 deg/tick advance
                    double x = pillarX[p] + Math.cos(helixA) * WRAP_RADIUS;
                    double z = pillarZ[p] + Math.sin(helixA) * WRAP_RADIUS;
                    links.get(idx).animateTo(
                            new Vector3f((float) x - xSc / 2, (float) y, (float) z - xSc / 2),
                            new AxisAngle4f((float) helixA, 0, 1, 0),
                            new Vector3f(xSc, ySc, xSc), 4);
                }
                if (tick % 30 == 0) {
                    Location pcenter = c.clone().add(pillarX[p], HELIX_HEIGHT / 2, pillarZ[p]);
                    DisplayBuilder.dustParticles(pcenter, 2, 0.6, 90, 90, 110, 1.0f);
                    w.spawnParticle(Particle.SMOKE, pcenter, 1, 0.4, 0.6, 0.4, 0.0);
                    w.spawnParticle(Particle.FALLING_DUST, pcenter, 1, 0.3, 0.3, 0.3, 0.0, Material.IRON_BLOCK.createBlockData());
                }
            }
            if (tick % 120 == 60) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ChainPillarWrap(plugin); }
    }

    // ================================================================
    // 47. IRON ARCH SPAN — "The Gate"
    //     A half-circle arch of 5 IRON_BLOCK + 4 NETHERITE_BLOCK corners
    //     + 2 hanging CHAIN keystone links. Static structure with sparse
    //     spark glow on the keystone.
    // ================================================================
    public static class IronArchSpan extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> arch = new ArrayList<>();
        private final List<ItemDisplayHandle> corners = new ArrayList<>();
        private final List<ItemDisplayHandle> keyChains = new ArrayList<>();
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 50;
        private static final double ARCH_R = 4.5;
        private static final double ARCH_X = 7.5;

        public IronArchSpan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_arch_span", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — iron arch span (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.5f, 0.4f);
            // 5 arch stones across upper half-circle
            for (int i = 0; i < 5; i++) {
                double a = Math.PI * (i + 0.5) / 5.0; // 0..PI, evenly stepped
                double y = Math.sin(a) * ARCH_R + 0.5;
                double xRel = Math.cos(a) * ARCH_R;
                Location loc = c.clone().add(ARCH_X, y, xRel);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(SPAWN_LEN, i * 2);
                arch.add(h); spawnedEntities.add(h.entity());
            }
            // 4 NETHERITE_BLOCK corner accents
            double[][] cornerOffsets = {
                    {ARCH_X - 0.2, 0.5, -ARCH_R - 0.2},
                    {ARCH_X - 0.2, 0.5,  ARCH_R + 0.2},
                    {ARCH_X + 0.2, 0.5, -ARCH_R - 0.2},
                    {ARCH_X + 0.2, 0.5,  ARCH_R + 0.2}
            };
            for (double[] o : cornerOffsets) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(o[0], o[1], o[2]), new ItemStack(Material.NETHERITE_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(40, 30, 50).interpolation(SPAWN_LEN, 0);
                corners.add(h); spawnedEntities.add(h.entity());
            }
            // 2 hanging keystone chains
            for (int i = 0; i < 2; i++) {
                double zOff = (i == 0) ? -0.25 : 0.25;
                Location loc = c.clone().add(ARCH_X, ARCH_R + 0.5 - 0.6, zOff);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(120, 120, 140).interpolation(SPAWN_LEN, 0);
                keyChains.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            for (int i = 0; i < arch.size(); i++) {
                double a = Math.PI * (i + 0.5) / 5.0;
                double y = Math.sin(a) * ARCH_R + 0.5;
                double xRel = Math.cos(a) * ARCH_R;
                float sx = 0.9f * baseScale;
                float sy = 0.9f * baseScale;
                float sz = 0.9f * baseScale;
                arch.get(i).animateTo(
                        new Vector3f((float)(ARCH_X) - sx / 2, (float) y, (float) xRel - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < corners.size(); i++) {
                float s = 0.45f * baseScale;
                corners.get(i).animateTo(
                        corners.get(i).entity().getTransformation().getTranslation(),
                        new AxisAngle4f(tick * 0.005f, 0, 1, 0),
                        new Vector3f(s, s, s), 6);
            }
            // Hanging chains — gentle sway
            float swaySway = (float)(Math.sin(tick * 0.04) * 0.07);
            for (int i = 0; i < keyChains.size(); i++) {
                float sx = 0.18f * baseScale;
                float sy = 0.55f * baseScale;
                double zOff = (i == 0) ? -0.25 : 0.25;
                keyChains.get(i).animateTo(
                        new Vector3f((float)(ARCH_X) - sx / 2, (float)(ARCH_R + 0.5 - 0.6 + swaySway), (float) zOff - sx / 2),
                        new AxisAngle4f(swaySway, 0, 0, 1),
                        new Vector3f(sx, sy, sx), 8);
            }

            if (tick % 25 == 0) {
                Location keystone = c.clone().add(ARCH_X, ARCH_R + 0.5, 0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, keystone, 1, 0.3, 0.1, 0.3, 0.01);
                DisplayBuilder.dustParticles(keystone, 1, 0.5, 200, 200, 220, 1.1f);
                w.spawnParticle(Particle.SMOKE, keystone, 1, 0.4, 0.2, 0.4, 0.0);
            }
            if (tick % 200 == 80) DisplayBuilder.playSound(c.clone().add(ARCH_X, 4, 0), Sound.BLOCK_ANVIL_PLACE, 0.25f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new IronArchSpan(plugin); }
    }

    // ================================================================
    // 48. BARRED WINDOW EFFECT — "The Cell"
    //     2 wall windows, each 4 vertical IRON_BLOCK bars + 1 horizontal,
    //     with SMOKE drifting inward through the bars.
    // ================================================================
    public static class BarredWindowEffect extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bars = new ArrayList<>();
        private static final int SPAWN_LEN = 35;
        private static final int FADE_LEN = 40;
        // Window centers: two adjacent walls, mid-height
        private static final double[][] WIN_CENTERS = {
                {-7.5, 2.5, 1.5}, // north wall
                { 7.5, 2.5,-1.5}  // south wall
        };

        public BarredWindowEffect(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("barred_window_effect", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1300, 150);
            config.setDesignType("Structural ambience — barred window (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.6f);
            for (double[] center : WIN_CENTERS) {
                // 4 vertical bars
                for (int i = 0; i < 4; i++) {
                    double zOff = (i - 1.5) * 0.18;
                    Location loc = c.clone().add(center[0], center[1], center[2] + zOff);
                    ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BLOCK));
                    h.scale(0.001f, 0.001f, 0.001f).glow(170, 170, 190).interpolation(SPAWN_LEN, 0);
                    bars.add(h); spawnedEntities.add(h.entity());
                }
                // 1 horizontal bar
                Location loc = c.clone().add(center[0], center[1], center[2]);
                ItemDisplayHandle h = displayBuilder.spawnItem(loc, new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(170, 170, 190).interpolation(SPAWN_LEN, 0);
                bars.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            int idx = 0;
            for (double[] center : WIN_CENTERS) {
                for (int i = 0; i < 4; i++) {
                    if (idx >= bars.size()) break;
                    double zOff = (i - 1.5) * 0.18;
                    float sx = 0.06f * baseScale;
                    float sy = 0.8f * baseScale;
                    float sz = 0.06f * baseScale;
                    bars.get(idx).animateTo(
                            new Vector3f((float)center[0] - sx / 2, (float)center[1] - sy / 2, (float)(center[2] + zOff) - sz / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sx, sy, sz), 6);
                    idx++;
                }
                if (idx < bars.size()) {
                    float sx = 0.06f * baseScale;
                    float sy = 0.06f * baseScale;
                    float sz = 0.72f * baseScale;
                    bars.get(idx).animateTo(
                            new Vector3f((float)center[0] - sx / 2, (float)center[1] - sy / 2, (float)center[2] - sz / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(sx, sy, sz), 6);
                    idx++;
                }
            }

            // SMOKE drift through bars (3-layer)
            if (tick % 6 == 0) {
                for (double[] center : WIN_CENTERS) {
                    Location wc = c.clone().add(center[0], center[1], center[2]);
                    double dirX = -Math.signum(center[0]);
                    w.spawnParticle(Particle.SMOKE, wc, 2, 0.05, 0.3, 0.4, 0.0);
                    w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, wc.clone().add(dirX * 0.2, 0, 0), 1, 0.1, 0.3, 0.3, 0.005);
                    DisplayBuilder.dustParticles(wc, 1, 0.4, 120, 120, 130, 0.8f);
                }
            }
            if (tick % 300 == 120) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.2f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new BarredWindowEffect(plugin); }
    }

    // ================================================================
    // 49. CHAIN SUPPORT CABLES — "The Infrastructure"
    //     4 diagonal heavy CHAIN cables from ceiling corners to wall
    //     attachment points. Each cable: 8 chain links along diagonal.
    //     Animation: slight sag oscillation on middle links.
    // ================================================================
    public static class ChainSupportCables extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cableLinks = new ArrayList<>();
        private final List<double[]> linkInfo = new ArrayList<>(); // {baseX, baseY, baseZ, sagFactor}
        private static final int LINKS_PER = 8;
        private static final int SPAWN_LEN = 35;
        private static final int FADE_LEN = 45;
        // Ceiling corners -> wall attachment points
        private static final double[][] STARTS = {
                {-7.0, 8.5, -7.0}, { 7.0, 8.5, -7.0},
                { 7.0, 8.5,  7.0}, {-7.0, 8.5,  7.0}
        };
        private static final double[][] ENDS = {
                {-6.0, 3.0,  3.0}, { 6.0, 3.0,  3.0},
                { 6.0, 3.0, -3.0}, {-6.0, 3.0, -3.0}
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
                    // Middle-link sag factor peaks at t=0.5
                    double sagFactor = Math.sin(t * Math.PI);
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(95, 95, 110).interpolation(SPAWN_LEN, i);
                    cableLinks.add(h); spawnedEntities.add(h.entity());
                    linkInfo.add(new double[]{x, y, z, sagFactor});
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            double sagOsc = Math.sin(tick * (Math.PI * 2 / 80.0)) * 0.02;

            for (int i = 0; i < cableLinks.size(); i++) {
                double[] info = linkInfo.get(i);
                double x = info[0];
                double y = info[1] - info[3] * 0.15 + sagOsc * info[3];
                double z = info[2];
                float sxz = 0.18f * baseScale;
                float sy = 0.5f * baseScale;
                cableLinks.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, (float) y, (float) z - sxz / 2),
                        new AxisAngle4f(0.6f + (float) Math.toRadians(tick * 0.5), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            if (tick % 30 == 0) {
                for (int cable = 0; cable < 4; cable++) {
                    double mx = (STARTS[cable][0] + ENDS[cable][0]) / 2.0;
                    double my = (STARTS[cable][1] + ENDS[cable][1]) / 2.0 - 0.2;
                    double mz = (STARTS[cable][2] + ENDS[cable][2]) / 2.0;
                    Location p = c.clone().add(mx, my, mz);
                    w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.2, 0.2, 0.2, 0.0, Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.dustParticles(p, 1, 0.3, 90, 90, 110, 0.9f);
                    if ((tick / 30 + cable) % 4 == 0) {
                        w.spawnParticle(Particle.SMOKE, p, 1, 0.2, 0.1, 0.2, 0.0);
                    }
                }
            }
            if (tick % 180 == 90) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.25f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ChainSupportCables(plugin); }
    }

    // ================================================================
    // 50. IRON FLOOR GRATING OVERLAY — "The Grid"
    //     3x3 area at one section of floor: 6 horizontal + 6 vertical
    //     IRON_BLOCK bars + 4 CHAIN corners. Static. ELECTRIC_SPARK
    //     burst every 20 ticks.
    // ================================================================
    public static class IronFloorGrating extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> bars = new ArrayList<>();
        private final List<ItemDisplayHandle> chainCorners = new ArrayList<>();
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 40;
        private static final double GRATE_CX = 6.0; // shifted off-center toward one side of arena
        private static final double GRATE_CZ = 0.0;
        private static final double GRATE_SIZE = 3.0;

        public IronFloorGrating(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_floor_grating", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 160);
            config.setDesignType("Structural ambience — iron floor grating (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.7f);
            // 6 horizontal (along X)
            for (int i = 0; i < 6; i++) {
                double zOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX, 0.01, GRATE_CZ + zOff), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(150, 150, 170).interpolation(SPAWN_LEN, 0);
                bars.add(h); spawnedEntities.add(h.entity());
            }
            // 6 vertical (along Z)
            for (int i = 0; i < 6; i++) {
                double xOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX + xOff, 0.01, GRATE_CZ), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(150, 150, 170).interpolation(SPAWN_LEN, 0);
                bars.add(h); spawnedEntities.add(h.entity());
            }
            // 4 corner chains
            double half = GRATE_SIZE / 2;
            double[][] corners = {{-half,-half},{half,-half},{half,half},{-half,half}};
            for (double[] o : corners) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(GRATE_CX + o[0], 0.04, GRATE_CZ + o[1]), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(120, 120, 140).interpolation(SPAWN_LEN, 0);
                chainCorners.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            // 6 horizontal
            for (int i = 0; i < 6; i++) {
                if (i >= bars.size()) break;
                double zOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                float sx = (float) GRATE_SIZE * baseScale;
                float sy = 0.02f * baseScale;
                float sz = 0.06f * baseScale;
                bars.get(i).animateTo(
                        new Vector3f((float) GRATE_CX - sx / 2, 0.01f, (float)(GRATE_CZ + zOff) - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            // 6 vertical
            for (int i = 0; i < 6; i++) {
                int idx = 6 + i;
                if (idx >= bars.size()) break;
                double xOff = -GRATE_SIZE / 2 + i * (GRATE_SIZE / 5);
                float sx = 0.06f * baseScale;
                float sy = 0.02f * baseScale;
                float sz = (float) GRATE_SIZE * baseScale;
                bars.get(idx).animateTo(
                        new Vector3f((float)(GRATE_CX + xOff) - sx / 2, 0.01f, (float) GRATE_CZ - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < chainCorners.size(); i++) {
                float s = 0.18f * baseScale;
                chainCorners.get(i).animateTo(
                        chainCorners.get(i).entity().getTransformation().getTranslation(),
                        new AxisAngle4f(tick * 0.01f, 0, 1, 0),
                        new Vector3f(s, 0.3f * baseScale, s), 8);
            }

            // ELECTRIC_SPARK every 20t + ambient layers
            if (tick % 20 == 0) {
                Location gc = c.clone().add(GRATE_CX, 0.05, GRATE_CZ);
                w.spawnParticle(Particle.ELECTRIC_SPARK, gc, 2, GRATE_SIZE / 2, 0.05, GRATE_SIZE / 2, 0.02);
                w.spawnParticle(Particle.SMOKE, gc, 1, GRATE_SIZE / 3, 0.1, GRATE_SIZE / 3, 0.0);
                DisplayBuilder.dustParticles(gc, 1, GRATE_SIZE / 3, 200, 130, 60, 1.0f);
            }
            if (tick % 240 == 80) DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.3f, 0.6f);
        }

        @Override public AbstractAttack newInstance() { return new IronFloorGrating(plugin); }
    }

    // ================================================================
    // 51. FORGE GLOW VENTS — "The Below"
    //     4 floor vent plates (HEAVY_WEIGHTED_PRESSURE_PLATE) with FLAME
    //     rising. Vents pulse Y-scale +/- 0.01 (heat vibration). Forge
    //     ambient sound.
    // ================================================================
    public static class ForgeGlowVents extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> vents = new ArrayList<>();
        private static final int SPAWN_LEN = 25;
        private static final int FADE_LEN = 35;
        private static final double[][] VENT_POS = {
                {-3.0, 0, -3.0}, { 3.0, 0, -3.0},
                {-3.0, 0,  3.0}, { 3.0, 0,  3.0}
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
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(p[0], 0.04, p[2]), new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
                h.scale(0.001f, 0.001f, 0.001f).glow(255, 140, 40).interpolation(SPAWN_LEN, 0);
                vents.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            float pulseY = (float)(1.0 + Math.sin(tick * 0.25) * 0.16); // visible "heat vibration"

            for (int i = 0; i < vents.size(); i++) {
                double[] p = VENT_POS[i];
                float sx = 0.6f * baseScale;
                float sy = 0.06f * baseScale * pulseY;
                float sz = 0.6f * baseScale;
                vents.get(i).animateTo(
                        new Vector3f((float) p[0] - sx / 2, 0.04f, (float) p[2] - sz / 2),
                        new AxisAngle4f((float)(tick * 0.005 + i), 0, 1, 0),
                        new Vector3f(sx, sy, sz), 4);
            }

            // Per-tick FLAME + layered SOUL_FIRE_FLAME + DUST glow
            for (double[] p : VENT_POS) {
                Location vc = c.clone().add(p[0], 0.1, p[2]);
                if (tick % 2 == 0) w.spawnParticle(Particle.FLAME, vc, 2, 0.15, 0.05, 0.15, 0.05);
                if (tick % 4 == 0) w.spawnParticle(Particle.SOUL_FIRE_FLAME, vc, 1, 0.1, 0.05, 0.1, 0.03);
                if (tick % 6 == 0) DisplayBuilder.dustParticles(vc, 1, 0.15, 255, 120, 30, 1.4f);
                if (tick % 12 == 0) w.spawnParticle(Particle.SMOKE, vc.clone().add(0, 0.7, 0), 1, 0.2, 0.2, 0.2, 0.01);
            }
            if (tick % 60 == 30) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.25f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ForgeGlowVents(plugin); }
    }

    // ================================================================
    // 52. CHAIN NET OVERHEAD — "The Ceiling Trap"
    //     A static chain net at Y=4.5: 5x5 grid of CHAIN links + 4 corner
    //     anchors. Slight sway. Particles drop from net.
    // ================================================================
    public static class ChainNetOverhead extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> netLinks = new ArrayList<>();
        private final double[][] linkBase = new double[25][2]; // {x, z}
        private static final int SPAWN_LEN = 35;
        private static final int FADE_LEN = 40;
        private static final double NET_Y = 4.5;
        private static final double NET_SIZE = 5.0;

        public ChainNetOverhead(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_net_overhead", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 165);
            config.setDesignType("Structural ambience — chain net overhead (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.45f);
            int idx = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 5; col++) {
                    double x = -NET_SIZE / 2 + col * (NET_SIZE / 4);
                    double z = -NET_SIZE / 2 + row * (NET_SIZE / 4);
                    linkBase[idx][0] = x;
                    linkBase[idx][1] = z;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, NET_Y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(100, 100, 120).interpolation(SPAWN_LEN, idx / 5);
                    netLinks.add(h); spawnedEntities.add(h.entity());
                    idx++;
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            // Corners oscillate +/-0.05 over 80t period
            double sway = Math.sin(tick * (Math.PI * 2 / 80.0)) * 0.05;

            for (int i = 0; i < netLinks.size(); i++) {
                double bx = linkBase[i][0];
                double bz = linkBase[i][1];
                double cornerWeight = (Math.abs(bx) + Math.abs(bz)) / (NET_SIZE);
                double dy = sway * cornerWeight;
                float sxz = 0.16f * baseScale;
                float sy = 0.5f * baseScale;
                netLinks.get(i).animateTo(
                        new Vector3f((float) bx - sxz / 2, (float)(NET_Y + dy), (float) bz - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.01 + i * 0.13), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }

            // Particles drop every 30 ticks (CHAIN dust + smoke + spark)
            if (tick % 30 == 0) {
                for (int k = 0; k < 2; k++) {
                    double rx = (Math.random() - 0.5) * NET_SIZE;
                    double rz = (Math.random() - 0.5) * NET_SIZE;
                    Location drop = c.clone().add(rx, NET_Y - 0.1, rz);
                    w.spawnParticle(Particle.FALLING_DUST, drop, 1, 0.1, 0.2, 0.1, 0.0, Material.CHAIN.createBlockData());
                    w.spawnParticle(Particle.SMOKE, drop, 1, 0.1, 0.1, 0.1, 0.0);
                    if ((tick / 30 + k) % 3 == 0) {
                        w.spawnParticle(Particle.ELECTRIC_SPARK, drop, 1, 0.05, 0.05, 0.05, 0.01);
                    }
                }
            }
            if (tick % 200 == 60) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ChainNetOverhead(plugin); }
    }

    // ================================================================
    // 53. WRECKING BALL DENT MARKS — "The History"
    //     6 flat GRAY_CONCRETE dent circles at ground level with smoke
    //     rising. Marks ground "history" of past impacts.
    // ================================================================
    public static class WreckingBallDentMarks extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> dents = new ArrayList<>();
        private final double[][] dentPos = new double[6][2];
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 60;

        public WreckingBallDentMarks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_dent_marks", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — wrecking ball dent marks (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.4f);
            for (int i = 0; i < 6; i++) {
                double a = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 4.0;
                dentPos[i][0] = Math.cos(a) * r;
                dentPos[i][1] = Math.sin(a) * r;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(dentPos[i][0], 0.02, dentPos[i][1]), new ItemStack(Material.GRAY_CONCRETE));
                h.scale(0.001f, 0.001f, 0.001f).glow(70, 70, 75).interpolation(SPAWN_LEN, i * 6);
                dents.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            for (int i = 0; i < dents.size(); i++) {
                float sxz = (float)(1.0 + Math.sin(tick * 0.02 + i) * 0.05) * baseScale;
                float sy = 0.01f * baseScale;
                dents.get(i).animateTo(
                        new Vector3f((float) dentPos[i][0] - sxz / 2, 0.02f, (float) dentPos[i][1] - sxz / 2),
                        new AxisAngle4f((float)(i * 0.3 + tick * 0.002), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 10);
                if (tick % 2 == 0) {
                    Location dl = c.clone().add(dentPos[i][0], 0.1, dentPos[i][1]);
                    w.spawnParticle(Particle.SMOKE, dl, 1, 0.2, 0.05, 0.2, 0.0);
                }
                if (tick % 18 == 0) {
                    Location dl = c.clone().add(dentPos[i][0], 0.05, dentPos[i][1]);
                    DisplayBuilder.dustParticles(dl, 1, 0.4, 80, 80, 85, 1.0f);
                    w.spawnParticle(Particle.FALLING_DUST, dl, 1, 0.2, 0.1, 0.2, 0.0, Material.GRAY_CONCRETE.createBlockData());
                }
            }
            if (tick % 260 == 100) DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.3f);
        }

        @Override public AbstractAttack newInstance() { return new WreckingBallDentMarks(plugin); }
    }

    // ================================================================
    // 54. IRON MACHINERY AMBIENT — "The Workshop"
    //     A visible mechanical assembly on one arena wall: 2 gears
    //     (HEAVY_WEIGHTED_PRESSURE_PLATE) rotating independently, 2 axles
    //     (DARK_OAK_LOG), 4 CHAIN drive links. Grindstone ambient sound.
    // ================================================================
    public static class IronMachineryAmbient extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> gears = new ArrayList<>();
        private final List<ItemDisplayHandle> axles = new ArrayList<>();
        private final List<ItemDisplayHandle> driveLinks = new ArrayList<>();
        private static final int SPAWN_LEN = 35;
        private static final int FADE_LEN = 45;
        private static final double WALL_X = -7.5;

        public IronMachineryAmbient(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_machinery_ambient", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 175);
            config.setDesignType("Structural ambience — iron machinery (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.3f, 0.4f);
            // 2 gears
            double[][] gearPos = {{WALL_X, 3.5, -1.2}, {WALL_X, 3.5, 1.2}};
            for (double[] p : gearPos) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(p[0], p[1], p[2]), new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(SPAWN_LEN, 0);
                gears.add(h); spawnedEntities.add(h.entity());
            }
            // 2 axles
            double[][] axlePos = {{WALL_X, 3.5, -1.2}, {WALL_X, 3.5, 1.2}};
            for (double[] p : axlePos) {
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(p[0] + 0.18, p[1], p[2]), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(70, 50, 30).interpolation(SPAWN_LEN, 5);
                axles.add(h); spawnedEntities.add(h.entity());
            }
            // 4 drive chain links
            for (int i = 0; i < 4; i++) {
                double t = i / 3.0;
                double zPos = -1.2 + t * 2.4;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(WALL_X + 0.1, 3.0, zPos), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(110, 110, 130).interpolation(SPAWN_LEN, 8);
                driveLinks.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            // Two gears rotating at independent rates
            for (int i = 0; i < gears.size(); i++) {
                float rotSpeed = (i == 0) ? 0.03f : -0.045f;
                float s = 0.7f * baseScale;
                double z = (i == 0) ? -1.2 : 1.2;
                gears.get(i).animateTo(
                        new Vector3f((float) WALL_X - s / 2, 3.5f - s / 2, (float) z - s / 2),
                        new AxisAngle4f(tick * rotSpeed, 1, 0, 0),
                        new Vector3f(s, s, s), 4);
            }
            for (int i = 0; i < axles.size(); i++) {
                float sx = 0.18f * baseScale;
                float sy = 0.45f * baseScale;
                double z = (i == 0) ? -1.2 : 1.2;
                axles.get(i).animateTo(
                        new Vector3f((float)(WALL_X + 0.18) - sx / 2, 3.5f - sy / 2, (float) z - sx / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sx), 4);
            }
            // Drive links — slow shift along chain length
            for (int i = 0; i < driveLinks.size(); i++) {
                double t = i / 3.0;
                double zOff = (Math.sin(tick * 0.04 + i) * 0.04);
                double zPos = -1.2 + t * 2.4 + zOff;
                float sx = 0.18f * baseScale;
                float sy = 0.4f * baseScale;
                driveLinks.get(i).animateTo(
                        new Vector3f((float)(WALL_X + 0.1) - sx / 2, 3.0f - sy / 2, (float) zPos - sx / 2),
                        new AxisAngle4f((float)(tick * 0.04), 0, 1, 0),
                        new Vector3f(sx, sy, sx), 4);
            }

            // Layered particles (3 types)
            if (tick % 12 == 0) {
                Location wc = c.clone().add(WALL_X + 0.4, 3.5, 0);
                w.spawnParticle(Particle.SMOKE, wc, 1, 0.4, 0.4, 0.6, 0.0);
                w.spawnParticle(Particle.ELECTRIC_SPARK, wc, 1, 0.3, 0.3, 0.5, 0.02);
                DisplayBuilder.dustParticles(wc, 1, 0.3, 200, 200, 220, 1.0f);
            }
            if (tick % 80 == 40) DisplayBuilder.playSound(c.clone().add(WALL_X, 3.5, 0), Sound.BLOCK_GRINDSTONE_USE, 0.15f, 0.55f);
        }

        @Override public AbstractAttack newInstance() { return new IronMachineryAmbient(plugin); }
    }

    // ================================================================
    // 55. CHAIN TETHER VISUAL — "The Connection"
    //     4 chain tethers (N/S/E/W) running from walls to ceiling anchor
    //     point. 6 links per tether. Middle links sag oscillation +/-0.03
    //     over 90-tick period.
    // ================================================================
    public static class ChainTetherVisual extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tetherLinks = new ArrayList<>();
        private final List<double[]> tetherInfo = new ArrayList<>(); // {x, y, z, sagFactor}
        private static final int LINKS_PER = 6;
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 45;
        private static final double[][] WALL_POS = {
                {0, 3.5,  7.5}, // north
                {0, 3.5, -7.5}, // south
                { 7.5, 3.5, 0}, // east
                {-7.5, 3.5, 0}  // west
        };
        private static final double[] CEILING = {0, 8.0, 0};

        public ChainTetherVisual(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_tether_visual", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 170);
            config.setDesignType("Structural ambience — chain tether visual (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.7f, 0.5f);
            for (double[] wall : WALL_POS) {
                for (int i = 0; i < LINKS_PER; i++) {
                    double t = i / (double)(LINKS_PER - 1);
                    double x = wall[0] + (CEILING[0] - wall[0]) * t;
                    double y = wall[1] + (CEILING[1] - wall[1]) * t;
                    double z = wall[2] + (CEILING[2] - wall[2]) * t;
                    double sagFactor = Math.sin(t * Math.PI);
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(100, 100, 115).interpolation(SPAWN_LEN, i);
                    tetherLinks.add(h); spawnedEntities.add(h.entity());
                    tetherInfo.add(new double[]{x, y, z, sagFactor});
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            double oscill = Math.sin(tick * (Math.PI * 2 / 90.0)) * 0.03;

            for (int i = 0; i < tetherLinks.size(); i++) {
                double[] info = tetherInfo.get(i);
                double dy = oscill * info[3];
                float sxz = 0.16f * baseScale;
                float sy = 0.42f * baseScale;
                tetherLinks.get(i).animateTo(
                        new Vector3f((float) info[0] - sxz / 2, (float)(info[1] + dy), (float) info[2] - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.005 + i * 0.2), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            // Particles at ceiling anchor + occasional tether midpoint
            if (tick % 25 == 0) {
                Location anchor = c.clone().add(CEILING[0], CEILING[1], CEILING[2]);
                w.spawnParticle(Particle.SMOKE, anchor, 1, 0.3, 0.1, 0.3, 0.0);
                DisplayBuilder.dustParticles(anchor, 1, 0.3, 95, 95, 110, 1.0f);
                w.spawnParticle(Particle.FALLING_DUST, anchor, 1, 0.2, 0.2, 0.2, 0.0, Material.IRON_BLOCK.createBlockData());
            }
            if (tick % 180 == 70) DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.3f, 0.45f);
        }

        @Override public AbstractAttack newInstance() { return new ChainTetherVisual(plugin); }
    }

    // ================================================================
    // 56. IRON SCAFFOLDING WALL — "The Construction"
    //     One wall has 3 vertical DARK_OAK_LOG posts + 5 horizontal
    //     platforms + 6 CHAIN railings. CAMPFIRE_COSY_SMOKE from base.
    // ================================================================
    public static class IronScaffoldingWall extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> verticals = new ArrayList<>();
        private final List<ItemDisplayHandle> horizontals = new ArrayList<>();
        private final List<ItemDisplayHandle> rails = new ArrayList<>();
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 50;
        private static final double WALL_Z = -7.5;

        public IronScaffoldingWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_scaffolding_wall", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 180);
            config.setDesignType("Structural ambience — iron scaffolding wall (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.5f);
            // 3 vertical posts
            for (int i = 0; i < 3; i++) {
                double x = (i - 1) * 1.5;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 1.5, WALL_Z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(70, 50, 30).interpolation(SPAWN_LEN, 0);
                verticals.add(h); spawnedEntities.add(h.entity());
            }
            // 5 horizontal platforms (different heights)
            for (int i = 0; i < 5; i++) {
                double y = 0.6 + i * 0.6;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(0, y, WALL_Z), new ItemStack(Material.DARK_OAK_LOG));
                h.scale(0.001f, 0.001f, 0.001f).glow(70, 50, 30).interpolation(SPAWN_LEN, 4);
                horizontals.add(h); spawnedEntities.add(h.entity());
            }
            // 6 CHAIN rail links
            for (int i = 0; i < 6; i++) {
                double y = 0.4 + (i / 3) * 1.5;
                double x = -1.5 + (i % 3) * 1.5;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, y, WALL_Z + 0.15), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(110, 110, 130).interpolation(SPAWN_LEN, 8);
                rails.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            for (int i = 0; i < verticals.size(); i++) {
                double x = (i - 1) * 1.5;
                float sx = 0.15f * baseScale;
                float sy = 3.0f * baseScale;
                float sz = 0.15f * baseScale;
                verticals.get(i).animateTo(
                        new Vector3f((float) x - sx / 2, 1.5f - sy / 2, (float) WALL_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < horizontals.size(); i++) {
                double y = 0.6 + i * 0.6;
                float sx = 1.5f * baseScale;
                float sy = 0.15f * baseScale;
                float sz = 0.15f * baseScale;
                horizontals.get(i).animateTo(
                        new Vector3f(0f - sx / 2, (float) y - sy / 2, (float) WALL_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 6);
            }
            for (int i = 0; i < rails.size(); i++) {
                double y = 0.4 + (i / 3) * 1.5;
                double x = -1.5 + (i % 3) * 1.5;
                float sxz = 0.16f * baseScale;
                float sy = 0.35f * baseScale;
                rails.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, (float) y - sy / 2, (float)(WALL_Z + 0.15) - sxz / 2),
                        new AxisAngle4f((float)(tick * 0.008 + i * 0.4), 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }

            // Smoke from base + layered ambient
            if (tick % 6 == 0) {
                Location base = c.clone().add(0, 0.1, WALL_Z + 0.4);
                w.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, base, 1, 0.7, 0.1, 0.3, 0.01);
            }
            if (tick % 12 == 0) {
                Location base = c.clone().add(0, 0.2, WALL_Z + 0.4);
                w.spawnParticle(Particle.SMOKE, base, 1, 0.6, 0.3, 0.3, 0.0);
                DisplayBuilder.dustParticles(base, 1, 0.4, 130, 90, 50, 1.0f);
            }
            if (tick % 220 == 90) DisplayBuilder.playSound(c.clone().add(0, 1.5, WALL_Z), Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new IronScaffoldingWall(plugin); }
    }

    // ================================================================
    // 57. ANCHOR CHAIN FLOOR RING — "The Mooring"
    //     10 CHAIN links in a circle r=0.9 at Y=0.04 + 1 IRON_BLOCK
    //     mooring bolt. Very slight rotation (0.5 deg/tick).
    // ================================================================
    public static class AnchorChainFloorRing extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> ring = new ArrayList<>();
        private ItemDisplayHandle bolt;
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 40;
        private static final double RING_R = 0.9;
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
            for (int i = 0; i < 10; i++) {
                double a = Math.PI * 2 * i / 10;
                double x = RING_X + Math.cos(a) * RING_R;
                double z = RING_Z + Math.sin(a) * RING_R;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.04, z), new ItemStack(Material.CHAIN));
                h.scale(0.001f, 0.001f, 0.001f).glow(100, 100, 115).interpolation(SPAWN_LEN, i);
                ring.add(h); spawnedEntities.add(h.entity());
            }
            bolt = displayBuilder.spawnItem(c.clone().add(RING_X + RING_R + 0.2, 0.05, RING_Z), new ItemStack(Material.IRON_BLOCK));
            bolt.scale(0.001f, 0.001f, 0.001f).glow(170, 170, 200).interpolation(SPAWN_LEN, 0);
            spawnedEntities.add(bolt.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            double rot = Math.toRadians(0.5) * tick; // very slight rotation
            for (int i = 0; i < ring.size(); i++) {
                double a = Math.PI * 2 * i / 10 + rot;
                double x = RING_X + Math.cos(a) * RING_R;
                double z = RING_Z + Math.sin(a) * RING_R;
                float sxz = 0.18f * baseScale;
                float sy = 0.3f * baseScale;
                ring.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, 0.04f, (float) z - sxz / 2),
                        new AxisAngle4f((float) a, 0, 1, 0),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (bolt != null) {
                float s = 0.25f * baseScale;
                bolt.animateTo(
                        new Vector3f((float)(RING_X + RING_R + 0.2) - s / 2, 0.05f, (float) RING_Z - s / 2),
                        new AxisAngle4f((float)(tick * 0.005), 0, 1, 0),
                        new Vector3f(s, s * 0.6f, s), 8);
            }
            if (tick % 30 == 0) {
                Location rc = c.clone().add(RING_X, 0.1, RING_Z);
                w.spawnParticle(Particle.SMOKE, rc, 1, RING_R, 0.05, RING_R, 0.0);
                DisplayBuilder.dustParticles(rc, 1, RING_R, 90, 90, 100, 1.0f);
                w.spawnParticle(Particle.FALLING_DUST, rc, 1, RING_R, 0.1, RING_R, 0.0, Material.IRON_BLOCK.createBlockData());
            }
            if (tick % 260 == 100) DisplayBuilder.playSound(c.clone().add(RING_X, 0, RING_Z), Sound.BLOCK_CHAIN_FALL, 0.25f, 0.45f);
        }

        @Override public AbstractAttack newInstance() { return new AnchorChainFloorRing(plugin); }
    }

    // ================================================================
    // 58. CHAIN WALL SECTION — "The Barrier"
    //     5 columns x 4 rows of CHAIN links forming a chain curtain wall.
    //     Each column has independent sway (+/-3 deg X, 20-tick offset).
    // ================================================================
    public static class ChainWallSection extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> wallLinks = new ArrayList<>();
        private final int[] colIndex = new int[20];
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 50;
        private static final double WALL_X = 7.5;

        public ChainWallSection(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_wall_section", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1500, 175);
            config.setDesignType("Structural ambience — chain wall section (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.45f);
            int idx = 0;
            for (int col = 0; col < 5; col++) {
                for (int row = 0; row < 4; row++) {
                    double z = -0.8 + col * 0.4;
                    double y = 0.5 + row * 0.4;
                    ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(WALL_X, y, z), new ItemStack(Material.CHAIN));
                    h.scale(0.001f, 0.001f, 0.001f).glow(105, 105, 120).interpolation(SPAWN_LEN, col * 4);
                    wallLinks.add(h); spawnedEntities.add(h.entity());
                    colIndex[idx++] = col;
                }
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            for (int i = 0; i < wallLinks.size(); i++) {
                int col = colIndex[i];
                int row = i % 4;
                double z = -0.8 + col * 0.4;
                double y = 0.5 + row * 0.4;
                float swayAngle = (float) Math.toRadians(3.0 * Math.sin((tick - col * 20) * 0.05));
                float sxz = 0.16f * baseScale;
                float sy = 0.5f * baseScale;
                wallLinks.get(i).animateTo(
                        new Vector3f((float) WALL_X - sxz / 2, (float) y - sy / 2, (float) z - sxz / 2),
                        new AxisAngle4f(swayAngle, 1, 0, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }

            if (tick % 18 == 0) {
                Location wc = c.clone().add(WALL_X - 0.2, 1.5, 0);
                w.spawnParticle(Particle.SMOKE, wc, 1, 0.1, 0.8, 1.0, 0.0);
                w.spawnParticle(Particle.FALLING_DUST, wc, 1, 0.1, 0.6, 1.0, 0.0, Material.CHAIN.createBlockData());
                DisplayBuilder.dustParticles(wc, 1, 0.6, 100, 100, 115, 0.9f);
            }
            if (tick % 200 == 80) DisplayBuilder.playSound(c.clone().add(WALL_X, 1.5, 0), Sound.BLOCK_CHAIN_FALL, 0.3f, 0.5f);
        }

        @Override public AbstractAttack newInstance() { return new ChainWallSection(plugin); }
    }

    // ================================================================
    // 59. IRON SPIKE FLOOR STRIP — "The Edge"
    //     8 small IRON_BLOCK spikes along wall base, slightly angled
    //     outward, ELECTRIC_SPARK every 30 ticks. Non-damaging.
    // ================================================================
    public static class IronSpikeFloorStrip extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> spikes = new ArrayList<>();
        private static final int SPAWN_LEN = 30;
        private static final int FADE_LEN = 40;
        private static final double STRIP_Z = -7.4;

        public IronSpikeFloorStrip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_spike_floor_strip", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            configureAmbient(config, 1400, 160);
            config.setDesignType("Structural ambience — iron spike floor strip (no damage, decor)");
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 0.4f, 0.6f);
            for (int i = 0; i < 8; i++) {
                double x = -3.5 + i * 1.0;
                ItemDisplayHandle h = displayBuilder.spawnItem(c.clone().add(x, 0.2, STRIP_Z), new ItemStack(Material.IRON_BLOCK));
                h.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 210).interpolation(SPAWN_LEN, i);
                spikes.add(h); spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);

            for (int i = 0; i < spikes.size(); i++) {
                double x = -3.5 + i * 1.0;
                float sxz = 0.1f * baseScale;
                float sy = 0.4f * baseScale;
                // Subtle outward lean: rotation around X axis depending on side
                float lean = (float) Math.toRadians(8.0 + Math.sin(tick * 0.04 + i) * 1.0);
                spikes.get(i).animateTo(
                        new Vector3f((float) x - sxz / 2, 0.2f, (float) STRIP_Z - sxz / 2),
                        new AxisAngle4f(lean, 1, 0, 0),
                        new Vector3f(sxz, sy, sxz), 6);
            }
            if (tick % 30 == 0) {
                for (int i = 0; i < spikes.size(); i++) {
                    double x = -3.5 + i * 1.0;
                    Location tipLoc = c.clone().add(x, 0.6, STRIP_Z);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, tipLoc, 1, 0.05, 0.05, 0.05, 0.02);
                    if (i % 2 == 0) DisplayBuilder.dustParticles(tipLoc, 1, 0.15, 200, 200, 220, 0.9f);
                    if (i % 3 == 0) w.spawnParticle(Particle.SMOKE, tipLoc, 1, 0.1, 0.1, 0.1, 0.0);
                }
            }
            if (tick % 240 == 110) DisplayBuilder.playSound(c.clone().add(0, 0.5, STRIP_Z), Sound.BLOCK_ANVIL_PLACE, 0.2f, 0.55f);
        }

        @Override public AbstractAttack newInstance() { return new IronSpikeFloorStrip(plugin); }
    }

    // ================================================================
    // 60. CHAIN LOCK MECHANISM — "The Seal"
    //     NETHERITE_BLOCK lock body, CHAIN shackle arc (2 links), small
    //     IRON_BLOCK keyhole. Pulses ELECTRIC_SPARK every 40 ticks.
    // ================================================================
    public static class ChainLockMechanism extends EnvironmentalAttack {
        private ItemDisplayHandle body;
        private ItemDisplayHandle shackleLeft;
        private ItemDisplayHandle shackleRight;
        private ItemDisplayHandle keyhole;
        private static final int SPAWN_LEN = 40;
        private static final int FADE_LEN = 50;
        private static final double DOOR_X = 0;
        private static final double DOOR_Y = 2.5;
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
            body.scale(0.001f, 0.001f, 0.001f).glow(50, 40, 60).interpolation(SPAWN_LEN, 0);
            spawnedEntities.add(body.entity());

            shackleLeft = displayBuilder.spawnItem(c.clone().add(DOOR_X - 0.3, DOOR_Y + 0.5, DOOR_Z - 0.1), new ItemStack(Material.CHAIN));
            shackleLeft.scale(0.001f, 0.001f, 0.001f).glow(110, 110, 130).interpolation(SPAWN_LEN, 5);
            spawnedEntities.add(shackleLeft.entity());

            shackleRight = displayBuilder.spawnItem(c.clone().add(DOOR_X + 0.3, DOOR_Y + 0.5, DOOR_Z - 0.1), new ItemStack(Material.CHAIN));
            shackleRight.scale(0.001f, 0.001f, 0.001f).glow(110, 110, 130).interpolation(SPAWN_LEN, 5);
            spawnedEntities.add(shackleRight.entity());

            keyhole = displayBuilder.spawnItem(c.clone().add(DOOR_X, DOOR_Y, DOOR_Z - 0.26), new ItemStack(Material.IRON_BLOCK));
            keyhole.scale(0.001f, 0.001f, 0.001f).glow(180, 180, 200).interpolation(SPAWN_LEN, 8);
            spawnedEntities.add(keyhole.entity());
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();
            float baseScale = lifecycleScale(tick, config.getDurationTicks(), SPAWN_LEN, FADE_LEN, 1.0f);
            float pulse = (float)(1.0 + Math.sin(tick * 0.04) * 0.02);

            if (body != null) {
                float sx = 0.8f * baseScale * pulse;
                float sy = 0.8f * baseScale * pulse;
                float sz = 0.5f * baseScale * pulse;
                body.animateTo(
                        new Vector3f((float) DOOR_X - sx / 2, (float) DOOR_Y - sy / 2, (float) DOOR_Z - sz / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(sx, sy, sz), 8);
            }
            if (shackleLeft != null) {
                float sxz = 0.16f * baseScale;
                float sy = 0.6f * baseScale;
                shackleLeft.animateTo(
                        new Vector3f((float)(DOOR_X - 0.3) - sxz / 2, (float)(DOOR_Y + 0.5) - sy / 2, (float)(DOOR_Z - 0.1) - sxz / 2),
                        new AxisAngle4f((float) Math.toRadians(-25.0), 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (shackleRight != null) {
                float sxz = 0.16f * baseScale;
                float sy = 0.6f * baseScale;
                shackleRight.animateTo(
                        new Vector3f((float)(DOOR_X + 0.3) - sxz / 2, (float)(DOOR_Y + 0.5) - sy / 2, (float)(DOOR_Z - 0.1) - sxz / 2),
                        new AxisAngle4f((float) Math.toRadians(25.0), 0, 0, 1),
                        new Vector3f(sxz, sy, sxz), 8);
            }
            if (keyhole != null) {
                float s = 0.15f * baseScale;
                keyhole.animateTo(
                        new Vector3f((float) DOOR_X - s / 2, (float) DOOR_Y - s / 2, (float)(DOOR_Z - 0.26) - s / 2),
                        new AxisAngle4f((float)(tick * 0.02), 0, 0, 1),
                        new Vector3f(s, s, s * 0.4f), 6);
            }

            // ELECTRIC_SPARK pulse every 40t + layered ambient
            if (tick % 40 == 0) {
                Location lc = c.clone().add(DOOR_X, DOOR_Y, DOOR_Z - 0.2);
                w.spawnParticle(Particle.ELECTRIC_SPARK, lc, 4, 0.4, 0.4, 0.1, 0.02);
                w.spawnParticle(Particle.SMOKE, lc, 1, 0.3, 0.3, 0.1, 0.0);
                DisplayBuilder.dustParticles(lc, 2, 0.35, 80, 50, 100, 1.2f);
            }
            // BLOCK_CHAIN_PLACE sound every 2 minutes (~2400t)
            if (tick % 2400 == 600) DisplayBuilder.playSound(c.clone().add(DOOR_X, DOOR_Y, DOOR_Z), Sound.BLOCK_CHAIN_PLACE, 0.4f, 0.4f);
        }

        @Override public AbstractAttack newInstance() { return new ChainLockMechanism(plugin); }
    }
}
