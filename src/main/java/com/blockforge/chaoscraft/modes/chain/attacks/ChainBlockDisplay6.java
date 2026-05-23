package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 51-60 (Projectile / Cages)
 *
 * Second half of Category 5 (Reaching & Projectile) plus opening of
 * Category 6 (Cage & Enclosure). Heavy industrial chain/iron theme;
 * 30+ block displays each.
 *
 * Block palette: IRON_BLOCK, NETHERITE_BLOCK, CHAIN, GRAY_CONCRETE,
 *                DARK_OAK_LOG, POLISHED_BLACKSTONE, CHISELED_POLISHED_BLACKSTONE
 *
 * Particles: BLOCK(IRON_BLOCK/NETHERITE_BLOCK/CHAIN), CRIT, SMOKE,
 *            LARGE_SMOKE, ELECTRIC_SPARK, EXPLOSION, DUST, END_ROD,
 *            SOUL_FIRE_FLAME
 * Sounds: BLOCK_CHAIN_PLACE, BLOCK_CHAIN_FALL, BLOCK_CHAIN_HIT,
 *         BLOCK_ANVIL_LAND, BLOCK_ANVIL_HIT, BLOCK_NETHERITE_BLOCK_PLACE,
 *         BLOCK_NETHERITE_BLOCK_HIT, ENTITY_IRON_GOLEM_ATTACK,
 *         BLOCK_GRINDSTONE_USE, BLOCK_PISTON_EXTEND, BLOCK_PISTON_CONTRACT
 *
 * Attacks:
 *  51. IronBola             — 3-weight rotating triangle, impact-only (wide arc tangle)
 *  52. ChainWhiplash        — slow extension, fast snap-back (recoiling whip)
 *  53. IronBolt             — railroad spike, impact-only (predictive bolt)
 *  54. WreckingBallRoll     — ground-rolling sphere, constant + FOLLOW-AI 0.10
 *  55. ChainSpear           — heavy lance with helical chain wrap, impact-only
 *  56. NetheriteCage        — top-down assembled lockdown cage
 *  57. ChainDome            — bell-jar dome enclosure (find edge gap)
 *  58. IronCubeCrush        — 4 walls closing inward (escape vertical/perimeter)
 *  59. BindingSpiral        — tightening ground spiral (escape outward)
 *  60. ChainMandala         — concentric ground sigil (read sectors)
 */
public final class ChainBlockDisplay6 {
    private ChainBlockDisplay6() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IronBola(plugin));
        registry.register(new ChainWhiplash(plugin));
        registry.register(new IronBolt(plugin));
        registry.register(new WreckingBallRoll(plugin));
        registry.register(new ChainSpear(plugin));
        registry.register(new NetheriteCage(plugin));
        registry.register(new ChainDome(plugin));
        registry.register(new IronCubeCrush(plugin));
        registry.register(new BindingSpiral(plugin));
        registry.register(new ChainMandala(plugin));
    }

    // ================================================================
    // Shared helpers (mirrored from ChainBlockDisplay5)
    // ================================================================

    private static Player findNearestPlayer(Location center, double range) {
        if (center == null || center.getWorld() == null) return null;
        Player nearest = null;
        double nearestDist = range * range;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            double dist = p.getLocation().distanceSquared(center);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    private static void setScale(BlockDisplay e, float sx, float sy, float sz, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void rotateOn(BlockDisplay e, float angle, float ax, float ay, float az, int dur) {
        if (e == null || !e.isValid()) return;
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(dur);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    private static void shrinkToZero(BlockDisplayHandle h, int dur) {
        if (h == null) return;
        setScale(h.entity(), 0f, 0f, 0f, dur);
    }

    // ================================================================
    // #51 — IRON BOLA ("The Tangle")
    // 3 iron weights on chains in a rotating triangle, hurled at a player.
    // 33 blocks: per weight (3): 1 IRON_BLOCK core + 4 IRON_BLOCK poles + 4 CHAIN spokes
    // + 2 NETHERITE_BLOCK formation hubs at origin/aim
    // Impact-only — wide spread, single sidestep insufficient.
    // ================================================================
    public static class IronBola extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> weights = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> hubs = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private float zSpinAngle = 0f;

        public IronBola(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_bola", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(280.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(80);
            config.setCooldownTicks(200);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Bola projectile (sidestep wide arc)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            targetLoc = (target != null) ? target.getLocation().clone().add(0, 1.0, 0) : center.clone().add(0, 1, 0);
            // Origin at perimeter -14X
            origin = center.clone().add(-14.0, 2.0, 0.0);

            // 2 formation hubs (origin marker + tracking center)
            BlockDisplayHandle hubOrigin = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            hubOrigin.scale(0f, 0f, 0f).glow(70, 70, 80).interpolation(6, 0);
            spawnedEntities.add(hubOrigin.entity());
            hubs.add(hubOrigin);
            all.add(hubOrigin);
            setScale(hubOrigin.entity(), 0.4f, 0.4f, 0.4f, 8);

            BlockDisplayHandle hubAim = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            hubAim.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(6, 0);
            spawnedEntities.add(hubAim.entity());
            hubs.add(hubAim);
            all.add(hubAim);
            setScale(hubAim.entity(), 0.28f, 0.28f, 0.28f, 8);

            // 3 weights arranged at 120 degrees around triangle center
            for (int wi = 0; wi < 3; wi++) {
                List<BlockDisplayHandle> weight = new ArrayList<>();
                List<BlockDisplayHandle> spk = new ArrayList<>();
                double baseAngle = wi * (Math.PI * 2.0 / 3.0);
                double wx = Math.cos(baseAngle) * 1.2;
                double wy = Math.sin(baseAngle) * 1.2;

                // 1 core IRON_BLOCK
                BlockDisplayHandle core = displayBuilder.spawnBlock(
                        origin.clone().add(wx, wy, 0), Material.IRON_BLOCK);
                core.scale(0f, 0f, 0f).glow(220, 220, 235).interpolation(6, 0);
                spawnedEntities.add(core.entity());
                weight.add(core);
                all.add(core);
                setScale(core.entity(), 0.4f, 0.4f, 0.4f, 8);

                // 4 poles around core (sphere-approx)
                double[][] poleOffs = {{0.32, 0, 0}, {-0.32, 0, 0}, {0, 0.32, 0}, {0, -0.32, 0}};
                for (double[] po : poleOffs) {
                    BlockDisplayHandle pole = displayBuilder.spawnBlock(
                            origin.clone().add(wx + po[0], wy + po[1], po[2]), Material.IRON_BLOCK);
                    pole.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(6, 1);
                    spawnedEntities.add(pole.entity());
                    weight.add(pole);
                    all.add(pole);
                    setScale(pole.entity(), 0.18f, 0.18f, 0.18f, 8);
                }

                // 4 CHAIN spokes from center to weight
                for (int si = 0; si < 4; si++) {
                    double f = (si + 1) * 0.24;
                    BlockDisplayHandle s = displayBuilder.spawnBlock(
                            origin.clone().add(wx * f, wy * f, 0), Material.CHAIN);
                    s.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(6, 2 + si);
                    spawnedEntities.add(s.entity());
                    spk.add(s);
                    all.add(s);
                    setScale(s.entity(), 0.12f, 0.12f, 0.12f, 8);
                }

                weights.add(weight);
                spokes.add(spk);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.9f);
            w.spawnParticle(Particle.SMOKE, origin, 25, 0.8, 0.8, 0.8, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 6;
            int flightEnd = 50;
            int impactTick = 52;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);
                zSpinAngle += (float) Math.toRadians(30.0);

                double cx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double cy = origin.getY() + (targetLoc.getY() - origin.getY()) * progress;
                double cz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                Location anchor = new Location(c.getWorld(), cx, cy, cz);

                // Move the 2 hubs along
                if (hubs.size() >= 2) {
                    BlockDisplay e0 = hubs.get(0).entity();
                    BlockDisplay e1 = hubs.get(1).entity();
                    if (e0 != null && e0.isValid()) try { e0.teleport(anchor); } catch (Throwable ignored) {}
                    if (e1 != null && e1.isValid()) try { e1.teleport(anchor); } catch (Throwable ignored) {}
                }

                // Rotate the triangle on Z, each weight at r=1.2 around formation center
                for (int wi = 0; wi < weights.size(); wi++) {
                    double a = wi * (Math.PI * 2.0 / 3.0) + zSpinAngle;
                    double wx = Math.cos(a) * 1.2;
                    double wy = Math.sin(a) * 1.2;

                    List<BlockDisplayHandle> weight = weights.get(wi);
                    // Weight core
                    {
                        BlockDisplay e = weight.get(0).entity();
                        if (e != null && e.isValid()) {
                            try { e.teleport(anchor.clone().add(wx, wy, 0)); } catch (Throwable ignored) {}
                        }
                    }
                    // 4 poles around core
                    double[][] poleOffs = {{0.32, 0, 0}, {-0.32, 0, 0}, {0, 0.32, 0}, {0, -0.32, 0}};
                    for (int p = 0; p < 4; p++) {
                        BlockDisplay e = weight.get(1 + p).entity();
                        if (e != null && e.isValid()) {
                            try {
                                e.teleport(anchor.clone().add(wx + poleOffs[p][0], wy + poleOffs[p][1], poleOffs[p][2]));
                            } catch (Throwable ignored) {}
                        }
                    }
                    // Spokes from center to weight
                    List<BlockDisplayHandle> spk = spokes.get(wi);
                    for (int si = 0; si < spk.size(); si++) {
                        double f = (si + 1) * 0.24;
                        BlockDisplay e = spk.get(si).entity();
                        if (e != null && e.isValid()) {
                            try {
                                e.teleport(anchor.clone().add(wx * f, wy * f, 0));
                            } catch (Throwable ignored) {}
                        }
                    }

                    // CRIT particles from each weight
                    if (tick % 2 == 0) {
                        c.getWorld().spawnParticle(Particle.CRIT,
                                anchor.clone().add(wx, wy, 0), 3, 0.2, 0.2, 0.2, 0.08);
                    }
                }
            }

            // Flight sound
            if (tick > flightStart && tick < flightEnd && tick % 5 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 1.0f);
            }

            // Impact
            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                for (int i = 0; i < 3; i++) {
                    DisplayBuilder.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.3f, 0.5f + i * 0.1f);
                }
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.8f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 10, 1.2, 0.6, 1.2, 0.05);
                c.getWorld().spawnParticle(Particle.CRIT, targetLoc, 25, 1.6, 0.8, 1.6, 0.2);
                c.getWorld().spawnParticle(Particle.BLOCK, targetLoc, 18,
                        1.5, 0.6, 1.5, 0.04, Material.IRON_BLOCK.createBlockData());
                for (BlockDisplayHandle h : all) shrinkToZero(h, 8);
            }

            if (tick == 70) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 6);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronBola(plugin); }
    }

    // ================================================================
    // #52 — CHAIN WHIPLASH STRIKE ("The Recoil")
    // Chain extends slowly forward then snaps back at 3x speed.
    // 30 blocks: 14 CHAIN links + 1 IRON tip + 1 IRON anchor + 14 helper segs
    // ================================================================
    public static class ChainWhiplash extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> links = new ArrayList<>();
        private final List<BlockDisplayHandle> tipParts = new ArrayList<>();
        private final List<BlockDisplayHandle> anchorParts = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Vector strikeDir;
        private double extension = 0.0;

        public ChainWhiplash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whiplash", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(140);
            config.setCooldownTicks(220);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Recoiling whip (read counter-arc)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            if (target != null) {
                double dx = target.getLocation().getX() - center.getX();
                double dz = target.getLocation().getZ() - center.getZ();
                double d = Math.sqrt(dx*dx + dz*dz);
                if (d < 0.01) d = 1.0;
                strikeDir = new Vector(dx / d, 0, dz / d);
            } else {
                strikeDir = new Vector(1, 0, 0);
            }

            double cy = 1.6;

            // 1 IRON_BLOCK anchor at the swing root
            BlockDisplayHandle root = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.IRON_BLOCK);
            root.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(8, 0);
            spawnedEntities.add(root.entity());
            anchorParts.add(root);
            all.add(root);
            setScale(root.entity(), 0.55f, 0.55f, 0.55f, 10);

            // 14 chain links (start collapsed at root, will extend along strikeDir)
            for (int i = 0; i < 14; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, cy, 0), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(8, i / 2);
                spawnedEntities.add(h.entity());
                links.add(h);
                all.add(h);
                float s = 0.28f - i * 0.013f; // taper 0.28 -> 0.10
                setScale(h.entity(), s, s, s, 10);
            }

            // 1 IRON tip weight at end
            BlockDisplayHandle tip = displayBuilder.spawnBlock(center.clone().add(0, cy, 0), Material.IRON_BLOCK);
            tip.scale(0f, 0f, 0f).glow(230, 230, 240).interpolation(8, 4);
            spawnedEntities.add(tip.entity());
            tipParts.add(tip);
            all.add(tip);
            setScale(tip.entity(), 0.58f, 0.58f, 0.58f, 10);

            // 14 helper anchor brace pieces around root (collar)
            for (int i = 0; i < 14; i++) {
                double a = (Math.PI * 2.0 * i) / 14.0;
                Location loc = center.clone().add(Math.cos(a) * 0.55, cy, Math.sin(a) * 0.55);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(70, 70, 80).interpolation(6, 1);
                spawnedEntities.add(h.entity());
                anchorParts.add(h);
                all.add(h);
                setScale(h.entity(), 0.14f, 0.16f, 0.14f, 8);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.6f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, cy, 0), 25, 1.0, 0.6, 1.0, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (strikeDir == null) return;

            double cy = 1.6;
            // Phase A: slow extend (ticks 10-45), reach maximum 6.5 blocks
            // Phase B: hold briefly (45-55)
            // Phase C: fast snap-back (55-65)
            // Phase D: secondary slower extend (75-105), then snap-back (105-115)

            int phaseADuration = 35;
            if (tick >= 10 && tick <= 10 + phaseADuration) {
                double p = (tick - 10) / (double) phaseADuration;
                extension = 6.5 * p;
                positionWhip(c, cy, 18); // slow interp
                if (tick % 4 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.8f);
                }
            }
            if (tick >= 55 && tick <= 65) {
                double p = (tick - 55) / 10.0;
                extension = 6.5 * (1.0 - p);
                positionWhip(c, cy, 5); // fast snap-back
                if (tick == 55) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.5f, 1.4f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 1.2f);
                }
                if (tick == 65) {
                    // Tip impact particles when snap completes back at root
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 6, 0.6, 0.4, 0.6, 0.05);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, cy, 0), 20, 1.0, 0.5, 1.0, 0.15);
                }
            }
            // Second extend-retract pair (different angle): rotate strikeDir slightly
            if (tick == 75) {
                // Rotate strike dir 45 deg right for follow-up
                double cs = Math.cos(Math.toRadians(45));
                double sn = Math.sin(Math.toRadians(45));
                double nx = strikeDir.getX() * cs - strikeDir.getZ() * sn;
                double nz = strikeDir.getX() * sn + strikeDir.getZ() * cs;
                strikeDir = new Vector(nx, 0, nz);
            }
            if (tick >= 75 && tick <= 105) {
                double p = (tick - 75) / 30.0;
                extension = 6.5 * p;
                positionWhip(c, cy, 14);
            }
            if (tick >= 105 && tick <= 115) {
                double p = (tick - 105) / 10.0;
                extension = 6.5 * (1.0 - p);
                positionWhip(c, cy, 5);
                if (tick == 105) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 1.3f);
                }
            }

            // Crit trail along whip every 2 ticks during extension/retract
            if (tick % 2 == 0 && extension > 0.5) {
                Location mid = c.clone().add(strikeDir.getX() * (extension * 0.7), cy, strikeDir.getZ() * (extension * 0.7));
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, mid, 3, 0.3, 0.3, 0.3, 0.08);
                c.getWorld().spawnParticle(Particle.CRIT, mid, 2, 0.2, 0.2, 0.2, 0.05);
            }

            int duration = config.getDurationTicks();
            if (tick == duration - 15) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 12);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.2f, 0.6f);
            }
        }

        private void positionWhip(Location c, double cy, int interp) {
            // Tip at extension distance
            Location tipLoc = c.clone().add(strikeDir.getX() * extension, cy, strikeDir.getZ() * extension);
            for (BlockDisplayHandle h : tipParts) {
                BlockDisplay e = h.entity();
                if (e == null || !e.isValid()) continue;
                e.setInterpolationDuration(interp);
                e.setInterpolationDelay(0);
                try { e.teleport(tipLoc); } catch (Throwable ignored) {}
            }
            // Distribute 14 links evenly between root and tip
            for (int i = 0; i < links.size(); i++) {
                double f = (i + 1) / (links.size() + 1.0);
                Location loc = c.clone().add(strikeDir.getX() * extension * f, cy, strikeDir.getZ() * extension * f);
                BlockDisplay e = links.get(i).entity();
                if (e == null || !e.isValid()) continue;
                e.setInterpolationDuration(interp);
                e.setInterpolationDelay(0);
                try { e.teleport(loc); } catch (Throwable ignored) {}
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainWhiplash(plugin); }
    }

    // ================================================================
    // #53 — IRON BOLT ("The Spike")
    // Railroad spike: square head + cylindrical shaft + tapered point.
    // 30 blocks: 1 head + 8 shaft + 1 tip core + 4 taper + 1 tail anchor
    // + 15 trail/ring fittings.
    // Impact-only — predictive bolt fired at lead position.
    // ================================================================
    public static class IronBolt extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> tip = new ArrayList<>();
        private final List<BlockDisplayHandle> taper = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> rings = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private Vector flightDir;

        public IronBolt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_bolt", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(400.0);
            config.setImpactRadius(1.6);
            config.setDurationTicks(60);
            config.setCooldownTicks(180);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Predictive bolt (read lead aim)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            Location aim = (target != null) ? target.getLocation().clone() : center.clone();
            if (target != null) {
                Vector v = target.getVelocity();
                aim = aim.clone().add(v.getX() * 18, 0, v.getZ() * 18);
            }
            targetLoc = aim.clone().add(0, 0.8, 0);
            origin = center.clone().add(0.0, 8.0, -12.0);

            double dx = targetLoc.getX() - origin.getX();
            double dy = targetLoc.getY() - origin.getY();
            double dz = targetLoc.getZ() - origin.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 1.0;
            flightDir = new Vector(dx / dist, dy / dist, dz / dist);

            // 1 head (square)
            BlockDisplayHandle hd = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
            hd.scale(0f, 0f, 0f).glow(220, 220, 235).interpolation(5, 0);
            spawnedEntities.add(hd.entity());
            head.add(hd);
            all.add(hd);
            setScale(hd.entity(), 0.6f, 0.4f, 0.6f, 6);

            // 8 shaft (iron) — placed along axis, animated each tick
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(5, i / 2);
                spawnedEntities.add(h.entity());
                shaft.add(h);
                all.add(h);
                setScale(h.entity(), 0.24f, 0.24f, 0.48f, 6);
            }

            // 1 tip core (netherite)
            BlockDisplayHandle tc = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            tc.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(5, 0);
            spawnedEntities.add(tc.entity());
            tip.add(tc);
            all.add(tc);
            setScale(tc.entity(), 0.2f, 0.2f, 0.32f, 6);

            // 4 taper slabs around tip
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                taper.add(h);
                all.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.22f, 6);
            }

            // 1 tail anchor brace (netherite)
            BlockDisplayHandle ta = displayBuilder.spawnBlock(origin.clone(), Material.NETHERITE_BLOCK);
            ta.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(5, 0);
            spawnedEntities.add(ta.entity());
            head.add(ta);
            all.add(ta);
            setScale(ta.entity(), 0.45f, 0.18f, 0.45f, 6);

            // 8 chain trail behind
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(5, i);
                spawnedEntities.add(h.entity());
                trail.add(h);
                all.add(h);
                float ts = 0.18f - i * 0.015f;
                setScale(h.entity(), ts, ts, ts, 6);
            }

            // 7 ring fittings around head (decorative)
            for (int i = 0; i < 7; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                rings.add(h);
                all.add(h);
                setScale(h.entity(), 0.1f, 0.1f, 0.1f, 6);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.9f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_HIT, 1.0f, 1.0f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, origin, 22, 0.5, 0.5, 0.5, 0.12);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 5;
            int flightEnd = 30;
            int impactTick = 32;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);

                double cx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double cy = origin.getY() + (targetLoc.getY() - origin.getY()) * progress;
                double cz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                Location anchor = new Location(c.getWorld(), cx, cy, cz);

                // Head & tail anchor: at base of bolt (rear)
                Location rear = anchor.clone().add(-flightDir.getX() * 1.0, -flightDir.getY() * 1.0, -flightDir.getZ() * 1.0);
                if (!head.isEmpty()) {
                    BlockDisplay e0 = head.get(0).entity();
                    if (e0 != null && e0.isValid()) try { e0.teleport(rear); } catch (Throwable ignored) {}
                }
                if (head.size() > 1) {
                    Location farRear = anchor.clone().add(-flightDir.getX() * 1.25, -flightDir.getY() * 1.25, -flightDir.getZ() * 1.25);
                    BlockDisplay e1 = head.get(1).entity();
                    if (e1 != null && e1.isValid()) try { e1.teleport(farRear); } catch (Throwable ignored) {}
                }

                // Shaft along axis from -0.8 to +0.8 around anchor
                for (int i = 0; i < shaft.size(); i++) {
                    double off = -0.8 + (i / (double)(shaft.size() - 1)) * 1.6;
                    Location loc = anchor.clone().add(flightDir.getX() * off, flightDir.getY() * off, flightDir.getZ() * off);
                    BlockDisplay e = shaft.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Tip core in front
                if (!tip.isEmpty()) {
                    Location forward = anchor.clone().add(flightDir.getX() * 1.05, flightDir.getY() * 1.05, flightDir.getZ() * 1.05);
                    BlockDisplay e = tip.get(0).entity();
                    if (e != null && e.isValid()) try { e.teleport(forward); } catch (Throwable ignored) {}
                }
                // Taper around tip
                double[][] taperOffs = {{0.08, 0, 0}, {-0.08, 0, 0}, {0, 0.08, 0}, {0, -0.08, 0}};
                for (int i = 0; i < taper.size() && i < taperOffs.length; i++) {
                    Location loc = anchor.clone().add(
                            flightDir.getX() * 0.9 + taperOffs[i][0],
                            flightDir.getY() * 0.9 + taperOffs[i][1],
                            flightDir.getZ() * 0.9 + taperOffs[i][2]);
                    BlockDisplay e = taper.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Chain trail behind
                for (int i = 0; i < trail.size(); i++) {
                    double back = -1.5 - i * 0.4;
                    Location loc = anchor.clone().add(flightDir.getX() * back, flightDir.getY() * back, flightDir.getZ() * back);
                    BlockDisplay e = trail.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Rings around rear hub
                for (int i = 0; i < rings.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / rings.size();
                    Location loc = anchor.clone().add(
                            -flightDir.getX() * 0.8 + Math.cos(a) * 0.45,
                            -flightDir.getY() * 0.8 + Math.sin(a) * 0.2,
                            -flightDir.getZ() * 0.8);
                    BlockDisplay e = rings.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }

                // Trail particles
                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.BLOCK, anchor, 6,
                            0.3, 0.3, 0.3, 0.05, Material.IRON_BLOCK.createBlockData());
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, anchor, 2, 0.2, 0.2, 0.2, 0.06);
                }
            }

            if (tick > flightStart && tick < flightEnd && tick % 3 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.55f, 1.3f);
            }

            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.7f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 6, 0.6, 0.4, 0.6, 0.05);
                c.getWorld().spawnParticle(Particle.BLOCK, targetLoc, 25,
                        1.0, 0.5, 1.0, 0.05, Material.IRON_BLOCK.createBlockData());
                c.getWorld().spawnParticle(Particle.CRIT, targetLoc, 20, 1.0, 0.5, 1.0, 0.25);
            }

            // Embed: bolt stays for 20 ticks after impact at targetLoc
            if (tick == impactTick + 1) {
                Location embed = targetLoc.clone().add(0, -0.2, 0);
                Location embedTip = embed.clone().add(flightDir.getX() * 0.6, flightDir.getY() * 0.6, flightDir.getZ() * 0.6);
                if (!tip.isEmpty()) {
                    BlockDisplay e = tip.get(0).entity();
                    if (e != null && e.isValid()) try { e.teleport(embedTip); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < shaft.size(); i++) {
                    double off = -0.6 + (i / (double)(shaft.size() - 1)) * 1.2;
                    Location loc = embed.clone().add(flightDir.getX() * off, flightDir.getY() * off, flightDir.getZ() * off);
                    BlockDisplay e = shaft.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            if (tick == 55) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 6);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronBolt(plugin); }
    }

    // ================================================================
    // #54 — WRECKING BALL ROLL ("The Grind")
    // Iron sphere rolls along the ground, chain trail dragging behind.
    // 30 blocks: 1 core + 8 sphere poles + 6 sphere rim + 10 chain trail + 5 ground rim
    // FOLLOW-AI 0.10 — ball drifts toward player.
    // ================================================================
    public static class WreckingBallRoll extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ballCore = new ArrayList<>();
        private final List<BlockDisplayHandle> ballPoles = new ArrayList<>();
        private final List<BlockDisplayHandle> ballRim = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> groundRim = new ArrayList<>();
        private float rollAngle = 0f;

        public WreckingBallRoll(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_roll", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(360.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(15);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setFollowAiEnabled(true);
            config.setFollowAiWalkSpeed(0.10);
            config.setDesignType("Rolling ball (sprint perpendicular)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double cy = 1.0;
            // 1 core IRON_BLOCK at ball center
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                    center.clone().add(0, cy, 0), Material.IRON_BLOCK);
            core.scale(0f, 0f, 0f).glow(225, 225, 235).interpolation(12, 0);
            spawnedEntities.add(core.entity());
            ballCore.add(core);
            setScale(core.entity(), 0.85f, 0.85f, 0.85f, 14);

            // 8 poles around core (sphere approx)
            double[][] poleOffs = {
                    {0.65, 0, 0}, {-0.65, 0, 0},
                    {0, 0.65, 0}, {0, -0.65, 0},
                    {0, 0, 0.65}, {0, 0, -0.65},
                    {0.45, 0.45, 0}, {-0.45, -0.45, 0}
            };
            for (double[] po : poleOffs) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(po[0], cy + po[1], po[2]), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(12, 1);
                spawnedEntities.add(h.entity());
                ballPoles.add(h);
                setScale(h.entity(), 0.32f, 0.32f, 0.32f, 14);
            }

            // 6 rim segments (netherite, equator)
            for (int i = 0; i < 6; i++) {
                double a = (Math.PI * 2.0 * i) / 6.0;
                Location loc = center.clone().add(Math.cos(a) * 0.7, cy, Math.sin(a) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(12, 2);
                spawnedEntities.add(h.entity());
                ballRim.add(h);
                setScale(h.entity(), 0.24f, 0.24f, 0.24f, 14);
            }

            // 10 chain trail behind (will be teleported as ball rolls)
            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(-0.4 - i * 0.35, 0.4, 0), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, i / 2);
                spawnedEntities.add(h.entity());
                trail.add(h);
                float ts = 0.22f - i * 0.012f;
                setScale(h.entity(), ts, ts, ts, 12);
            }

            // 5 ground rim slabs around ball at ground level (impact ring)
            for (int i = 0; i < 5; i++) {
                double a = (Math.PI * 2.0 * i) / 5.0;
                Location loc = center.clone().add(Math.cos(a) * 1.05, 0.05, Math.sin(a) * 1.05);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                h.scale(0f, 0f, 0f).glow(110, 110, 120).interpolation(10, 3);
                spawnedEntities.add(h.entity());
                groundRim.add(h);
                setScale(h.entity(), 0.6f, 0.05f, 0.6f, 12);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.2f, 0.6f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, cy, 0), 35, 1.2, 0.6, 1.2, 0.04);
        }

        @Override
        protected void onTick(int tick) {
            tickFollowAI();
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 1.0;

            // Rotate ball on Z axis (faces direction of travel — but follow-AI moves center,
            // so just keep rolling visually)
            if (tick > 15) {
                rollAngle += (float) Math.toRadians(9.0);
                if (!ballCore.isEmpty()) {
                    rotateOn(ballCore.get(0).entity(), rollAngle, 0f, 0f, 1f, 4);
                }
                // Rim rotates around equator
                for (int i = 0; i < ballRim.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / ballRim.size() + rollAngle;
                    Location loc = c.clone().add(Math.cos(a) * 0.7, cy, Math.sin(a) * 0.7);
                    BlockDisplay e = ballRim.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Poles also spin around ball center
                double[][] poleOffs = {
                        {0.65, 0, 0}, {-0.65, 0, 0},
                        {0, 0.65, 0}, {0, -0.65, 0},
                        {0, 0, 0.65}, {0, 0, -0.65},
                        {0.45, 0.45, 0}, {-0.45, -0.45, 0}
                };
                for (int i = 0; i < ballPoles.size() && i < poleOffs.length; i++) {
                    double[] po = poleOffs[i];
                    // Rotate around Y axis by rollAngle/2 (slower)
                    double a = rollAngle * 0.5;
                    double nx = po[0] * Math.cos(a) - po[2] * Math.sin(a);
                    double nz = po[0] * Math.sin(a) + po[2] * Math.cos(a);
                    Location loc = c.clone().add(nx, cy + po[1], nz);
                    BlockDisplay e = ballPoles.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            // Trail drags behind on ground
            for (int i = 0; i < trail.size(); i++) {
                double back = -0.4 - i * 0.35;
                Location loc = c.clone().add(back, 0.4, 0);
                BlockDisplay e = trail.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(loc); } catch (Throwable ignored) {}
            }
            // Ground rim follows ball
            for (int i = 0; i < groundRim.size(); i++) {
                double a = (Math.PI * 2.0 * i) / groundRim.size();
                Location loc = c.clone().add(Math.cos(a) * 1.05, 0.05, Math.sin(a) * 1.05);
                BlockDisplay e = groundRim.get(i).entity();
                if (e == null || !e.isValid()) continue;
                try { e.teleport(loc); } catch (Throwable ignored) {}
            }

            // Ground contact sparks
            if (tick > 15 && tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.05, 0), 6,
                        0.8, 0.05, 0.8, 0.08, Material.IRON_BLOCK.createBlockData());
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 0.1, 0), 3, 0.5, 0.1, 0.5, 0.1);
            }
            // Dust trail
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 0.3, 0), 5, 0.7, 0.4, 0.7, 0.03);
            }

            // Rumble sounds
            if (tick > 15 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.55f);
            }
            if (tick > 15 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.6f);
            }

            int duration = config.getDurationTicks();
            if (tick == duration - 20) {
                for (BlockDisplayHandle h : trail) shrinkToZero(h, 14);
                for (BlockDisplayHandle h : groundRim) shrinkToZero(h, 14);
            }
            if (tick == duration - 8) {
                for (BlockDisplayHandle h : ballCore) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : ballPoles) shrinkToZero(h, 6);
                for (BlockDisplayHandle h : ballRim) shrinkToZero(h, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 8, 1.0, 0.6, 1.0, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new WreckingBallRoll(plugin); }
    }

    // ================================================================
    // #55 — CHAIN SPEAR ("The Lance")
    // Thick lance with helical chain wrap. Heavier and slower than javelin.
    // 30 blocks: 5 DARK_OAK_LOG shaft + 8 CHAIN helix + 4 IRON_BLOCK tip
    // + 2 IRON_BLOCK crossguard + 4 IRON_BLOCK fittings + 7 trail
    // ================================================================
    public static class ChainSpear extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> helix = new ArrayList<>();
        private final List<BlockDisplayHandle> tip = new ArrayList<>();
        private final List<BlockDisplayHandle> crossguard = new ArrayList<>();
        private final List<BlockDisplayHandle> fittings = new ArrayList<>();
        private final List<BlockDisplayHandle> trail = new ArrayList<>();
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private Location targetLoc;
        private Vector flightDir;
        private float helixAngle = 0f;

        public ChainSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spear", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(380.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(60);
            config.setCooldownTicks(180);
            config.setChance(7);
            config.setEnabled(true);
            config.setDesignType("Lance thrust (sidestep narrow line)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            Player target = findNearestPlayer(center, 50.0);
            targetLoc = (target != null) ? target.getLocation().clone().add(0, 1.0, 0) : center.clone().add(0, 1, 0);
            origin = center.clone().add(13.0, 4.0, 6.0);

            double dx = targetLoc.getX() - origin.getX();
            double dy = targetLoc.getY() - origin.getY();
            double dz = targetLoc.getZ() - origin.getZ();
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 0.01) dist = 1.0;
            flightDir = new Vector(dx / dist, dy / dist, dz / dist);

            // 5 shaft (DARK_OAK_LOG)
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.DARK_OAK_LOG);
                h.scale(0f, 0f, 0f).glow(120, 90, 60).interpolation(5, i / 2);
                spawnedEntities.add(h.entity());
                shaft.add(h);
                all.add(h);
                setScale(h.entity(), 0.22f, 0.5f, 0.22f, 6);
            }
            // 8 helix chain links (placed around shaft)
            for (int i = 0; i < 8; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(5, i);
                spawnedEntities.add(h.entity());
                helix.add(h);
                all.add(h);
                setScale(h.entity(), 0.14f, 0.14f, 0.14f, 6);
            }
            // 4 IRON_BLOCK tip pieces (wide 4-sided)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(230, 230, 240).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                tip.add(h);
                all.add(h);
                setScale(h.entity(), 0.2f, 0.2f, 0.36f, 6);
            }
            // 2 crossguard IRON_BLOCK (perpendicular bars near rear of tip)
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                crossguard.add(h);
                all.add(h);
                setScale(h.entity(), 0.5f, 0.12f, 0.12f, 6);
            }
            // 4 fittings around grip (IRON_BLOCK rings)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(5, 0);
                spawnedEntities.add(h.entity());
                fittings.add(h);
                all.add(h);
                setScale(h.entity(), 0.16f, 0.08f, 0.16f, 6);
            }
            // 7 chain trail behind
            for (int i = 0; i < 7; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(origin.clone(), Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(5, i);
                spawnedEntities.add(h.entity());
                trail.add(h);
                all.add(h);
                float ts = 0.18f - i * 0.015f;
                setScale(h.entity(), ts, ts, ts, 6);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.1f, 0.8f);
            w.spawnParticle(Particle.LARGE_SMOKE, origin, 28, 0.6, 0.6, 0.6, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (origin == null || targetLoc == null) return;

            int flightStart = 6;
            int flightEnd = 42;
            int impactTick = 44;

            if (tick >= flightStart && tick <= flightEnd) {
                double progress = (tick - flightStart) / (double) (flightEnd - flightStart);
                progress = Math.min(1.0, progress);
                helixAngle += (float) Math.toRadians(20.0);

                double cx = origin.getX() + (targetLoc.getX() - origin.getX()) * progress;
                double cy = origin.getY() + (targetLoc.getY() - origin.getY()) * progress;
                double cz = origin.getZ() + (targetLoc.getZ() - origin.getZ()) * progress;
                Location anchor = new Location(c.getWorld(), cx, cy, cz);

                // Shaft along axis
                for (int i = 0; i < shaft.size(); i++) {
                    double off = -0.6 + (i / (double)(shaft.size() - 1)) * 1.6;
                    Location loc = anchor.clone().add(flightDir.getX() * off, flightDir.getY() * off, flightDir.getZ() * off);
                    BlockDisplay e = shaft.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Helix chain wrap: position each link at corresponding shaft segment + offset
                for (int i = 0; i < helix.size(); i++) {
                    double off = -0.6 + (i / (double)(helix.size() - 1)) * 1.6;
                    double a = helixAngle + i * Math.toRadians(45);
                    double ox = Math.cos(a) * 0.28;
                    double oy = Math.sin(a) * 0.28;
                    Location loc = anchor.clone().add(
                            flightDir.getX() * off + ox,
                            flightDir.getY() * off + oy,
                            flightDir.getZ() * off);
                    BlockDisplay e = helix.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Tip pieces ahead (4 in a + pattern)
                double[][] tipOffs = {{0.1, 0, 0}, {-0.1, 0, 0}, {0, 0.1, 0}, {0, -0.1, 0}};
                for (int i = 0; i < tip.size() && i < tipOffs.length; i++) {
                    Location loc = anchor.clone().add(
                            flightDir.getX() * 1.1 + tipOffs[i][0],
                            flightDir.getY() * 1.1 + tipOffs[i][1],
                            flightDir.getZ() * 1.1 + tipOffs[i][2]);
                    BlockDisplay e = tip.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Crossguard (2) perpendicular near rear of tip
                if (crossguard.size() >= 2) {
                    Location loc1 = anchor.clone().add(flightDir.getX() * 0.6, flightDir.getY() * 0.6 + 0.25, flightDir.getZ() * 0.6);
                    Location loc2 = anchor.clone().add(flightDir.getX() * 0.6, flightDir.getY() * 0.6 - 0.25, flightDir.getZ() * 0.6);
                    BlockDisplay e0 = crossguard.get(0).entity();
                    BlockDisplay e1 = crossguard.get(1).entity();
                    if (e0 != null && e0.isValid()) try { e0.teleport(loc1); } catch (Throwable ignored) {}
                    if (e1 != null && e1.isValid()) try { e1.teleport(loc2); } catch (Throwable ignored) {}
                }
                // Fittings around grip
                for (int i = 0; i < fittings.size(); i++) {
                    double off = -0.5 + i * 0.25;
                    Location loc = anchor.clone().add(flightDir.getX() * off, flightDir.getY() * off, flightDir.getZ() * off);
                    BlockDisplay e = fittings.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Trail behind
                for (int i = 0; i < trail.size(); i++) {
                    double back = -1.4 - i * 0.4;
                    Location loc = anchor.clone().add(flightDir.getX() * back, flightDir.getY() * back, flightDir.getZ() * back);
                    BlockDisplay e = trail.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }

                if (tick % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.LARGE_SMOKE, anchor, 4, 0.4, 0.3, 0.4, 0.04);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, anchor, 2, 0.3, 0.3, 0.3, 0.05);
                }
            }

            if (tick > flightStart && tick < flightEnd && tick % 4 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.9f);
            }

            if (tick == impactTick) {
                triggerImpactDamage(targetLoc);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.6f);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.3f, 0.7f);
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_CHAIN_HIT, 1.2f, 0.9f);
                c.getWorld().spawnParticle(Particle.CRIT, targetLoc, 22, 1.0, 0.5, 1.0, 0.25);
                c.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 6, 0.6, 0.4, 0.6, 0.05);
            }

            if (tick == 52) {
                for (BlockDisplayHandle h : all) shrinkToZero(h, 8);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainSpear(plugin); }
    }

    // ================================================================
    // #56 — NETHERITE REINFORCED CAGE ("The Lockdown")
    // Top-down assembled cage: ceiling chains -> corner posts -> bars -> braces -> lock
    // 36 blocks: 4 NETHERITE corner posts + 8 IRON horiz bars + 4 CHAIN diagonal braces
    // + 1 NETHERITE lock plate + 4 IRON floor grating + 4 IRON top cap + 4 CHAIN ceiling
    // + 7 telegraph DUST anchors (visual only, IRON_BLOCK micro-pieces)
    // ================================================================
    public static class NetheriteCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ceilingChains = new ArrayList<>();
        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> bars = new ArrayList<>();
        private final List<BlockDisplayHandle> braces = new ArrayList<>();
        private final List<BlockDisplayHandle> lock = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> topCap = new ArrayList<>();
        private final List<BlockDisplayHandle> telegraph = new ArrayList<>();

        public NetheriteCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("netherite_cage", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(280.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(260);
            config.setCooldownTicks(300);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Lockdown enclosure (escape before lock)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double r = 2.3;
            double h = 2.5;

            // 7 telegraph ground markers (IRON_BLOCK micro slabs at ground showing footprint)
            for (int i = 0; i < 7; i++) {
                double a = (Math.PI * 2.0 * i) / 7.0;
                Location loc = center.clone().add(Math.cos(a) * r * 1.05, 0.04, Math.sin(a) * r * 1.05);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                bd.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(8, 0);
                spawnedEntities.add(bd.entity());
                telegraph.add(bd);
                setScale(bd.entity(), 0.5f, 0.04f, 0.5f, 10);
            }

            // 4 ceiling chains hanging from above (top first)
            double[][] cornerOffs = {{r, 0, r}, {-r, 0, r}, {r, 0, -r}, {-r, 0, -r}};
            for (int i = 0; i < 4; i++) {
                double[] co = cornerOffs[i];
                BlockDisplayHandle cc = displayBuilder.spawnBlock(
                        center.clone().add(co[0], h + 2.5, co[2]), Material.CHAIN);
                cc.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(10, i);
                spawnedEntities.add(cc.entity());
                ceilingChains.add(cc);
                setScale(cc.entity(), 0.18f, 2.0f, 0.18f, 12);
            }

            // 4 corner posts (NETHERITE) — spawned high, descend in onTick
            for (int i = 0; i < 4; i++) {
                double[] co = cornerOffs[i];
                BlockDisplayHandle bd = displayBuilder.spawnBlock(
                        center.clone().add(co[0], h + 3.0, co[2]), Material.NETHERITE_BLOCK);
                bd.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(10, 4);
                spawnedEntities.add(bd.entity());
                corners.add(bd);
                setScale(bd.entity(), 0.24f, 2.5f, 0.24f, 12);
            }

            // 8 horizontal bars (IRON) — start outside, slide in on onTick
            // 4 walls × 2 heights (0.7 and 1.8)
            double[][] wallMids = {{0, 0, r}, {0, 0, -r}, {r, 0, 0}, {-r, 0, 0}};
            double[] barYs = {0.7, 1.8};
            for (int wi = 0; wi < 4; wi++) {
                double[] wm = wallMids[wi];
                boolean alongX = Math.abs(wm[2]) > 0.01;
                for (int hi = 0; hi < 2; hi++) {
                    Location offLoc = center.clone().add(wm[0] * 3.0, barYs[hi], wm[2] * 3.0);
                    BlockDisplayHandle bd = displayBuilder.spawnBlock(offLoc, Material.IRON_BLOCK);
                    bd.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 10);
                    spawnedEntities.add(bd.entity());
                    bars.add(bd);
                    if (alongX) {
                        setScale(bd.entity(), 2.0f, 0.13f, 0.13f, 12);
                    } else {
                        setScale(bd.entity(), 0.13f, 0.13f, 2.0f, 12);
                    }
                }
            }

            // 4 diagonal braces (CHAIN) — one per wall (initially scale 0)
            for (int i = 0; i < 4; i++) {
                double[] wm = wallMids[i];
                Location loc = center.clone().add(wm[0], 1.25, wm[2]);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, 15);
                spawnedEntities.add(bd.entity());
                braces.add(bd);
                // Rotate brace 45 deg on the wall plane
                boolean alongX = Math.abs(wm[2]) > 0.01;
                if (alongX) {
                    setScale(bd.entity(), 2.4f, 0.04f, 0.1f, 12);
                } else {
                    setScale(bd.entity(), 0.1f, 0.04f, 2.4f, 12);
                }
            }

            // 1 lock plate (NETHERITE) on the +Z face
            BlockDisplayHandle lk = displayBuilder.spawnBlock(
                    center.clone().add(0, 1.25, r * 1.02), Material.NETHERITE_BLOCK);
            lk.scale(0f, 0f, 0f).glow(80, 80, 90).interpolation(10, 18);
            spawnedEntities.add(lk.entity());
            lock.add(lk);
            setScale(lk.entity(), 0.35f, 0.3f, 0.15f, 12);

            // 4 IRON floor grating slabs
            double[][] floorOffs = {{r/2, 0, r/2}, {-r/2, 0, r/2}, {r/2, 0, -r/2}, {-r/2, 0, -r/2}};
            for (double[] fo : floorOffs) {
                BlockDisplayHandle bd = displayBuilder.spawnBlock(
                        center.clone().add(fo[0], 0.06, fo[2]), Material.IRON_BLOCK);
                bd.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 20);
                spawnedEntities.add(bd.entity());
                floor.add(bd);
                setScale(bd.entity(), 0.95f, 0.05f, 0.95f, 12);
            }

            // 4 IRON top cap pieces
            for (int i = 0; i < 4; i++) {
                double[] wm = wallMids[i];
                Location loc = center.clone().add(wm[0], h + 0.05, wm[2]);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                bd.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 22);
                spawnedEntities.add(bd.entity());
                topCap.add(bd);
                boolean alongX = Math.abs(wm[2]) > 0.01;
                if (alongX) {
                    setScale(bd.entity(), 2.0f, 0.1f, 0.1f, 12);
                } else {
                    setScale(bd.entity(), 0.1f, 0.1f, 2.0f, 12);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.5f, 0.5f);
            // Telegraph DUST ring on the ground showing footprint
            Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(200, 40, 20), 1.6f);
            for (int i = 0; i < 36; i++) {
                double a = (Math.PI * 2.0 * i) / 36.0;
                Location p = center.clone().add(Math.cos(a) * r * 1.05, 0.05, Math.sin(a) * r * 1.05);
                w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double r = 2.3;
            double h = 2.5;
            double[][] cornerOffs = {{r, 0, r}, {-r, 0, r}, {r, 0, -r}, {-r, 0, -r}};
            double[][] wallMids = {{0, 0, r}, {0, 0, -r}, {r, 0, 0}, {-r, 0, 0}};

            // Telegraph DUST ring on the ground every 4 ticks until lock
            if (tick <= 30 && tick % 4 == 0) {
                Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(220, 50, 30), 1.4f);
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2.0 * i) / 24.0;
                    Location p = c.clone().add(Math.cos(a) * r * 1.05, 0.05, Math.sin(a) * r * 1.05);
                    c.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                }
            }

            // Phase 1 — corners descend (ticks 5-25)
            if (tick >= 5 && tick <= 25) {
                double p = (tick - 5) / 20.0;
                for (int i = 0; i < corners.size(); i++) {
                    double[] co = cornerOffs[i];
                    double y = (h + 3.0) * (1 - p) + 0.0 * p;
                    BlockDisplay e = corners.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(co[0], y, co[2])); } catch (Throwable ignored) {}
                }
                // Ceiling chains follow corner posts down
                for (int i = 0; i < ceilingChains.size(); i++) {
                    double[] co = cornerOffs[i];
                    double y = (h + 2.5) * (1 - p * 0.6) + (h + 0.5) * (p * 0.6);
                    BlockDisplay e = ceilingChains.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(c.clone().add(co[0], y, co[2])); } catch (Throwable ignored) {}
                }
            }
            if (tick == 25) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
                for (int i = 0; i < 4; i++) {
                    double[] co = cornerOffs[i];
                    DisplayBuilder.playSound(c.clone().add(co[0], 0, co[2]), Sound.BLOCK_CHAIN_PLACE, 1.2f, 0.7f);
                }
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.1, 0), 20,
                        r, 0.1, r, 0.05, Material.NETHERITE_BLOCK.createBlockData());
            }

            // Phase 2 — horizontal bars slide in (ticks 28-45)
            if (tick >= 28 && tick <= 45) {
                double p = (tick - 28) / 17.0;
                double[] barYs = {0.7, 1.8};
                for (int wi = 0; wi < 4; wi++) {
                    double[] wm = wallMids[wi];
                    for (int hi = 0; hi < 2; hi++) {
                        int idx = wi * 2 + hi;
                        if (idx >= bars.size()) break;
                        double mult = (1.0 - p) * 3.0 + p * 1.0;
                        Location loc = c.clone().add(wm[0] * mult, barYs[hi], wm[2] * mult);
                        BlockDisplay e = bars.get(idx).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(loc); } catch (Throwable ignored) {}
                    }
                }
            }
            if (tick == 45) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.3f, 0.9f);
            }

            // Phase 3 — braces and lock materialize via DUST sparks (ticks 50-65)
            if (tick == 55) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_PLACE, 1.4f, 0.7f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1.25, r), 25, 0.5, 0.4, 0.2, 0.15);
            }

            // Lock fully sealed at tick 60
            if (tick == 60) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
            }

            // Maintenance: lock plate sparks during lockdown
            if (tick > 60 && tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 1.25, r * 1.02), 4, 0.2, 0.2, 0.1, 0.1);
            }
            if (tick > 60 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
            }

            int duration = config.getDurationTicks();
            // Dissipate (last 30 ticks): unlock → bars retract → corners rise
            if (tick == duration - 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.3f, 0.7f);
                for (BlockDisplayHandle bd : lock) shrinkToZero(bd, 10);
                for (BlockDisplayHandle bd : braces) shrinkToZero(bd, 12);
            }
            if (tick == duration - 20) {
                for (BlockDisplayHandle bd : bars) shrinkToZero(bd, 12);
                for (BlockDisplayHandle bd : topCap) shrinkToZero(bd, 12);
            }
            if (tick == duration - 10) {
                for (BlockDisplayHandle bd : corners) shrinkToZero(bd, 8);
                for (BlockDisplayHandle bd : ceilingChains) shrinkToZero(bd, 8);
                for (BlockDisplayHandle bd : floor) shrinkToZero(bd, 8);
                for (BlockDisplayHandle bd : telegraph) shrinkToZero(bd, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.5f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 1.0, 0), 30, r, 1.0, r, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new NetheriteCage(plugin); }
    }

    // ================================================================
    // #57 — CHAIN DOME ("The Bell Jar")
    // Bell-curve dome descends from above. Players must escape before it seats.
    // 30 blocks: 5 rings (8/10/12/10/4 = 44 too many) — adjusted to 5/7/9/7/3 = 31
    // + 4 vertical rib columns of CHAIN at cardinal positions (but rings already
    // give 31). Actually: 5+7+9+7+3 = 31 ring chains; we add 4 rib CHAIN columns
    // (rib columns are conceptual — already represented by rings).
    // Final: 5+7+9+7+3 ring CHAINs (31) — plenty.
    // ================================================================
    public static class ChainDome extends BlockDisplayAttack {
        private final List<List<BlockDisplayHandle>> rings = new ArrayList<>();
        private final List<BlockDisplayHandle> floorAnchor = new ArrayList<>();
        private final double[] ringHeights = {0.3, 0.9, 1.8, 2.7, 3.3};
        private final double[] ringRadii = {2.2, 2.8, 3.0, 2.3, 0.8};
        private final int[] ringCounts = {5, 7, 9, 7, 3};

        public ChainDome(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_dome", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(4.5);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Dome enclosure (find dome edge gap)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double startY = 6.0;
            // 5 rings — spawn high (Y + startY) then descend
            for (int ri = 0; ri < ringHeights.length; ri++) {
                List<BlockDisplayHandle> ring = new ArrayList<>();
                int count = ringCounts[ri];
                double radius = ringRadii[ri];
                for (int i = 0; i < count; i++) {
                    double a = (Math.PI * 2.0 * i) / count;
                    Location loc = center.clone().add(
                            Math.cos(a) * radius, startY + ringHeights[ri], Math.sin(a) * radius);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, ri * 2 + i / 2);
                    spawnedEntities.add(h.entity());
                    ring.add(h);
                    setScale(h.entity(), 0.22f, 0.22f, 0.45f, 12);
                }
                rings.add(ring);
            }

            // 12 IRON_BLOCK floor anchor in a ring at the dome base (decorative footprint)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2.0 * i) / 12.0;
                Location loc = center.clone().add(Math.cos(a) * 3.1, 0.05, Math.sin(a) * 3.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(10, 0);
                spawnedEntities.add(h.entity());
                floorAnchor.add(h);
                setScale(h.entity(), 0.5f, 0.04f, 0.5f, 12);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.5f);
            // Telegraph DUST ring on the ground showing dome footprint
            Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(180, 90, 30), 1.5f);
            for (int i = 0; i < 32; i++) {
                double a = (Math.PI * 2.0 * i) / 32.0;
                Location p = center.clone().add(Math.cos(a) * 3.0, 0.05, Math.sin(a) * 3.0);
                w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Telegraph DUST until tick 30
            if (tick <= 30 && tick % 5 == 0) {
                Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(220, 110, 40), 1.4f);
                for (int i = 0; i < 28; i++) {
                    double a = (Math.PI * 2.0 * i) / 28.0;
                    Location p = c.clone().add(Math.cos(a) * 3.0, 0.05, Math.sin(a) * 3.0);
                    c.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                }
            }

            // Phase 1 — descend from Y+6 to Y=0 (ticks 5-35)
            if (tick >= 5 && tick <= 35) {
                double progress = (tick - 5) / 30.0;
                double yOffset = 6.0 * (1.0 - progress);
                for (int ri = 0; ri < rings.size(); ri++) {
                    List<BlockDisplayHandle> ring = rings.get(ri);
                    int count = ringCounts[ri];
                    double radius = ringRadii[ri];
                    for (int i = 0; i < ring.size(); i++) {
                        double a = (Math.PI * 2.0 * i) / count;
                        Location loc = c.clone().add(
                                Math.cos(a) * radius, yOffset + ringHeights[ri], Math.sin(a) * radius);
                        BlockDisplay e = ring.get(i).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(loc); } catch (Throwable ignored) {}
                    }
                }
            }
            if (tick == 35) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.5f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.7f);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.05, 0), 30,
                        3.0, 0.1, 3.0, 0.05, Material.CHAIN.createBlockData());
            }

            // Phase 2 — locked: sway slightly (outer rings oscillate)
            if (tick > 35 && tick % 4 == 0) {
                double sway = Math.sin(tick * 0.08) * 0.06;
                // Outer ring (index 2: r=3.0, 9 chains) sways
                List<BlockDisplayHandle> outer = rings.get(2);
                int count = ringCounts[2];
                double radius = ringRadii[2];
                for (int i = 0; i < outer.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / count;
                    Location loc = c.clone().add(
                            Math.cos(a) * radius + sway, ringHeights[2], Math.sin(a) * radius + sway);
                    BlockDisplay e = outer.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            // Continuous chain ambience inside dome
            if (tick > 35 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.5f);
            }
            if (tick > 35 && tick % 30 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 1.5, 0), 8, 1.0, 1.0, 1.0, 0.02);
            }

            int duration = config.getDurationTicks();
            // Dissipate — dome lifts back to Y+6 then vanishes
            if (tick >= duration - 25 && tick <= duration - 5) {
                double progress = (tick - (duration - 25)) / 20.0;
                double yOffset = 6.0 * progress;
                for (int ri = 0; ri < rings.size(); ri++) {
                    List<BlockDisplayHandle> ring = rings.get(ri);
                    int count = ringCounts[ri];
                    double radius = ringRadii[ri];
                    for (int i = 0; i < ring.size(); i++) {
                        double a = (Math.PI * 2.0 * i) / count;
                        Location loc = c.clone().add(
                                Math.cos(a) * radius, yOffset + ringHeights[ri], Math.sin(a) * radius);
                        BlockDisplay e = ring.get(i).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(loc); } catch (Throwable ignored) {}
                    }
                }
            }
            if (tick == duration - 5) {
                for (List<BlockDisplayHandle> ring : rings) {
                    for (BlockDisplayHandle bd : ring) shrinkToZero(bd, 6);
                }
                for (BlockDisplayHandle bd : floorAnchor) shrinkToZero(bd, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.3f, 0.6f);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, 2.0, 0), 25, 2.5, 1.5, 2.5, 0.04);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainDome(plugin); }
    }

    // ================================================================
    // #58 — IRON CUBE CRUSH ("The Press")
    // 4 walls + ceiling cube descending. Players escape vertically/perimeter
    // before walls and ceiling crush.
    // 32 blocks: 4 walls × 3 blocks = 12 + 4 NETHERITE corner posts + 4 CHAIN
    // connection links + 9 ceiling cube blocks + 3 floor accent
    // ================================================================
    public static class IronCubeCrush extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> walls = new ArrayList<>();
        private final List<BlockDisplayHandle> corners = new ArrayList<>();
        private final List<BlockDisplayHandle> connectors = new ArrayList<>();
        private final List<BlockDisplayHandle> ceiling = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private double wallRadius = 2.5;
        private double ceilingY = 8.0;

        public IronCubeCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_cube_crush", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(380.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(180);
            config.setCooldownTicks(260);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Press enclosure (escape vertical compression)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double r = 2.5;
            double cy = 1.0;

            // 4 walls, 3 segments each — directions N/S/E/W (at outer radius)
            double[][] dirs = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};
            for (int wi = 0; wi < 4; wi++) {
                double[] d = dirs[wi];
                boolean alongX = Math.abs(d[2]) > 0.01; // wall faces Z
                for (int s = 0; s < 3; s++) {
                    double offset = -0.95 + s * 0.95;
                    Location loc;
                    if (alongX) {
                        loc = center.clone().add(offset, cy, d[2] * r);
                    } else {
                        loc = center.clone().add(d[0] * r, cy, offset);
                    }
                    BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    bd.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(10, s);
                    spawnedEntities.add(bd.entity());
                    walls.add(bd);
                    if (alongX) {
                        setScale(bd.entity(), 0.95f, 1.5f, 0.2f, 12);
                    } else {
                        setScale(bd.entity(), 0.2f, 1.5f, 0.95f, 12);
                    }
                }
            }

            // 4 NETHERITE corner posts at outer radius (stationary visual anchor)
            double[][] corns = {{r, 0, r}, {-r, 0, r}, {r, 0, -r}, {-r, 0, -r}};
            for (double[] co : corns) {
                BlockDisplayHandle bd = displayBuilder.spawnBlock(
                        center.clone().add(co[0], cy, co[2]), Material.NETHERITE_BLOCK);
                bd.scale(0f, 0f, 0f).glow(70, 70, 80).interpolation(10, 5);
                spawnedEntities.add(bd.entity());
                corners.add(bd);
                setScale(bd.entity(), 0.28f, 1.5f, 0.28f, 12);
            }

            // 4 CHAIN connectors (one per wall to its corner)
            for (int wi = 0; wi < 4; wi++) {
                double[] d = dirs[wi];
                Location loc = center.clone().add(d[0] * r * 0.7, cy, d[2] * r * 0.7);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(10, 7);
                spawnedEntities.add(bd.entity());
                connectors.add(bd);
                setScale(bd.entity(), 0.14f, 0.14f, 0.6f, 12);
            }

            // 9 ceiling cube blocks (3x3 NETHERITE slab, descends Y +8 -> +2)
            for (int i = 0; i < 9; i++) {
                int row = i / 3;
                int col = i % 3;
                Location loc = center.clone().add(-1.0 + col * 1.0, ceilingY, -1.0 + row * 1.0);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                bd.scale(0f, 0f, 0f).glow(60, 60, 70).interpolation(10, 10);
                spawnedEntities.add(bd.entity());
                ceiling.add(bd);
                setScale(bd.entity(), 0.95f, 0.95f, 0.95f, 12);
            }

            // 3 IRON floor accents at center
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(-1.0 + i * 1.0, 0.04, 0);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                bd.scale(0f, 0f, 0f).glow(210, 210, 220).interpolation(10, 0);
                spawnedEntities.add(bd.entity());
                floor.add(bd);
                setScale(bd.entity(), 0.95f, 0.05f, 0.95f, 12);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);
            // Telegraph DUST: vertical lines at each wall's path
            Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(230, 30, 30), 1.6f);
            for (double[] d : dirs) {
                for (int yi = 0; yi < 10; yi++) {
                    Location p = center.clone().add(d[0] * r, 0.2 + yi * 0.3, d[2] * r);
                    w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                }
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double cy = 1.0;
            double startR = 2.5;
            double finalR = 0.6;
            double startCeil = 8.0;
            double finalCeil = 2.0;

            // Telegraph DUST until tick 25
            if (tick <= 25 && tick % 4 == 0) {
                Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(240, 50, 30), 1.4f);
                double[][] dirs = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};
                for (double[] d : dirs) {
                    for (int yi = 0; yi < 8; yi++) {
                        Location p = c.clone().add(d[0] * startR, 0.2 + yi * 0.35, d[2] * startR);
                        c.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                    }
                }
            }

            // Press phase ticks 30-140 (110-tick compression)
            if (tick >= 30 && tick <= 140) {
                double progress = (tick - 30) / 110.0;
                progress = Math.min(1.0, progress);
                wallRadius = startR + (finalR - startR) * progress;
                ceilingY = startCeil + (finalCeil - startCeil) * progress;

                double[][] dirs = {{0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0}};

                // Walls slide inward
                for (int wi = 0; wi < 4; wi++) {
                    double[] d = dirs[wi];
                    boolean alongX = Math.abs(d[2]) > 0.01;
                    for (int s = 0; s < 3; s++) {
                        int idx = wi * 3 + s;
                        if (idx >= walls.size()) break;
                        double offset = -0.95 + s * 0.95;
                        Location loc;
                        if (alongX) {
                            loc = c.clone().add(offset, cy, d[2] * wallRadius);
                        } else {
                            loc = c.clone().add(d[0] * wallRadius, cy, offset);
                        }
                        BlockDisplay e = walls.get(idx).entity();
                        if (e == null || !e.isValid()) continue;
                        try { e.teleport(loc); } catch (Throwable ignored) {}
                    }
                }
                // Connectors (chain) stretches from corner to wall midpoint
                for (int wi = 0; wi < 4; wi++) {
                    double[] d = dirs[wi];
                    double mid = (startR + wallRadius) * 0.5;
                    Location loc = c.clone().add(d[0] * mid, cy, d[2] * mid);
                    BlockDisplay e = connectors.get(wi).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                    // Stretch length
                    float len = (float) (startR - wallRadius + 0.4);
                    if (Math.abs(d[2]) > 0.01) {
                        setScale(e, 0.14f, 0.14f, Math.max(0.2f, len), 6);
                    } else {
                        setScale(e, Math.max(0.2f, len), 0.14f, 0.14f, 6);
                    }
                }

                // Ceiling cube descends
                for (int i = 0; i < ceiling.size(); i++) {
                    int row = i / 3;
                    int col = i % 3;
                    Location loc = c.clone().add(-1.0 + col * 1.0, ceilingY, -1.0 + row * 1.0);
                    BlockDisplay e = ceiling.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            // Mechanical clank during press
            if (tick >= 30 && tick <= 140 && tick % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.0f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_HIT, 0.7f, 0.5f);
            }
            // Compression sparks
            if (tick >= 30 && tick <= 140 && tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, cy, 0), 8,
                        wallRadius, 0.5, wallRadius, 0.04, Material.IRON_BLOCK.createBlockData());
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, cy + 0.5, 0), 4, 0.8, 0.4, 0.8, 0.1);
            }

            // Final crush impact at tick 140
            if (tick == 140) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.5f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, cy, 0), 12, 1.0, 0.8, 1.0, 0.05);
                c.getWorld().spawnParticle(Particle.LARGE_SMOKE, c.clone().add(0, cy, 0), 40, 1.5, 1.0, 1.5, 0.06);
            }

            int duration = config.getDurationTicks();
            // Dissipate — walls scale to 0
            if (tick == duration - 25) {
                for (BlockDisplayHandle bd : walls) shrinkToZero(bd, 14);
                for (BlockDisplayHandle bd : connectors) shrinkToZero(bd, 14);
            }
            if (tick == duration - 10) {
                for (BlockDisplayHandle bd : ceiling) shrinkToZero(bd, 8);
                for (BlockDisplayHandle bd : corners) shrinkToZero(bd, 8);
                for (BlockDisplayHandle bd : floor) shrinkToZero(bd, 8);
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IronCubeCrush(plugin); }
    }

    // ================================================================
    // #59 — BINDING SPIRAL ("The Coil")
    // Tightening spiral of chains draws inward from outer perimeter.
    // 30 blocks: 20 CHAIN spiral links + 10 IRON_BLOCK knot accents
    // ================================================================
    public static class BindingSpiral extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spiral = new ArrayList<>();
        private final List<BlockDisplayHandle> knots = new ArrayList<>();
        private float rotateAngle = 0f;
        private final double rMax = 3.5;
        private final double rMin = 0.4;
        private final int turns = 3;
        private final int linkCount = 20;

        public BindingSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("binding_spiral", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(300.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(260);
            config.setChance(6);
            config.setEnabled(true);
            config.setDesignType("Spiral enclosure (escape outward)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // 20 CHAIN spiral links — spawn at scale 0 (will materialize over time outer->inner)
            for (int i = 0; i < linkCount; i++) {
                double t = (i / (double)(linkCount - 1)) * (2.0 * Math.PI * turns);
                double r = rMax - (rMax - rMin) * (i / (double)(linkCount - 1));
                Location loc = center.clone().add(Math.cos(t) * r, 0.06, Math.sin(t) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                h.scale(0f, 0f, 0f).glow(190, 190, 205).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                spiral.add(h);
            }

            // 10 IRON_BLOCK knot accents at every other link
            for (int i = 0; i < 10; i++) {
                int idx = i * 2; // every other link
                if (idx >= linkCount) idx = linkCount - 1;
                double t = (idx / (double)(linkCount - 1)) * (2.0 * Math.PI * turns);
                double r = rMax - (rMax - rMin) * (idx / (double)(linkCount - 1));
                Location loc = center.clone().add(Math.cos(t) * r, 0.1, Math.sin(t) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0f, 0f, 0f).glow(220, 220, 230).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                knots.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            // Telegraph DUST showing outer radius
            Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(160, 30, 90), 1.5f);
            for (int i = 0; i < 36; i++) {
                double a = (Math.PI * 2.0 * i) / 36.0;
                Location p = center.clone().add(Math.cos(a) * rMax, 0.05, Math.sin(a) * rMax);
                w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Telegraph until tick 20
            if (tick <= 20 && tick % 4 == 0) {
                Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(180, 50, 110), 1.4f);
                for (int i = 0; i < 28; i++) {
                    double a = (Math.PI * 2.0 * i) / 28.0;
                    Location p = c.clone().add(Math.cos(a) * rMax, 0.05, Math.sin(a) * rMax);
                    c.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                }
            }

            // Phase 1 — spiral draws outer-to-inner, 3 ticks per link from tick 5
            if (tick >= 5 && tick <= 5 + spiral.size() * 3) {
                int idx = (tick - 5) / 3;
                if (idx < spiral.size()) {
                    setScale(spiral.get(idx).entity(), 0.22f, 0.22f, 0.55f, 6);
                    if (idx < knots.size()) {
                        setScale(knots.get(idx).entity(), 0.16f, 0.14f, 0.16f, 6);
                    }
                    DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.7f + idx * 0.02f);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            spiral.get(idx).entity().getLocation(), 3, 0.2, 0.2, 0.2, 0.08);
                }
            }

            // Phase 2 — rotate spiral (inner faster than outer)
            int drawEnd = 5 + spiral.size() * 3;
            if (tick > drawEnd) {
                rotateAngle += (float) Math.toRadians(2.5);
                for (int i = 0; i < spiral.size(); i++) {
                    // Inner = faster (4 deg/tick), outer = slower (1.5 deg/tick)
                    double fraction = i / (double)(spiral.size() - 1);
                    double localRate = 4.0 - fraction * 2.5; // deg per tick: 4 inner -> 1.5 outer
                    // Compute current absolute angle accumulator
                    double baseT = (i / (double)(spiral.size() - 1)) * (2.0 * Math.PI * turns);
                    double dynamic = baseT + Math.toRadians(localRate) * tick;
                    double r = rMax - (rMax - rMin) * fraction;
                    Location loc = c.clone().add(Math.cos(dynamic) * r, 0.06, Math.sin(dynamic) * r);
                    BlockDisplay e = spiral.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                // Update knots
                for (int i = 0; i < knots.size(); i++) {
                    int idx = i * 2;
                    if (idx >= linkCount) idx = linkCount - 1;
                    double fraction = idx / (double)(linkCount - 1);
                    double localRate = 4.0 - fraction * 2.5;
                    double baseT = fraction * (2.0 * Math.PI * turns);
                    double dynamic = baseT + Math.toRadians(localRate) * tick;
                    double r = rMax - (rMax - rMin) * fraction;
                    Location loc = c.clone().add(Math.cos(dynamic) * r, 0.1, Math.sin(dynamic) * r);
                    BlockDisplay e = knots.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            // Center sparks during active phase
            if (tick > drawEnd && tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 0.1, 0), 4, 0.3, 0.1, 0.3, 0.1);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.1, 0), 4,
                        0.3, 0.1, 0.3, 0.05, Material.CHAIN.createBlockData());
            }
            if (tick > drawEnd && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 0.6f);
            }

            int duration = config.getDurationTicks();
            if (tick == duration - 18) {
                // Outer half shrinks first
                int half = spiral.size() / 2;
                for (int i = 0; i < half; i++) shrinkToZero(spiral.get(i), 10);
                for (int i = 0; i < knots.size() / 2; i++) shrinkToZero(knots.get(i), 10);
            }
            if (tick == duration - 8) {
                int half = spiral.size() / 2;
                for (int i = half; i < spiral.size(); i++) shrinkToZero(spiral.get(i), 6);
                for (int i = knots.size() / 2; i < knots.size(); i++) shrinkToZero(knots.get(i), 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.3f, 0.6f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.2, 0), 6, 0.5, 0.2, 0.5, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new BindingSpiral(plugin); }
    }

    // ================================================================
    // #60 — CHAIN MANDALA ("The Sigil")
    // Concentric mandala pattern burned into ground. 3 rings + 8 spokes + 8 nodes.
    // 32 blocks: 8 inner CHAIN (r=1.0) + 12 middle CHAIN (r=2.0) + 16 outer (subset 4 to fit) — actually:
    // Per spec: inner 8 + middle 12 + outer 16 = 36 chain ring + 8 spokes + 8 nodes = 52 — too many.
    // Reduced to: inner 6 + middle 9 + outer 12 = 27 + 8 spokes (chain slabs) + 8 nodes = 43, OK.
    // We'll do: inner 6 + middle 9 + outer 12 = 27 + 5 spokes = 32 (drop 3 spokes to fit 32).
    // Actually simpler: 6 + 8 + 10 = 24 ring chains, 8 spokes, 8 nodes = 40. We want >=30, this is fine.
    // Final: inner 6 + middle 8 + outer 10 + 4 spokes + 4 nodes = 32 blocks.
    // ================================================================
    public static class ChainMandala extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> nodes = new ArrayList<>();
        private float spinAngle = 0f;
        private final double innerR = 1.0;
        private final double middleR = 2.4;
        private final double outerR = 3.8;
        private final int innerCount = 6;
        private final int middleCount = 10;
        private final int outerCount = 14;
        private final int spokeCount = 6;
        private final int nodeCount = 6;

        public ChainMandala(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_mandala", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setChance(5);
            config.setEnabled(true);
            config.setDesignType("Sigil-pattern (read pattern, pick safe sector)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Inner ring (6 CHAIN, r=1.0)
            for (int i = 0; i < innerCount; i++) {
                double a = (Math.PI * 2.0 * i) / innerCount;
                Location loc = center.clone().add(Math.cos(a) * innerR, 0.04, Math.sin(a) * innerR);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(200, 200, 215).interpolation(8, i);
                spawnedEntities.add(bd.entity());
                innerRing.add(bd);
            }
            // Middle ring (10 CHAIN, r=2.4)
            for (int i = 0; i < middleCount; i++) {
                double a = (Math.PI * 2.0 * i) / middleCount;
                Location loc = center.clone().add(Math.cos(a) * middleR, 0.04, Math.sin(a) * middleR);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(190, 190, 200).interpolation(8, i);
                spawnedEntities.add(bd.entity());
                middleRing.add(bd);
            }
            // Outer ring (14 CHAIN, r=3.8)
            for (int i = 0; i < outerCount; i++) {
                double a = (Math.PI * 2.0 * i) / outerCount;
                Location loc = center.clone().add(Math.cos(a) * outerR, 0.04, Math.sin(a) * outerR);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(180, 180, 195).interpolation(8, i);
                spawnedEntities.add(bd.entity());
                outerRing.add(bd);
            }
            // 6 spokes (radial CHAIN slabs from inner to outer)
            for (int i = 0; i < spokeCount; i++) {
                double a = (Math.PI * 2.0 * i) / spokeCount;
                double midR = (innerR + outerR) * 0.5;
                Location loc = center.clone().add(Math.cos(a) * midR, 0.05, Math.sin(a) * midR);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.CHAIN);
                bd.scale(0f, 0f, 0f).glow(170, 170, 185).interpolation(8, 0);
                spawnedEntities.add(bd.entity());
                spokes.add(bd);
            }
            // 6 IRON nodes at outer ring intersections
            for (int i = 0; i < nodeCount; i++) {
                double a = (Math.PI * 2.0 * i) / nodeCount;
                Location loc = center.clone().add(Math.cos(a) * outerR, 0.08, Math.sin(a) * outerR);
                BlockDisplayHandle bd = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                bd.scale(0f, 0f, 0f).glow(230, 230, 240).interpolation(8, 0);
                spawnedEntities.add(bd.entity());
                nodes.add(bd);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            // Telegraph DUST: spiral burst from center showing footprint
            Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(220, 170, 40), 1.5f);
            for (int i = 0; i < 36; i++) {
                double a = (Math.PI * 2.0 * i) / 36.0;
                Location p = center.clone().add(Math.cos(a) * outerR, 0.05, Math.sin(a) * outerR);
                w.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
            }
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Telegraph DUST until tick 25
            if (tick <= 25 && tick % 4 == 0) {
                Particle.DustOptions warn = new Particle.DustOptions(Color.fromRGB(240, 190, 50), 1.4f);
                for (int i = 0; i < 28; i++) {
                    double a = (Math.PI * 2.0 * i) / 28.0;
                    Location p = c.clone().add(Math.cos(a) * outerR, 0.05, Math.sin(a) * outerR);
                    c.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, warn);
                }
            }

            // 5-phase drawing: inner (5-15), spokes (15-25), middle (25-35), outer (35-45), nodes (45-55)
            if (tick == 5) {
                for (BlockDisplayHandle bd : innerRing) {
                    setScale(bd.entity(), 0.24f, 0.05f, 0.5f, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
            }
            if (tick == 15) {
                for (BlockDisplayHandle bd : spokes) {
                    setScale(bd.entity(), 0.1f, 0.02f, 1.4f, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.85f);
            }
            if (tick == 25) {
                for (BlockDisplayHandle bd : middleRing) {
                    setScale(bd.entity(), 0.22f, 0.05f, 0.5f, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.9f);
            }
            if (tick == 35) {
                for (BlockDisplayHandle bd : outerRing) {
                    setScale(bd.entity(), 0.22f, 0.05f, 0.5f, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.95f);
            }
            if (tick == 45) {
                for (BlockDisplayHandle bd : nodes) {
                    setScale(bd.entity(), 0.2f, 0.2f, 0.2f, 10);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.8f);
            }

            // Phase 2 (after 60) — rotate the whole mandala 1.5 deg/tick
            if (tick > 60) {
                spinAngle += (float) Math.toRadians(1.5);
                for (int i = 0; i < innerRing.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / innerCount + spinAngle;
                    Location loc = c.clone().add(Math.cos(a) * innerR, 0.04, Math.sin(a) * innerR);
                    BlockDisplay e = innerRing.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < middleRing.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / middleCount + spinAngle;
                    Location loc = c.clone().add(Math.cos(a) * middleR, 0.04, Math.sin(a) * middleR);
                    BlockDisplay e = middleRing.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < outerRing.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / outerCount + spinAngle;
                    Location loc = c.clone().add(Math.cos(a) * outerR, 0.04, Math.sin(a) * outerR);
                    BlockDisplay e = outerRing.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
                for (int i = 0; i < spokes.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / spokeCount + spinAngle;
                    double midR = (innerR + outerR) * 0.5;
                    Location loc = c.clone().add(Math.cos(a) * midR, 0.05, Math.sin(a) * midR);
                    BlockDisplay e = spokes.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                    rotateOn(e, (float) a, 0f, 1f, 0f, 4);
                }
                for (int i = 0; i < nodes.size(); i++) {
                    double a = (Math.PI * 2.0 * i) / nodeCount + spinAngle;
                    Location loc = c.clone().add(Math.cos(a) * outerR, 0.08, Math.sin(a) * outerR);
                    BlockDisplay e = nodes.get(i).entity();
                    if (e == null || !e.isValid()) continue;
                    try { e.teleport(loc); } catch (Throwable ignored) {}
                }
            }

            // ELECTRIC_SPARK at nodes while active
            if (tick > 50 && tick % 4 == 0) {
                for (BlockDisplayHandle bd : nodes) {
                    BlockDisplay e = bd.entity();
                    if (e == null || !e.isValid()) continue;
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, e.getLocation(), 3, 0.15, 0.15, 0.15, 0.08);
                }
            }
            // Center END_ROD pillar during active
            if (tick > 60 && tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.2, 0), 4, 0.2, 0.6, 0.2, 0.04);
            }
            if (tick > 60 && tick % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.0f);
            }

            int duration = config.getDurationTicks();
            if (tick == duration - 25) {
                for (BlockDisplayHandle bd : outerRing) shrinkToZero(bd, 14);
                for (BlockDisplayHandle bd : nodes) shrinkToZero(bd, 14);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.8f);
            }
            if (tick == duration - 15) {
                for (BlockDisplayHandle bd : middleRing) shrinkToZero(bd, 12);
                for (BlockDisplayHandle bd : spokes) shrinkToZero(bd, 12);
            }
            if (tick == duration - 5) {
                for (BlockDisplayHandle bd : innerRing) shrinkToZero(bd, 6);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.3f, 0.5f);
                c.getWorld().spawnParticle(Particle.EXPLOSION, c.clone().add(0, 0.5, 0), 6, 1.0, 0.5, 1.0, 0.05);
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new ChainMandala(plugin); }
    }
}
