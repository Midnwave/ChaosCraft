package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain Mode — BLOCK DISPLAY ATTACKS 11-20 (Falling & Dropping)
 *
 * Heavy-industrial iron/chain themed sky-drops. Every attack telegraphs
 * its impact zone (ground shadow / warning ring) BEFORE the kinetic
 * payload arrives, so skilled players can sidestep. Damage is mostly
 * impact-only; structures collapse, sink, or dissipate after settling.
 *
 * Material palette: IRON_BLOCK, CHAIN, NETHERITE_BLOCK, ANVIL,
 *                   GRAY_CONCRETE, DARK_OAK_LOG, RED_CONCRETE
 * Particles: BLOCK(IRON_BLOCK / NETHERITE_BLOCK / ANVIL), SMOKE,
 *            LARGE_SMOKE, CRIT, ELECTRIC_SPARK, LAVA, SOUL_FIRE_FLAME,
 *            EXPLOSION (sparingly)
 * Sounds:    BLOCK_CHAIN_FALL, BLOCK_ANVIL_LAND, BLOCK_ANVIL_HIT,
 *            BLOCK_ANVIL_PLACE, BLOCK_NETHERITE_BLOCK_HIT,
 *            ENTITY_IRON_GOLEM_ATTACK, ENTITY_IRON_GOLEM_HURT,
 *            ENTITY_IRON_GOLEM_STEP, BLOCK_METAL_HIT,
 *            BLOCK_GRINDSTONE_USE, ENTITY_GENERIC_EXPLODE (sparingly)
 *
 * Attacks:
 * 11. ChainWeightDrop      — Targeted plumb bob slam
 * 12. AnvilBarrage         — 6 anvils rain in scattered spots
 * 13. ChainCurtain         — Wide wall of chains crosses the arena
 * 14. IronCageDrop         — Cage enclosure with escape window
 * 15. GravityHammer        — Telegraphed netherite hammer slam
 * 16. ChainAnchorFall      — Spinning ship anchor embeds in ground
 * 17. IronSpikeShower      — 15 thrown spikes rain over an area
 * 18. WreckingBallCrater   — One colossal ball, massive radius
 * 19. ChainNetDescent      — 4x4 chain net slowly descends
 * 20. ChainPlumbArray      — 3x3 grid of plumbs in cascade
 */
public final class ChainBlockDisplay2 {
    private ChainBlockDisplay2() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainWeightDrop(plugin));
        registry.register(new AnvilBarrage(plugin));
        registry.register(new ChainCurtain(plugin));
        registry.register(new IronCageDrop(plugin));
        registry.register(new GravityHammer(plugin));
        registry.register(new ChainAnchorFall(plugin));
        registry.register(new IronSpikeShower(plugin));
        registry.register(new WreckingBallCrater(plugin));
        registry.register(new ChainNetDescent(plugin));
        registry.register(new ChainPlumbArray(plugin));
    }

    // ================================================================
    // Shared helpers (static utility on outer class)
    // ================================================================

    private static void setTranslation(BlockDisplayHandle h, float x, float y, float z, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                new Vector3f(x, y, z),
                new AxisAngle4f().set(t.getLeftRotation()),
                t.getScale(),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void setScale(BlockDisplayHandle h, float sx, float sy, float sz, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f().set(t.getLeftRotation()),
                new Vector3f(sx, sy, sz),
                new AxisAngle4f().set(t.getRightRotation())
        ));
    }

    private static void setRotation(BlockDisplayHandle h, float angle, float ax, float ay, float az, int duration) {
        BlockDisplay e = h.entity();
        Transformation t = e.getTransformation();
        e.setInterpolationDuration(duration);
        e.setInterpolationDelay(0);
        e.setTransformation(new Transformation(
                t.getTranslation(),
                new AxisAngle4f(angle, ax, ay, az),
                t.getScale(),
                new AxisAngle4f(0, 0, 1, 0)
        ));
    }

    // ================================================================
    // #11 — CHAIN WEIGHT DROP — "The Plumb"
    // 32 blocks. Targeted slam (read shadow, sidestep).
    // ================================================================
    public static class ChainWeightDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private final List<BlockDisplayHandle> weightParts = new ArrayList<>();
        private BlockDisplayHandle warningSlab;
        private Location impactGround;
        private boolean impacted = false;
        // Y position of the weight body (Y+16 → 0). All weight pieces share this.
        private double weightY = 16.0;
        private double velocity = -0.3; // accelerating

        public ChainWeightDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_weight_drop", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(280.0);
            config.setImpactRadius(2.5);
            config.setDurationTicks(60);
            config.setCooldownTicks(200);
            config.setChance(8.0);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Targeted slam (read shadow, sidestep)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            impactGround = center.clone();

            // Warning slab: flat RED_CONCRETE on the ground directly below drop
            warningSlab = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.RED_CONCRETE);
            warningSlab.scale(0.8f, 0.01f, 0.8f).glow(255, 30, 30).interpolation(0, 0);
            spawnedEntities.add(warningSlab.entity());

            // 16 chain links stacked top→bottom at 0.5 spacing (Y+16 down to Y+8)
            for (int i = 0; i < 16; i++) {
                Location loc = center.clone().add(0, 16.0 - i * 0.5, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(loc, Material.CHAIN);
                link.scale(0.18f, 0.4f, 0.18f).glow(140, 140, 150).interpolation(2, 0);
                spawnedEntities.add(link.entity());
                chainLinks.add(link);
            }

            // Weight body: 1 IRON_BLOCK core
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, 16, 0), Material.IRON_BLOCK);
            core.scale(0.65f, 0.8f, 0.65f).glow(200, 200, 210).interpolation(2, 0);
            spawnedEntities.add(core.entity());
            weightParts.add(core);

            // 4 GRAY_CONCRETE taper panels around the core (octagonal taper)
            double[][] panel = {{0.35, -0.1, 0}, {-0.35, -0.1, 0}, {0, -0.1, 0.35}, {0, -0.1, -0.35}};
            for (double[] off : panel) {
                BlockDisplayHandle p = displayBuilder.spawnBlock(
                        center.clone().add(off[0], 16 + off[1], off[2]), Material.GRAY_CONCRETE);
                p.scale(0.55f, 0.18f, 0.18f).glow(120, 120, 130).interpolation(2, 0);
                spawnedEntities.add(p.entity());
                weightParts.add(p);
            }

            // Pointed tip: 1 NETHERITE_BLOCK below core
            BlockDisplayHandle tip = displayBuilder.spawnBlock(center.clone().add(0, 16 - 0.55, 0), Material.NETHERITE_BLOCK);
            tip.scale(0.22f, 0.35f, 0.22f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(tip.entity());
            weightParts.add(tip);

            // 4 IRON_BLOCK micro-spike fins from tip
            double[][] fin = {{0.18, -0.7, 0}, {-0.18, -0.7, 0}, {0, -0.7, 0.18}, {0, -0.7, -0.18}};
            for (double[] off : fin) {
                BlockDisplayHandle f = displayBuilder.spawnBlock(
                        center.clone().add(off[0], 16 + off[1], off[2]), Material.IRON_BLOCK);
                f.scale(0.05f, 0.18f, 0.05f).glow(200, 200, 210).interpolation(2, 0);
                spawnedEntities.add(f.entity());
                weightParts.add(f);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.5f);
            // Warning glow ring on ground
            w.spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 0.1, 0), 30, 1.2, 0.05, 1.2, 0.01);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pulse warning ring while falling
            if (!impacted && tick % 4 == 0) {
                DisplayBuilder.particleRing(impactGround.clone().add(0, 0.15, 0), config.getImpactRadius(),
                        Particle.SOUL_FIRE_FLAME, 24, null);
            }

            if (impacted) {
                // Dissipate sequence
                int dt = tick - impactTick;
                if (dt == 2) {
                    for (BlockDisplayHandle p : weightParts) setScale(p, 1.0f, 1.0f, 1.0f, 2);
                }
                if (dt == 6) {
                    for (BlockDisplayHandle p : weightParts) setScale(p, 0f, 0f, 0f, 6);
                    for (BlockDisplayHandle l : chainLinks) setScale(l, 0f, 0f, 0f, 6);
                    if (warningSlab != null) setScale(warningSlab, 0f, 0f, 0f, 6);
                }
                return;
            }

            // Falling motion: accelerate downward
            velocity -= 0.04;
            if (velocity < -3.0) velocity = -3.0;
            weightY += velocity;

            // Move all weight pieces
            for (BlockDisplayHandle p : weightParts) {
                p.entity().teleport(impactGround.clone().add(p.entity().getLocation().getX() - impactGround.getX(),
                        weightY, p.entity().getLocation().getZ() - impactGround.getZ()));
            }
            // Move chain bottom→top to follow descent (chain materialises as weight drops past)
            for (int i = 0; i < chainLinks.size(); i++) {
                double linkY = Math.min(16.0, weightY + (i + 1) * 0.5);
                chainLinks.get(i).entity().teleport(impactGround.clone().add(0, linkY, 0));
            }

            // Falling dust trailing weight
            if (tick % 2 == 0) {
                w.spawnParticle(Particle.BLOCK, impactGround.clone().add(0, weightY, 0), 6, 0.25, 0.25, 0.25, 0.02,
                        Material.IRON_BLOCK.createBlockData());
            }

            // Whoosh sound mid-fall
            if (tick == 10 || tick == 18 || tick == 26) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.4f + tick * 0.02f);
            }

            // Impact
            if (weightY <= 0.1) {
                impacted = true;
                impactTick = tick;
                triggerImpactDamage(impactGround.clone());
                w.spawnParticle(Particle.BLOCK, impactGround, 35, 1.8, 0.6, 1.8, 0.3,
                        Material.IRON_BLOCK.createBlockData());
                w.spawnParticle(Particle.CRIT, impactGround, 25, 1.5, 0.5, 1.5, 0.4);
                w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.5, 0), 10, 0.8, 0.4, 0.8, 0.05);
                DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.7f);
                DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.7f);
                // Expand the core briefly on impact
                if (!weightParts.isEmpty()) {
                    setScale(weightParts.get(0), 1.0f, 1.0f, 1.0f, 2);
                }
            }
        }

        private int impactTick = -1;

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainWeightDrop(plugin); }
    }

    // ================================================================
    // #12 — ANVIL BARRAGE — "The Raining Metal"
    // 36 blocks (6 anvils × 6 displays). Multi-spot rain (read shadow grid).
    // ================================================================
    public static class AnvilBarrage extends BlockDisplayAttack {
        private static final int ANVIL_COUNT = 6;
        private final List<AnvilHandle> anvils = new ArrayList<>();
        private final List<BlockDisplayHandle> warnings = new ArrayList<>();

        private static class AnvilHandle {
            final List<BlockDisplayHandle> parts = new ArrayList<>();
            final List<BlockDisplayHandle> chain = new ArrayList<>();
            final Location ground;
            final int spawnTick;
            double y;
            double vel = -0.3;
            boolean impacted = false;
            int impactTick = -1;
            float tilt;
            AnvilHandle(Location ground, int spawnTick, float tilt) {
                this.ground = ground; this.spawnTick = spawnTick; this.tilt = tilt; this.y = 12.0;
            }
        }

        public AnvilBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anvil_barrage", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(220.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(240);
            config.setChance(7.0);
            config.setEnabled(true);
            config.setDesignType("Multi-spot rain (read shadow grid)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            java.util.Random rnd = new java.util.Random();

            for (int i = 0; i < ANVIL_COUNT; i++) {
                double a = (Math.PI * 2 * i) / ANVIL_COUNT + rnd.nextDouble() * 0.6;
                double r = 1.5 + rnd.nextDouble() * 2.5;
                Location ground = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                int spawnTick = i * 8;
                float tilt = (float) Math.toRadians((rnd.nextDouble() - 0.5) * 12);
                AnvilHandle ah = new AnvilHandle(ground, spawnTick, tilt);

                // Warning slab on ground
                BlockDisplayHandle warn = displayBuilder.spawnBlock(ground.clone().add(0, 0.05, 0), Material.RED_CONCRETE);
                warn.scale(0.7f, 0.01f, 0.7f).glow(255, 30, 30).interpolation(0, spawnTick);
                spawnedEntities.add(warn.entity());
                warnings.add(warn);

                anvils.add(ah);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.3f, 0.7f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 6, 0), 30, 3, 2, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < anvils.size(); i++) {
                AnvilHandle ah = anvils.get(i);
                if (tick < ah.spawnTick) continue;

                // Lazily spawn anvil structure on first eligible tick
                if (ah.parts.isEmpty()) {
                    // Anvil body
                    BlockDisplayHandle body = displayBuilder.spawnBlock(ah.ground.clone().add(0, 12, 0), Material.ANVIL);
                    body.scale(0.8f, 0.7f, 0.8f).glow(80, 80, 90).interpolation(2, 0);
                    setRotation(body, ah.tilt, 1f, 0f, 0f, 0);
                    spawnedEntities.add(body.entity());
                    ah.parts.add(body);

                    // Face slab on top
                    BlockDisplayHandle face = displayBuilder.spawnBlock(ah.ground.clone().add(0, 12 + 0.4, 0), Material.IRON_BLOCK);
                    face.scale(0.75f, 0.08f, 0.45f).glow(200, 200, 210).interpolation(2, 0);
                    spawnedEntities.add(face.entity());
                    ah.parts.add(face);

                    // 4 legs beneath
                    double[][] legOff = {{-0.25, -0.2, -0.25}, {0.25, -0.2, -0.25}, {-0.25, -0.2, 0.25}, {0.25, -0.2, 0.25}};
                    for (double[] off : legOff) {
                        BlockDisplayHandle leg = displayBuilder.spawnBlock(
                                ah.ground.clone().add(off[0], 12 + off[1], off[2]), Material.IRON_BLOCK);
                        leg.scale(0.15f, 0.28f, 0.15f).glow(180, 180, 190).interpolation(2, 0);
                        spawnedEntities.add(leg.entity());
                        ah.parts.add(leg);
                    }

                    // Chain stack above (5 links)
                    for (int j = 0; j < 5; j++) {
                        BlockDisplayHandle link = displayBuilder.spawnBlock(
                                ah.ground.clone().add(0, 12 + 0.8 + j * 0.5, 0), Material.CHAIN);
                        link.scale(0.14f, 0.45f, 0.14f).glow(140, 140, 150).interpolation(2, 0);
                        spawnedEntities.add(link.entity());
                        ah.chain.add(link);
                    }
                    DisplayBuilder.playSound(ah.ground, Sound.BLOCK_ANVIL_PLACE, 0.7f, 0.5f);
                }

                if (ah.impacted) {
                    int dt = tick - ah.impactTick;
                    if (dt == 5) {
                        for (BlockDisplayHandle p : ah.parts) setScale(p, 0f, 0f, 0f, 5);
                    }
                    if (dt == 8) {
                        for (BlockDisplayHandle l : ah.chain) setScale(l, 0f, 0f, 0f, 6);
                    }
                    continue;
                }

                // Accelerating fall
                ah.vel -= 0.05;
                if (ah.vel < -3.0) ah.vel = -3.0;
                ah.y += ah.vel;

                // Move structure
                for (BlockDisplayHandle p : ah.parts) {
                    p.entity().teleport(ah.ground.clone().add(
                            p.entity().getLocation().getX() - ah.ground.getX(),
                            ah.y - 12 + p.entity().getLocation().getY() - p.entity().getLocation().getY() + (p.entity().getLocation().getY() - (12 + ah.y - ah.y)), // approximate — see simpler path below
                            p.entity().getLocation().getZ() - ah.ground.getZ()));
                }
                // (Simpler & correct movement: teleport every part to its initial offset from anvil-body Y)
                // We'll just do this directly:
                rebuildAnvilTransform(ah);

                if (tick % 2 == 0 && !ah.parts.isEmpty()) {
                    w.spawnParticle(Particle.BLOCK, ah.parts.get(0).entity().getLocation(), 4, 0.3, 0.3, 0.3, 0.02,
                            Material.IRON_BLOCK.createBlockData());
                }

                if (ah.y <= 0.1) {
                    ah.impacted = true;
                    ah.impactTick = tick;
                    triggerImpactDamage(ah.ground.clone());
                    w.spawnParticle(Particle.BLOCK, ah.ground, 30, 1.5, 0.5, 1.5, 0.3,
                            Material.ANVIL.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, ah.ground.clone().add(0, 0.4, 0), 12, 0.7, 0.4, 0.7, 0.05);
                    w.spawnParticle(Particle.CRIT, ah.ground, 15, 1.0, 0.3, 1.0, 0.2);
                    DisplayBuilder.playSound(ah.ground, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.7f + i * 0.05f);
                    DisplayBuilder.playSound(ah.ground, Sound.BLOCK_ANVIL_HIT, 1.0f, 0.6f);
                }
            }
        }

        private void rebuildAnvilTransform(AnvilHandle ah) {
            // Layout the parts at their original offsets, but using current Y as the anvil body.
            // parts[0] body @ y, parts[1] face top slab @ y+0.4, parts[2..5] legs @ y-0.2
            // chain links @ y+0.8 + j*0.5
            if (ah.parts.isEmpty()) return;
            Location g = ah.ground;
            ah.parts.get(0).entity().teleport(g.clone().add(0, ah.y, 0));
            ah.parts.get(1).entity().teleport(g.clone().add(0, ah.y + 0.4, 0));
            double[][] legOff = {{-0.25, -0.2, -0.25}, {0.25, -0.2, -0.25}, {-0.25, -0.2, 0.25}, {0.25, -0.2, 0.25}};
            for (int j = 0; j < 4 && j + 2 < ah.parts.size(); j++) {
                ah.parts.get(j + 2).entity().teleport(g.clone().add(legOff[j][0], ah.y + legOff[j][1], legOff[j][2]));
            }
            for (int j = 0; j < ah.chain.size(); j++) {
                ah.chain.get(j).entity().teleport(g.clone().add(0, ah.y + 0.8 + j * 0.5, 0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AnvilBarrage(plugin); }
    }

    // ================================================================
    // #13 — CHAIN CURTAIN — "The Iron Rain"
    // 32 blocks. Wide-AoE wall (sprint perpendicular).
    // ================================================================
    public static class ChainCurtain extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> columns = new ArrayList<>(); // 8×4 = 32 chain links
        private final List<BlockDisplayHandle> weights = new ArrayList<>(); // 8
        private final List<BlockDisplayHandle> support = new ArrayList<>(); // crossbar + brackets + supports
        private Location curtainCenter;
        private double curtainY = 8.0; // starting height
        private boolean landed = false;
        private int landTick = -1;

        public ChainCurtain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_curtain", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(320.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(5);
            config.setDamageDelayTicks(20);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Wide-AoE wall (sprint perpendicular)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            curtainCenter = center.clone();

            // Crossbar DARK_OAK_LOG (wide horizontal)
            BlockDisplayHandle bar = displayBuilder.spawnBlock(center.clone().add(0, 8.5, 0), Material.DARK_OAK_LOG);
            bar.scale(5.8f, 0.22f, 0.22f).glow(80, 50, 20).interpolation(20, 0);
            spawnedEntities.add(bar.entity());
            support.add(bar);

            // 2 IRON_BLOCK hook fittings at crossbar ends
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -2.9 : 2.9;
                BlockDisplayHandle hook = displayBuilder.spawnBlock(center.clone().add(xOff, 8.5, 0), Material.IRON_BLOCK);
                hook.scale(0.3f, 0.35f, 0.3f).glow(200, 200, 210).interpolation(20, 0);
                spawnedEntities.add(hook.entity());
                support.add(hook);
            }

            // 8 chain columns, each 4 links
            for (int col = 0; col < 8; col++) {
                double xOff = -2.45 + col * 0.7;
                for (int link = 0; link < 4; link++) {
                    Location loc = center.clone().add(xOff, 8.0 - link * 0.75, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CHAIN);
                    h.scale(0.18f, 0.75f, 0.18f).glow(140, 140, 150).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    columns.add(h);
                }
            }

            // 8 weight blocks at column bases
            for (int col = 0; col < 8; col++) {
                double xOff = -2.45 + col * 0.7;
                Location loc = center.clone().add(xOff, 5.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.22f, 0.22f, 0.22f).glow(220, 220, 230).interpolation(20, 0);
                spawnedEntities.add(h.entity());
                weights.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 2.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.9f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 8.5, 0), 50, 3, 0.3, 0.3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Descent over first 20 ticks: 8 → 1 (chain bottoms at head height)
            if (tick < 20) {
                curtainY = 8.0 - (tick / 20.0) * 7.0;
                rebuildCurtain();
                if (tick == 19) {
                    landed = true; landTick = tick;
                    // Weights continue further down
                    for (int i = 0; i < weights.size(); i++) {
                        weights.get(i).entity().teleport(curtainCenter.clone().add(-2.45 + i * 0.7, 0.2, 0));
                    }
                    w.spawnParticle(Particle.BLOCK, curtainCenter.clone(), 60, 2.9, 0.2, 0.4, 0.1,
                            Material.IRON_BLOCK.createBlockData());
                    DisplayBuilder.playSound(curtainCenter, Sound.BLOCK_METAL_HIT, 1.4f, 0.6f);
                    DisplayBuilder.playSound(curtainCenter, Sound.BLOCK_ANVIL_LAND, 1.1f, 0.4f);
                }
            }

            // After landing: slowly advance forward (Z+) — 0.07 blocks/tick
            if (landed && tick > 20 && tick < 160) {
                curtainCenter.add(0, 0, 0.07);
                rebuildCurtain();
            }

            // Particles along the curtain
            if (tick % 3 == 0 && landed) {
                for (int i = 0; i < 8; i++) {
                    double xOff = -2.45 + i * 0.7;
                    w.spawnParticle(Particle.BLOCK, curtainCenter.clone().add(xOff, 0.3, 0), 3, 0.1, 0.2, 0.1, 0.01,
                            Material.IRON_BLOCK.createBlockData());
                }
                w.spawnParticle(Particle.SMOKE, curtainCenter.clone().add(0, 7.5, 0), 4, 2.5, 0.2, 0.2, 0.02);
            }

            // Periodic chain rattle
            if (landed && tick % 25 == 0) {
                DisplayBuilder.playSound(curtainCenter, Sound.BLOCK_CHAIN_FALL, 0.7f, 0.5f);
            }

            // Dissipate near end
            if (tick >= 165) {
                int dt = tick - 165;
                if (dt == 0) {
                    for (BlockDisplayHandle h : columns) setScale(h, 0f, 0f, 0f, 10);
                    for (BlockDisplayHandle h : weights) setScale(h, 0f, 0f, 0f, 10);
                    for (BlockDisplayHandle h : support) setScale(h, 0f, 0f, 0f, 10);
                    DisplayBuilder.playSound(curtainCenter, Sound.BLOCK_CHAIN_FALL, 1.3f, 0.3f);
                }
            }
        }

        private void rebuildCurtain() {
            Location c = curtainCenter;
            // Support (crossbar + 2 hooks)
            if (support.size() >= 3) {
                support.get(0).entity().teleport(c.clone().add(0, curtainY + 0.5, 0));
                support.get(1).entity().teleport(c.clone().add(-2.9, curtainY + 0.5, 0));
                support.get(2).entity().teleport(c.clone().add(2.9, curtainY + 0.5, 0));
            }
            // 32 chain links
            for (int idx = 0; idx < columns.size(); idx++) {
                int col = idx / 4;
                int link = idx % 4;
                double xOff = -2.45 + col * 0.7;
                columns.get(idx).entity().teleport(c.clone().add(xOff, curtainY - link * 0.75, 0));
            }
            // Weights
            for (int i = 0; i < weights.size(); i++) {
                double xOff = -2.45 + i * 0.7;
                double y = landed ? 0.2 : (curtainY - 2.5);
                weights.get(i).entity().teleport(c.clone().add(xOff, y, 0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainCurtain(plugin); }
    }

    // ================================================================
    // #14 — IRON CAGE DROP — "The Prison"
    // 36 blocks. Enclosure (escape before closure).
    // ================================================================
    public static class IronCageDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cageParts = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> chainLift = new ArrayList<>();
        private Location cageCenter;
        private double cageY = 14.0;
        private int pauseTicks = 0;
        private boolean landed = false;
        private int landTick = -1;

        public IronCageDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_cage_drop", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(240.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(240);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Enclosure (escape before closure)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            cageCenter = center.clone();

            // 4 corner posts (IRON_BLOCK 0.28×2.5×0.28)
            double[][] posts = {{-1.1, 0, -1.1}, {1.1, 0, -1.1}, {-1.1, 0, 1.1}, {1.1, 0, 1.1}};
            for (double[] off : posts) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 14 + 1.25, off[2]), Material.IRON_BLOCK);
                h.scale(0.28f, 2.5f, 0.28f).glow(200, 200, 210).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                cageParts.add(h);
            }

            // 8 horizontal bars: 4 walls × 2 heights (Y=0.5, Y=2.0)
            double[][] barOff = {
                    {0, 0.5, -1.1}, {0, 2.0, -1.1}, // wall front
                    {0, 0.5, 1.1}, {0, 2.0, 1.1},   // wall back
                    {-1.1, 0.5, 0}, {-1.1, 2.0, 0}, // wall left
                    {1.1, 0.5, 0}, {1.1, 2.0, 0}    // wall right
            };
            for (int j = 0; j < barOff.length; j++) {
                double[] off = barOff[j];
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 14 + off[1], off[2]), Material.IRON_BLOCK);
                // Long axis depends on which wall
                if (j < 4) h.scale(2.0f, 0.14f, 0.14f); // front/back (along X)
                else h.scale(0.14f, 0.14f, 2.0f);       // left/right (along Z)
                h.glow(190, 190, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                cageParts.add(h);
            }

            // Top cap: 4 IRON_BLOCK frame pieces along top perimeter
            double[][] capOff = {{0, 2.5, -1.1}, {0, 2.5, 1.1}, {-1.1, 2.5, 0}, {1.1, 2.5, 0}};
            for (int j = 0; j < capOff.length; j++) {
                double[] off = capOff[j];
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 14 + off[1], off[2]), Material.IRON_BLOCK);
                if (j < 2) h.scale(1.8f, 0.1f, 0.1f);
                else h.scale(0.1f, 0.1f, 1.8f);
                h.glow(220, 220, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                cageParts.add(h);
            }

            // 4 NETHERITE_BLOCK reinforcement brackets at wall midpoints
            double[][] brOff = {{0, 1.25, -1.15}, {0, 1.25, 1.15}, {-1.15, 1.25, 0}, {1.15, 1.25, 0}};
            for (int j = 0; j < brOff.length; j++) {
                double[] off = brOff[j];
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 14 + off[1], off[2]), Material.NETHERITE_BLOCK);
                if (j < 2) h.scale(0.28f, 0.2f, 0.1f);
                else h.scale(0.1f, 0.2f, 0.28f);
                h.glow(60, 60, 70).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                cageParts.add(h);
            }

            // Chain lift: 4 columns of 3 chain links above corners → 1 central hook
            for (double[] off : posts) {
                for (int j = 0; j < 3; j++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(off[0] * (1.0 - j * 0.25), 14 + 2.7 + j * 0.5, off[2] * (1.0 - j * 0.25)),
                            Material.CHAIN);
                    h.scale(0.14f, 0.45f, 0.14f).glow(140, 140, 150).interpolation(3, 0);
                    spawnedEntities.add(h.entity());
                    chainLift.add(h);
                }
            }
            BlockDisplayHandle hook = displayBuilder.spawnBlock(center.clone().add(0, 14 + 4.5, 0), Material.IRON_BLOCK);
            hook.scale(0.35f, 0.35f, 0.35f).glow(220, 220, 230).interpolation(3, 0);
            spawnedEntities.add(hook.entity());
            chainLift.add(hook);

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_PLACE, 1.0f, 0.4f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 16, 0), 25, 1.5, 0.5, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Phase 1: drop from 14 → 3 over 18 ticks
            if (tick < 18) {
                cageY = 14.0 - (tick / 18.0) * 11.0;
                rebuildCage();
            } else if (tick < 21) {
                cageY = 3.0;
                rebuildCage();
                pauseTicks++;
                if (tick == 18) {
                    DisplayBuilder.playSound(cageCenter, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.7f);
                    w.spawnParticle(Particle.CRIT, cageCenter.clone().add(0, 3, 0), 30, 1.5, 0.5, 1.5, 0.2);
                }
            } else if (!landed && tick < 25) {
                // Phase 2: slam from 3 → 0 fast
                cageY = 3.0 - ((tick - 21) / 4.0) * 3.0;
                rebuildCage();
                if (tick == 24) {
                    landed = true; landTick = tick;
                    cageY = 0;
                    rebuildCage();
                    spawnFloor();
                    triggerImpactDamage(cageCenter.clone());
                    w.spawnParticle(Particle.BLOCK, cageCenter, 40, 1.5, 0.3, 1.5, 0.3,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, cageCenter.clone().add(0, 0.5, 0), 20, 1.3, 0.4, 1.3, 0.05);
                    DisplayBuilder.playSound(cageCenter, Sound.ENTITY_IRON_GOLEM_HURT, 1.6f, 0.5f);
                    DisplayBuilder.playSound(cageCenter, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.4f);
                }
            }

            // Cage settling steps every 30t while active
            if (landed && tick > 30 && tick % 30 == 0) {
                DisplayBuilder.playSound(cageCenter, Sound.ENTITY_IRON_GOLEM_STEP, 0.9f, 0.4f);
            }

            // Bars rattle particle while active
            if (landed && tick % 6 == 0) {
                w.spawnParticle(Particle.SMOKE, cageCenter.clone().add(
                        (Math.random() - 0.5) * 2.4, 1.2, (Math.random() - 0.5) * 2.4), 1, 0.05, 0.05, 0.05, 0.01);
            }

            // Sink phase final 20t
            if (tick >= config.getDurationTicks() - 25) {
                int dt = tick - (config.getDurationTicks() - 25);
                double sinkY = -dt * 0.15;
                cageY = sinkY;
                rebuildCage();
                for (BlockDisplayHandle f : floor) setTranslation(f, -0.5f, (float) sinkY - 0.5f, -0.5f, 4);
                if (dt == 20) {
                    for (BlockDisplayHandle h : cageParts) setScale(h, 0f, 0f, 0f, 5);
                    for (BlockDisplayHandle h : chainLift) setScale(h, 0f, 0f, 0f, 5);
                    for (BlockDisplayHandle f : floor) setScale(f, 0f, 0f, 0f, 5);
                }
            }
        }

        private void spawnFloor() {
            double[][] tile = {{-0.5, 0.02, -0.5}, {0.5, 0.02, -0.5}, {-0.5, 0.02, 0.5}, {0.5, 0.02, 0.5}};
            for (double[] t : tile) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(cageCenter.clone().add(t[0], t[1], t[2]), Material.IRON_BLOCK);
                h.scale(0.0f, 0.0f, 0.0f).glow(160, 160, 170).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                floor.add(h);
                setScale(h, 0.85f, 0.03f, 0.85f, 4);
            }
            DisplayBuilder.playSound(cageCenter, Sound.BLOCK_CHAIN_FALL, 0.6f, 1.2f);
        }

        private void rebuildCage() {
            Location c = cageCenter;
            // Posts
            for (int i = 0; i < 4; i++) {
                double[] o = (i == 0) ? new double[]{-1.1, 0, -1.1} :
                        (i == 1) ? new double[]{1.1, 0, -1.1} :
                                (i == 2) ? new double[]{-1.1, 0, 1.1} : new double[]{1.1, 0, 1.1};
                cageParts.get(i).entity().teleport(c.clone().add(o[0], cageY + 1.25, o[2]));
            }
            // Bars (indices 4..11) — same positions used in spawn
            double[][] barOff = {
                    {0, 0.5, -1.1}, {0, 2.0, -1.1},
                    {0, 0.5, 1.1}, {0, 2.0, 1.1},
                    {-1.1, 0.5, 0}, {-1.1, 2.0, 0},
                    {1.1, 0.5, 0}, {1.1, 2.0, 0}
            };
            for (int j = 0; j < barOff.length; j++) {
                cageParts.get(4 + j).entity().teleport(c.clone().add(barOff[j][0], cageY + barOff[j][1], barOff[j][2]));
            }
            // Top cap (indices 12..15)
            double[][] capOff = {{0, 2.5, -1.1}, {0, 2.5, 1.1}, {-1.1, 2.5, 0}, {1.1, 2.5, 0}};
            for (int j = 0; j < capOff.length; j++) {
                cageParts.get(12 + j).entity().teleport(c.clone().add(capOff[j][0], cageY + capOff[j][1], capOff[j][2]));
            }
            // Brackets (16..19)
            double[][] brOff = {{0, 1.25, -1.15}, {0, 1.25, 1.15}, {-1.15, 1.25, 0}, {1.15, 1.25, 0}};
            for (int j = 0; j < brOff.length; j++) {
                cageParts.get(16 + j).entity().teleport(c.clone().add(brOff[j][0], cageY + brOff[j][1], brOff[j][2]));
            }
            // Chain lift: 12 links + 1 hook. Lift stays anchored aloft — links span from cage top to hook.
            int idx = 0;
            double[][] posts = {{-1.1, 0, -1.1}, {1.1, 0, -1.1}, {-1.1, 0, 1.1}, {1.1, 0, 1.1}};
            for (double[] off : posts) {
                for (int j = 0; j < 3; j++) {
                    chainLift.get(idx++).entity().teleport(c.clone().add(
                            off[0] * (1.0 - j * 0.25),
                            cageY + 2.7 + j * 0.5,
                            off[2] * (1.0 - j * 0.25)));
                }
            }
            chainLift.get(idx).entity().teleport(c.clone().add(0, cageY + 4.5, 0));
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronCageDrop(plugin); }
    }

    // ================================================================
    // #15 — GRAVITY HAMMER — "The Slam"
    // 30 blocks. Telegraphed slam (read warning radius).
    // ================================================================
    public static class GravityHammer extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> chains = new ArrayList<>();
        private BlockDisplayHandle warning;
        private Location impactGround;
        private double hammerY = 8.0;
        private boolean slammed = false;
        private int slamTick = -1;
        // Telegraph phase: 20 ticks visible overhead, then slam in 4 ticks.

        public GravityHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_hammer", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(480.0);
            config.setImpactRadius(4.5);
            config.setDurationTicks(80);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Telegraphed slam (read warning radius)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            impactGround = center.clone();

            // Warning shadow on ground (wide hammer footprint)
            warning = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.GRAY_CONCRETE);
            warning.scale(2.8f, 0.01f, 1.1f).glow(255, 30, 30).interpolation(0, 0);
            spawnedEntities.add(warning.entity());

            // Hammer head: 3 NETHERITE_BLOCK cubes side-by-side
            for (int i = 0; i < 3; i++) {
                double xOff = -1.8 + i * 1.8;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, 8, 0), Material.NETHERITE_BLOCK);
                h.scale(1.8f, 0.8f, 0.9f).glow(60, 60, 70).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }
            // 2 IRON_BLOCK end-caps
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -2.6 : 2.6;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(xOff, 8, 0), Material.IRON_BLOCK);
                h.scale(0.18f, 0.75f, 0.85f).glow(200, 200, 210).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }
            // 4 GRAY_CONCRETE trim slabs (top/bottom/front/back of head)
            double[][] trim = {{0, 0.5, 0}, {0, -0.5, 0}, {0, 0, 0.5}, {0, 0, -0.5}};
            for (double[] off : trim) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 8 + off[1], off[2]), Material.GRAY_CONCRETE);
                if (off[1] != 0) h.scale(1.75f, 0.04f, 0.85f);
                else h.scale(1.75f, 0.85f, 0.04f);
                h.glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                head.add(h);
            }

            // Shaft (DARK_OAK_LOG) below head
            BlockDisplayHandle s = displayBuilder.spawnBlock(center.clone().add(0, 8 + 1.5, 0), Material.DARK_OAK_LOG);
            s.scale(0.28f, 2.5f, 0.28f).glow(80, 50, 20).interpolation(3, 0);
            spawnedEntities.add(s.entity());
            shaft.add(s);
            // 2 IRON_BLOCK grip wraps
            for (int i = 0; i < 2; i++) {
                BlockDisplayHandle g = displayBuilder.spawnBlock(
                        center.clone().add(0, 8 + 0.8 + i * 1.2, 0), Material.IRON_BLOCK);
                g.scale(0.34f, 0.14f, 0.34f).glow(200, 200, 210).interpolation(3, 0);
                spawnedEntities.add(g.entity());
                shaft.add(g);
            }

            // 4 chains above
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle ch = displayBuilder.spawnBlock(
                        center.clone().add(0, 8 + 3.0 + i * 0.5, 0), Material.CHAIN);
                ch.scale(0.18f, 0.45f, 0.18f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(ch.entity());
                chains.add(ch);
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.8f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.4f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 0.2, 0), 30, 2.5, 0.1, 1.0, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Telegraph: hammer overhead, warning pulses
            if (tick < 20) {
                if (tick % 3 == 0) {
                    DisplayBuilder.particleRing(impactGround.clone().add(0, 0.2, 0), 2.6, Particle.SOUL_FIRE_FLAME, 32, null);
                }
                if (tick % 8 == 0) {
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.3f);
                }
                w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.2, 0), 4, 1.3, 0.05, 0.5, 0.02);
            }

            // Slam: Y+8 → 0 in 4 ticks
            if (tick >= 20 && tick < 24) {
                double t = (tick - 20) / 4.0;
                hammerY = 8.0 - t * 8.0;
                if (hammerY < 0) hammerY = 0;
                rebuildHammer();
            }
            if (tick == 24 && !slammed) {
                slammed = true; slamTick = tick;
                triggerImpactDamage(impactGround.clone());
                w.spawnParticle(Particle.BLOCK, impactGround, 50, 2.5, 0.5, 1.2, 0.4,
                        Material.NETHERITE_BLOCK.createBlockData());
                w.spawnParticle(Particle.CRIT, impactGround, 35, 2.2, 0.6, 1.0, 0.4);
                w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.4, 0), 25, 2.5, 0.5, 1.0, 0.08);
                w.spawnParticle(Particle.EXPLOSION, impactGround.clone().add(0, 0.2, 0), 3, 1.5, 0.2, 1.0, 0);
                DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.4f);
                DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.8f, 0.5f);
                DisplayBuilder.playSound(impactGround, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
                if (warning != null) setScale(warning, 0f, 0f, 0f, 4);
            }

            // Settle 15t, then lift 0 → 8 in 20t
            if (tick >= 39 && tick < 59) {
                double t = (tick - 39) / 20.0;
                hammerY = t * 8.0;
                rebuildHammer();
            }
            // Dissipate
            if (tick == 60) {
                for (BlockDisplayHandle h : head) setScale(h, 0f, 0f, 0f, 12);
                for (BlockDisplayHandle h : shaft) setScale(h, 0f, 0f, 0f, 12);
                for (BlockDisplayHandle h : chains) setScale(h, 0f, 0f, 0f, 12);
                DisplayBuilder.playSound(impactGround, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.5f);
            }
        }

        private void rebuildHammer() {
            Location c = impactGround;
            // Head 3 cubes
            for (int i = 0; i < 3; i++) {
                head.get(i).entity().teleport(c.clone().add(-1.8 + i * 1.8, hammerY, 0));
            }
            // End-caps
            head.get(3).entity().teleport(c.clone().add(-2.6, hammerY, 0));
            head.get(4).entity().teleport(c.clone().add(2.6, hammerY, 0));
            // Trim (4 slabs at +/-0.5 y/z around head center)
            double[][] trim = {{0, 0.5, 0}, {0, -0.5, 0}, {0, 0, 0.5}, {0, 0, -0.5}};
            for (int i = 0; i < 4; i++) {
                head.get(5 + i).entity().teleport(c.clone().add(trim[i][0], hammerY + trim[i][1], trim[i][2]));
            }
            // Shaft
            shaft.get(0).entity().teleport(c.clone().add(0, hammerY + 1.5, 0));
            shaft.get(1).entity().teleport(c.clone().add(0, hammerY + 0.8, 0));
            shaft.get(2).entity().teleport(c.clone().add(0, hammerY + 2.0, 0));
            // Chains
            for (int i = 0; i < chains.size(); i++) {
                chains.get(i).entity().teleport(c.clone().add(0, hammerY + 3.0 + i * 0.5, 0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GravityHammer(plugin); }
    }

    // ================================================================
    // #16 — CHAIN ANCHOR FALL — "The Weight of Doom"
    // 34 blocks. Heavy drop (timing + sidestep).
    // ================================================================
    public static class ChainAnchorFall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> anchor = new ArrayList<>();
        private final List<BlockDisplayHandle> chain = new ArrayList<>();
        private BlockDisplayHandle warning;
        private Location impactGround;
        private double anchorY = 14.0;
        private double velocity = -0.3;
        private boolean impacted = false;
        private int impactTick = -1;
        private float spin = 0f;

        public ChainAnchorFall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_anchor_fall", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(400.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(100);
            config.setCooldownTicks(260);
            config.setChance(6.0);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Heavy drop (timing + sidestep)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            impactGround = center.clone();

            // Warning ring
            warning = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.RED_CONCRETE);
            warning.scale(1.0f, 0.01f, 1.0f).glow(255, 30, 30).interpolation(0, 0);
            spawnedEntities.add(warning.entity());

            // Anchor ring: 4 CHAIN blocks at top in a small ring (anchor crown)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(a) * 0.25, 14 + 1.6, Math.sin(a) * 0.25), Material.CHAIN);
                h.scale(0.12f, 0.12f, 0.3f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                anchor.add(h);
            }
            // Shank (vertical IRON_BLOCK)
            BlockDisplayHandle shank = displayBuilder.spawnBlock(center.clone().add(0, 14 + 0.8, 0), Material.IRON_BLOCK);
            shank.scale(0.14f, 1.5f, 0.14f).glow(200, 200, 210).interpolation(3, 0);
            spawnedEntities.add(shank.entity());
            anchor.add(shank);
            // Stock crossbar: 2 IRON_BLOCK
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -0.35 : 0.35;
                BlockDisplayHandle st = displayBuilder.spawnBlock(center.clone().add(xOff, 14 + 1.3, 0), Material.IRON_BLOCK);
                st.scale(0.75f, 0.14f, 0.14f).glow(200, 200, 210).interpolation(3, 0);
                spawnedEntities.add(st.entity());
                anchor.add(st);
            }
            // Fluke arms left & right (2 IRON_BLOCK + 2 NETHERITE_BLOCK tips)
            double[][] flukeOff = {{-0.5, -0.4, 0}, {-0.7, -0.6, 0}, {0.5, -0.4, 0}, {0.7, -0.6, 0}};
            for (int i = 0; i < flukeOff.length; i++) {
                double[] off = flukeOff[i];
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(off[0], 14 + 0.0 + off[1], 0), Material.IRON_BLOCK);
                if (i % 2 == 0) h.scale(0.14f, 0.55f, 0.14f);
                else h.scale(0.1f, 0.22f, 0.1f);
                h.glow(190, 190, 200).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                anchor.add(h);
            }
            // Fluke tips: 2 NETHERITE_BLOCK
            for (int i = 0; i < 2; i++) {
                double xOff = (i == 0) ? -0.78 : 0.78;
                BlockDisplayHandle tip = displayBuilder.spawnBlock(center.clone().add(xOff, 14 - 0.75, 0), Material.NETHERITE_BLOCK);
                tip.scale(0.1f, 0.18f, 0.1f).glow(60, 60, 70).interpolation(3, 0);
                spawnedEntities.add(tip.entity());
                anchor.add(tip);
            }

            // 10 chain links above anchor crown
            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, 14 + 2.2 + i * 0.5, 0), Material.CHAIN);
                h.scale(0.18f, 0.45f, 0.18f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                chain.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!impacted) {
                if (tick % 4 == 0) {
                    DisplayBuilder.particleRing(impactGround.clone().add(0, 0.15, 0),
                            config.getImpactRadius(), Particle.SOUL_FIRE_FLAME, 28, null);
                }
                // Accelerating fall + Y-spin (3°/tick)
                velocity -= 0.045;
                if (velocity < -3.0) velocity = -3.0;
                anchorY += velocity;
                spin += (float) Math.toRadians(3.0);
                rebuildAnchor();
                // Apply spin to anchor parts
                for (BlockDisplayHandle p : anchor) setRotation(p, spin, 0f, 1f, 0f, 2);

                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.BLOCK, impactGround.clone().add(0, anchorY, 0), 6, 0.4, 0.4, 0.4, 0.02,
                            Material.IRON_BLOCK.createBlockData());
                }

                if (anchorY <= 0.1) {
                    impacted = true;
                    impactTick = tick;
                    anchorY = -0.3; // embeds slightly
                    rebuildAnchor();
                    triggerImpactDamage(impactGround.clone());
                    w.spawnParticle(Particle.BLOCK, impactGround, 60, 2.0, 0.4, 2.0, 0.4,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.4, 0), 20, 1.5, 0.4, 1.5, 0.05);
                    w.spawnParticle(Particle.EXPLOSION, impactGround.clone().add(0, 0.2, 0), 2, 1.2, 0.2, 1.2, 0);
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.4f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.5f);
                    if (warning != null) setScale(warning, 0f, 0f, 0f, 4);
                }
            } else {
                // Chain slackens after embed
                int dt = tick - impactTick;
                if (dt == 4) {
                    for (int i = 0; i < chain.size(); i++) {
                        setRotation(chain.get(i), (float) Math.toRadians(20 - i * 2), 1f, 0f, 0f, 6);
                    }
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_METAL_HIT, 0.9f, 0.4f);
                }
                // Dissipate at the end
                if (tick >= config.getDurationTicks() - 20) {
                    int rd = tick - (config.getDurationTicks() - 20);
                    if (rd == 0) {
                        for (BlockDisplayHandle p : anchor) setTranslation(p, -0.5f, -2.5f, -0.5f, 15);
                    }
                    if (rd == 16) {
                        for (BlockDisplayHandle p : anchor) setScale(p, 0f, 0f, 0f, 4);
                        for (BlockDisplayHandle h : chain) setScale(h, 0f, 0f, 0f, 4);
                    }
                }
            }
        }

        private void rebuildAnchor() {
            Location c = impactGround;
            // Ring (4 chains @ idx 0..3)
            for (int i = 0; i < 4; i++) {
                double a = (Math.PI * 2 * i) / 4;
                anchor.get(i).entity().teleport(c.clone().add(Math.cos(a) * 0.25, anchorY + 1.6, Math.sin(a) * 0.25));
            }
            // Shank (idx 4)
            anchor.get(4).entity().teleport(c.clone().add(0, anchorY + 0.8, 0));
            // Stock crossbars (idx 5..6)
            anchor.get(5).entity().teleport(c.clone().add(-0.35, anchorY + 1.3, 0));
            anchor.get(6).entity().teleport(c.clone().add(0.35, anchorY + 1.3, 0));
            // Fluke arms (idx 7..10)
            double[][] fluke = {{-0.5, -0.4}, {-0.7, -0.6}, {0.5, -0.4}, {0.7, -0.6}};
            for (int i = 0; i < 4; i++) {
                anchor.get(7 + i).entity().teleport(c.clone().add(fluke[i][0], anchorY + fluke[i][1], 0));
            }
            // Fluke tips (idx 11..12)
            anchor.get(11).entity().teleport(c.clone().add(-0.78, anchorY - 0.75, 0));
            anchor.get(12).entity().teleport(c.clone().add(0.78, anchorY - 0.75, 0));
            // 10 chains above
            for (int i = 0; i < chain.size(); i++) {
                chain.get(i).entity().teleport(c.clone().add(0, anchorY + 2.2 + i * 0.5, 0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainAnchorFall(plugin); }
    }

    // ================================================================
    // #17 — IRON SPIKE SHOWER — "The Barbs"
    // 30 blocks (15 spikes × 2 displays). Multi-spot impact.
    // ================================================================
    public static class IronSpikeShower extends BlockDisplayAttack {
        private static final int SPIKE_COUNT = 15;
        private final List<SpikeHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> warnings = new ArrayList<>();

        private static class SpikeHandle {
            BlockDisplayHandle shaft;
            BlockDisplayHandle tip;
            Location ground;
            float tilt; // single-axis rotation
            int axis; // 0=X, 1=Z
            double y = 9.0;
            boolean impacted = false;
            int impactTick = -1;
        }

        public IronSpikeShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_spike_shower", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(200.0);
            config.setImpactRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(240);
            config.setChance(7.0);
            config.setEnabled(true);
            config.setDesignType("Multi-spot impact (zigzag through gaps)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            java.util.Random rnd = new java.util.Random();

            for (int i = 0; i < SPIKE_COUNT; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double r = rnd.nextDouble() * 3.0;
                Location ground = center.clone().add(Math.cos(a) * r, 0, Math.sin(a) * r);

                SpikeHandle sh = new SpikeHandle();
                sh.ground = ground;
                sh.tilt = (float) Math.toRadians((rnd.nextDouble() - 0.5) * 26);
                sh.axis = rnd.nextInt(2);

                // Warning slab tiny
                BlockDisplayHandle warn = displayBuilder.spawnBlock(ground.clone().add(0, 0.05, 0), Material.RED_CONCRETE);
                warn.scale(0.4f, 0.01f, 0.4f).glow(255, 30, 30).interpolation(0, 0);
                spawnedEntities.add(warn.entity());
                warnings.add(warn);

                sh.shaft = displayBuilder.spawnBlock(ground.clone().add(0, 9, 0), Material.IRON_BLOCK);
                sh.shaft.scale(0.11f, 0.55f, 0.11f).glow(200, 200, 210).interpolation(2, 0);
                setRotation(sh.shaft, sh.tilt, sh.axis == 0 ? 1f : 0f, 0f, sh.axis == 0 ? 0f : 1f, 0);
                spawnedEntities.add(sh.shaft.entity());

                sh.tip = displayBuilder.spawnBlock(ground.clone().add(0, 9 - 0.35, 0), Material.NETHERITE_BLOCK);
                sh.tip.scale(0.07f, 0.2f, 0.07f).glow(60, 60, 70).interpolation(2, 0);
                setRotation(sh.tip, sh.tilt, sh.axis == 0 ? 1f : 0f, 0f, sh.axis == 0 ? 0f : 1f, 0);
                spawnedEntities.add(sh.tip.entity());

                spikes.add(sh);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 0.7f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 8, 0), 30, 3, 1, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Thrown velocity: fast linear (-0.5/tick, no acceleration)
            for (SpikeHandle sh : spikes) {
                if (sh.impacted) {
                    int dt = tick - sh.impactTick;
                    if (dt == 45) {
                        // Shake
                        setRotation(sh.shaft, sh.tilt + (float) Math.toRadians(3), sh.axis == 0 ? 1f : 0f, 0f, sh.axis == 0 ? 0f : 1f, 4);
                    }
                    if (dt == 49) {
                        setRotation(sh.shaft, sh.tilt, sh.axis == 0 ? 1f : 0f, 0f, sh.axis == 0 ? 0f : 1f, 4);
                    }
                    if (dt == 60) {
                        setScale(sh.shaft, 0f, 0f, 0f, 5);
                        setScale(sh.tip, 0f, 0f, 0f, 5);
                    }
                    continue;
                }
                sh.y -= 0.5;
                sh.shaft.entity().teleport(sh.ground.clone().add(0, sh.y, 0));
                sh.tip.entity().teleport(sh.ground.clone().add(0, sh.y - 0.35, 0));
                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.SMOKE, sh.ground.clone().add(0, sh.y, 0), 1, 0.05, 0.1, 0.05, 0.01);
                }
                if (sh.y <= 0.0) {
                    sh.impacted = true;
                    sh.impactTick = tick;
                    // Embed: lower by 0.12 into ground
                    sh.shaft.entity().teleport(sh.ground.clone().add(0, -0.12, 0));
                    sh.tip.entity().teleport(sh.ground.clone().add(0, -0.47, 0));
                    triggerImpactDamage(sh.ground.clone());
                    w.spawnParticle(Particle.BLOCK, sh.ground, 12, 0.5, 0.2, 0.5, 0.2,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, sh.ground, 6, 0.4, 0.1, 0.4, 0.2);
                    DisplayBuilder.playSound(sh.ground, Sound.BLOCK_METAL_HIT, 1.0f, 0.9f + (float) Math.random() * 0.3f);
                }
            }

            // Full-salvo sound when about half are landed and tick 30 reached
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_HURT, 1.4f, 0.7f);
            }

            // Clear warnings after first wave
            if (tick == 25) {
                for (BlockDisplayHandle w2 : warnings) setScale(w2, 0f, 0f, 0f, 4);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IronSpikeShower(plugin); }
    }

    // ================================================================
    // #18 — WRECKING BALL CRATER — "The Slam"
    // 32 blocks. Massive impact (large radius, get clear early).
    // ================================================================
    public static class WreckingBallCrater extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ball = new ArrayList<>(); // 15-block sphere
        private final List<BlockDisplayHandle> chain = new ArrayList<>(); // 10
        private final List<BlockDisplayHandle> warnRing = new ArrayList<>(); // 12 slabs
        private final List<BlockDisplayHandle> shockwave = new ArrayList<>(); // 8 burst slabs
        private Location impactGround;
        private double ballY = 26.0;
        private double velocity = -0.08;
        private boolean impacted = false;
        private int impactTick = -1;

        public WreckingBallCrater(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wrecking_ball_crater", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(500.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(100);
            config.setCooldownTicks(320);
            config.setChance(4.0);
            config.setTracksPlayer(true);
            config.setEnabled(true);
            config.setDesignType("Massive impact (large radius, get clear early)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            impactGround = center.clone();

            // 12-slab warning circle on ground (RED_CONCRETE)
            for (int i = 0; i < 12; i++) {
                double a = (Math.PI * 2 * i) / 12;
                Location loc = center.clone().add(Math.cos(a) * 2.2, 0.05, Math.sin(a) * 2.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.RED_CONCRETE);
                h.scale(0.6f, 0.01f, 0.1f).glow(255, 30, 30).interpolation(0, 0);
                setRotation(h, (float) a + (float) Math.PI / 2, 0f, 1f, 0f, 0);
                spawnedEntities.add(h.entity());
                warnRing.add(h);
            }

            // 15-block sphere (NETHERITE_BLOCK core + IRON_BLOCK studs)
            // core
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, 26, 0), Material.NETHERITE_BLOCK);
            core.scale(1.4f, 1.4f, 1.4f).glow(60, 60, 70).interpolation(3, 0);
            spawnedEntities.add(core.entity());
            ball.add(core);
            // 14 studs around using sphere distribution
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 14; i++) {
                double y = 1 - (2.0 * i / 13.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * 0.95;
                double z = Math.sin(theta) * radiusAtY * 0.95;
                Material m = (i % 2 == 0) ? Material.IRON_BLOCK : Material.GRAY_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 26 + y * 0.95, z), m);
                h.scale(0.45f, 0.45f, 0.45f).glow(120, 120, 130).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                ball.add(h);
            }

            // 10 chains above
            for (int i = 0; i < 10; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(0, 26 + 1.5 + i * 0.55, 0), Material.CHAIN);
                h.scale(0.22f, 0.5f, 0.22f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                chain.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.9f, 0.3f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.4f, 0.3f);
            w.spawnParticle(Particle.LARGE_SMOKE, center.clone().add(0, 26, 0), 30, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (!impacted) {
                // Warning circle pulse
                if (tick % 3 == 0) {
                    float pulse = (float) (1.0 + Math.sin(tick * 0.3) * 0.06);
                    for (BlockDisplayHandle h : warnRing) setScale(h, 0.6f * pulse, 0.01f, 0.1f * pulse, 3);
                    DisplayBuilder.particleRing(impactGround.clone().add(0, 0.2, 0), config.getImpactRadius(),
                            Particle.SOUL_FIRE_FLAME, 48, null);
                }

                // Acceleration: -0.06/tick squared, cap velocity at -3.5
                velocity -= 0.06;
                if (velocity < -3.5) velocity = -3.5;
                ballY += velocity;

                rebuildBall();

                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.BLOCK, impactGround.clone().add(0, ballY, 0), 8, 0.7, 0.7, 0.7, 0.05,
                            Material.NETHERITE_BLOCK.createBlockData());
                }
                if (tick == 15 || tick == 30 || tick == 45) {
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.4f);
                }

                if (ballY <= 0.2) {
                    impacted = true;
                    impactTick = tick;
                    ballY = 0;
                    rebuildBall();
                    triggerImpactDamage(impactGround.clone());
                    w.spawnParticle(Particle.BLOCK, impactGround, 80, 3.5, 0.8, 3.5, 0.5,
                            Material.NETHERITE_BLOCK.createBlockData());
                    w.spawnParticle(Particle.BLOCK, impactGround, 60, 4.5, 0.5, 4.5, 0.4,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, impactGround, 50, 3.5, 0.7, 3.5, 0.5);
                    w.spawnParticle(Particle.LARGE_SMOKE, impactGround.clone().add(0, 0.5, 0), 40, 3, 0.6, 3, 0.08);
                    w.spawnParticle(Particle.EXPLOSION, impactGround.clone().add(0, 0.3, 0), 5, 2.0, 0.3, 2.0, 0);
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.3f);
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.35f);
                    DisplayBuilder.playSound(impactGround, Sound.BLOCK_ANVIL_LAND, 2.0f, 0.4f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.8f, 0.4f);
                    DisplayBuilder.playSound(impactGround, Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.5f);
                    // Ball expands
                    for (BlockDisplayHandle h : ball) {
                        Vector3f sc = h.entity().getTransformation().getScale();
                        setScale(h, sc.x * 1.35f, sc.y * 1.35f, sc.z * 1.35f, 2);
                    }
                    // Spawn 8 shockwave NETHERITE slabs flying outward
                    for (int i = 0; i < 8; i++) {
                        double a = (Math.PI * 2 * i) / 8;
                        BlockDisplayHandle s = displayBuilder.spawnBlock(impactGround.clone().add(0, 0.1, 0), Material.NETHERITE_BLOCK);
                        s.scale(0.7f, 0.04f, 0.08f).glow(60, 60, 70).interpolation(10, 0);
                        setRotation(s, (float) a, 0f, 1f, 0f, 0);
                        spawnedEntities.add(s.entity());
                        shockwave.add(s);
                    }
                    // Hide warning ring
                    for (BlockDisplayHandle wr : warnRing) setScale(wr, 0f, 0f, 0f, 3);
                }
            } else {
                int dt = tick - impactTick;
                if (dt == 2) {
                    // Collapse ball
                    for (BlockDisplayHandle h : ball) setScale(h, 0f, 0f, 0f, 4);
                    for (BlockDisplayHandle h : chain) setScale(h, 0f, 0f, 0f, 4);
                }
                if (dt <= 10) {
                    // Shockwave slabs fly outward
                    double r = (dt / 10.0) * 5.0;
                    for (int i = 0; i < shockwave.size(); i++) {
                        double a = (Math.PI * 2 * i) / 8;
                        shockwave.get(i).entity().teleport(impactGround.clone().add(Math.cos(a) * r, 0.2, Math.sin(a) * r));
                    }
                }
                if (dt == 12) {
                    for (BlockDisplayHandle s : shockwave) setScale(s, 0f, 0f, 0f, 6);
                }
            }
        }

        private void rebuildBall() {
            Location c = impactGround;
            ball.get(0).entity().teleport(c.clone().add(0, ballY, 0));
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 14; i++) {
                double y = 1 - (2.0 * i / 13.0);
                double radiusAtY = Math.sqrt(1 - y * y);
                double theta = goldenAngle * i;
                double x = Math.cos(theta) * radiusAtY * 0.95;
                double z = Math.sin(theta) * radiusAtY * 0.95;
                ball.get(1 + i).entity().teleport(c.clone().add(x, ballY + y * 0.95, z));
            }
            for (int i = 0; i < chain.size(); i++) {
                chain.get(i).entity().teleport(c.clone().add(0, ballY + 1.5 + i * 0.55, 0));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new WreckingBallCrater(plugin); }
    }

    // ================================================================
    // #19 — CHAIN NET DESCENT — "The Catch"
    // 48 blocks. Spreading net (escape before fully descends).
    // ================================================================
    public static class ChainNetDescent extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> nodes = new ArrayList<>();        // 16 nodes
        private final List<BlockDisplayHandle> spans = new ArrayList<>();        // 24 chain spans
        private final List<BlockDisplayHandle> cornerWeights = new ArrayList<>();// 4
        private final List<BlockDisplayHandle> edgeChains = new ArrayList<>();   // 4×2=8 chains
        private Location netCenter;
        private double netY = 10.0;
        private boolean landed = false;

        public ChainNetDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_net_descent", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(260.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setChance(5.0);
            config.setEnabled(true);
            config.setDesignType("Spreading net (escape before fully descends)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            netCenter = center.clone();

            // 4×4 grid of IRON_BLOCK nodes (1.2 spacing → 3.6 width)
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double x = -1.8 + gx * 1.2;
                    double z = -1.8 + gz * 1.2;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 10, z), Material.IRON_BLOCK);
                    h.scale(0.12f, 0.12f, 0.12f).glow(220, 220, 230).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    nodes.add(h);
                }
            }

            // 24 chain spans: horizontal X (12) + horizontal Z (12) — simplified as connecting slabs
            // Row spans (along X): 4 rows × 3 = 12
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 3; col++) {
                    double x = -1.2 + col * 1.2;
                    double z = -1.8 + row * 1.2;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 10, z), Material.CHAIN);
                    h.scale(1.0f, 0.08f, 0.08f).glow(140, 140, 150).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    spans.add(h);
                }
            }
            // Col spans (along Z): 4 cols × 3 = 12
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 3; row++) {
                    double x = -1.8 + col * 1.2;
                    double z = -1.2 + row * 1.2;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(x, 10, z), Material.CHAIN);
                    h.scale(0.08f, 0.08f, 1.0f).glow(140, 140, 150).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    spans.add(h);
                }
            }

            // 4 corner weights
            double[][] cornerOff = {{-1.8, 0, -1.8}, {1.8, 0, -1.8}, {-1.8, 0, 1.8}, {1.8, 0, 1.8}};
            for (double[] off : cornerOff) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center.clone().add(off[0], 10 - 0.3, off[2]), Material.IRON_BLOCK);
                h.scale(0.25f, 0.25f, 0.25f).glow(200, 200, 210).interpolation(20, 0);
                spawnedEntities.add(h.entity());
                cornerWeights.add(h);
            }
            // 4×2 edge chains rising to ceiling
            for (double[] off : cornerOff) {
                for (int j = 0; j < 2; j++) {
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(off[0], 10 + 0.6 + j * 0.5, off[2]), Material.CHAIN);
                    h.scale(0.14f, 0.45f, 0.14f).glow(140, 140, 150).interpolation(20, 0);
                    spawnedEntities.add(h.entity());
                    edgeChains.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.3f, 0.7f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Net descends slowly at 0.08 blocks/tick until netY = 1.0 (head height)
            if (!landed) {
                netY -= 0.08;
                if (netY <= 1.0) {
                    netY = 1.0;
                    landed = true;
                    DisplayBuilder.playSound(netCenter, Sound.BLOCK_METAL_HIT, 1.0f, 0.4f);
                    DisplayBuilder.playSound(netCenter, Sound.BLOCK_METAL_HIT, 1.0f, 0.5f);
                    DisplayBuilder.playSound(netCenter, Sound.BLOCK_METAL_HIT, 1.0f, 0.6f);
                    DisplayBuilder.playSound(netCenter, Sound.BLOCK_METAL_HIT, 1.0f, 0.7f);
                    w.spawnParticle(Particle.BLOCK, netCenter.clone().add(0, 1, 0), 60, 2.0, 0.1, 2.0, 0.1,
                            Material.IRON_BLOCK.createBlockData());
                }
                rebuildNet();
            }

            // Wave: opposite corners oscillate Y ±0.08 (30t period)
            if (tick > 5 && tick % 2 == 0) {
                double wave = Math.sin(tick * 0.21) * 0.08;
                if (!cornerWeights.isEmpty()) {
                    cornerWeights.get(0).entity().teleport(netCenter.clone().add(-1.8, netY - 0.3 + wave, -1.8));
                    cornerWeights.get(3).entity().teleport(netCenter.clone().add(1.8, netY - 0.3 + wave, 1.8));
                    cornerWeights.get(1).entity().teleport(netCenter.clone().add(1.8, netY - 0.3 - wave, -1.8));
                    cornerWeights.get(2).entity().teleport(netCenter.clone().add(-1.8, netY - 0.3 - wave, 1.8));
                }
            }

            // Particles + soft sounds during descent
            if (!landed && tick % 4 == 0) {
                w.spawnParticle(Particle.BLOCK, netCenter.clone().add(0, netY, 0), 8, 1.8, 0.1, 1.8, 0.02,
                        Material.CHAIN.createBlockData());
                DisplayBuilder.playSound(netCenter, Sound.BLOCK_CHAIN_FALL, 0.5f, 0.7f);
            }
            if (tick % 10 == 0) {
                for (BlockDisplayHandle cw : cornerWeights) {
                    w.spawnParticle(Particle.ELECTRIC_SPARK, cw.entity().getLocation(), 2, 0.15, 0.15, 0.15, 0.01);
                }
            }

            // Dissipate
            if (tick >= config.getDurationTicks() - 20) {
                int dt = tick - (config.getDurationTicks() - 20);
                if (dt == 0) {
                    for (BlockDisplayHandle h : nodes) setScale(h, 0f, 0f, 0f, 15);
                    for (BlockDisplayHandle h : spans) setScale(h, 0f, 0f, 0f, 15);
                    for (BlockDisplayHandle h : cornerWeights) setScale(h, 0f, 0f, 0f, 15);
                    for (BlockDisplayHandle h : edgeChains) setScale(h, 0f, 0f, 0f, 15);
                    DisplayBuilder.playSound(netCenter, Sound.BLOCK_CHAIN_FALL, 1.2f, 0.5f);
                }
            }
        }

        private void rebuildNet() {
            Location c = netCenter;
            // Nodes
            int idx = 0;
            for (int gx = 0; gx < 4; gx++) {
                for (int gz = 0; gz < 4; gz++) {
                    double x = -1.8 + gx * 1.2;
                    double z = -1.8 + gz * 1.2;
                    nodes.get(idx++).entity().teleport(c.clone().add(x, netY, z));
                }
            }
            // Row spans
            idx = 0;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 3; col++) {
                    double x = -1.2 + col * 1.2;
                    double z = -1.8 + row * 1.2;
                    spans.get(idx++).entity().teleport(c.clone().add(x, netY, z));
                }
            }
            // Col spans
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 3; row++) {
                    double x = -1.8 + col * 1.2;
                    double z = -1.2 + row * 1.2;
                    spans.get(idx++).entity().teleport(c.clone().add(x, netY, z));
                }
            }
            // Corner weights & edge chains
            double[][] cornerOff = {{-1.8, 0, -1.8}, {1.8, 0, -1.8}, {-1.8, 0, 1.8}, {1.8, 0, 1.8}};
            for (int i = 0; i < 4; i++) {
                cornerWeights.get(i).entity().teleport(c.clone().add(cornerOff[i][0], netY - 0.3, cornerOff[i][2]));
            }
            int eIdx = 0;
            for (double[] off : cornerOff) {
                for (int j = 0; j < 2; j++) {
                    edgeChains.get(eIdx++).entity().teleport(c.clone().add(off[0], netY + 0.6 + j * 0.5, off[2]));
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainNetDescent(plugin); }
    }

    // ================================================================
    // #20 — CHAIN PLUMB ARRAY — "The Grid"
    // 36 blocks (9 plumbs × 4 displays). Multi-spot grid drop (find the safe cell).
    // ================================================================
    public static class ChainPlumbArray extends BlockDisplayAttack {
        private final List<PlumbHandle> plumbs = new ArrayList<>();
        private final List<BlockDisplayHandle> warnings = new ArrayList<>();

        private static class PlumbHandle {
            BlockDisplayHandle core;
            BlockDisplayHandle tip;
            BlockDisplayHandle chainA;
            BlockDisplayHandle chainB;
            Location ground;
            int spawnTick;
            double y = 16.0;
            double vel = -0.3;
            boolean impacted = false;
            int impactTick = -1;
            double speedJitter;
        }

        public ChainPlumbArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_plumb_array", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(280.0);
            config.setImpactRadius(1.8);
            config.setDurationTicks(100);
            config.setCooldownTicks(240);
            config.setChance(6.0);
            config.setEnabled(true);
            config.setDesignType("Multi-spot grid drop (find the safe cell)");
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0); center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;
            java.util.Random rnd = new java.util.Random();

            // 9 plumbs in 3x3 grid, 1.8 spacing
            // Drop order: weight5 (center) tick 0, weights2/4/6/8 tick 12, weights1/3/7/9 tick 24
            // Indexing: 0..8, where 0=NW,1=N,2=NE,3=W,4=center,5=E,6=SW,7=S,8=SE
            int[] dropOrder = {24, 12, 24, 12, 0, 12, 24, 12, 24};

            for (int i = 0; i < 9; i++) {
                int gx = i % 3;
                int gz = i / 3;
                double x = (gx - 1) * 1.8;
                double z = (gz - 1) * 1.8;
                Location ground = center.clone().add(x, 0, z);

                PlumbHandle ph = new PlumbHandle();
                ph.ground = ground;
                ph.spawnTick = dropOrder[i];
                ph.speedJitter = rnd.nextDouble() * 0.02;

                // Warning slab
                BlockDisplayHandle warn = displayBuilder.spawnBlock(ground.clone().add(0, 0.05, 0), Material.RED_CONCRETE);
                warn.scale(0.5f, 0.01f, 0.5f).glow(255, 30, 30).interpolation(0, ph.spawnTick);
                spawnedEntities.add(warn.entity());
                warnings.add(warn);

                // Plumb body parts
                ph.core = displayBuilder.spawnBlock(ground.clone().add(0, 16, 0), Material.IRON_BLOCK);
                ph.core.scale(0.48f, 0.55f, 0.48f).glow(200, 200, 210).interpolation(3, 0);
                spawnedEntities.add(ph.core.entity());

                ph.tip = displayBuilder.spawnBlock(ground.clone().add(0, 16 - 0.45, 0), Material.NETHERITE_BLOCK);
                ph.tip.scale(0.18f, 0.28f, 0.18f).glow(60, 60, 70).interpolation(3, 0);
                spawnedEntities.add(ph.tip.entity());

                ph.chainA = displayBuilder.spawnBlock(ground.clone().add(0, 16 + 0.5, 0), Material.CHAIN);
                ph.chainA.scale(0.14f, 0.45f, 0.14f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(ph.chainA.entity());

                ph.chainB = displayBuilder.spawnBlock(ground.clone().add(0, 16 + 1.0, 0), Material.CHAIN);
                ph.chainB.scale(0.14f, 0.45f, 0.14f).glow(140, 140, 150).interpolation(3, 0);
                spawnedEntities.add(ph.chainB.entity());

                plumbs.add(ph);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_FALL, 1.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.8f, 0.5f);
            w.spawnParticle(Particle.SMOKE, center.clone().add(0, 14, 0), 40, 3, 1, 3, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (PlumbHandle ph : plumbs) {
                if (tick < ph.spawnTick) continue;
                if (ph.impacted) {
                    int dt = tick - ph.impactTick;
                    if (dt == 4) {
                        setScale(ph.core, 0f, 0f, 0f, 4);
                        setScale(ph.tip, 0f, 0f, 0f, 4);
                        setScale(ph.chainA, 0f, 0f, 0f, 4);
                        setScale(ph.chainB, 0f, 0f, 0f, 4);
                    }
                    continue;
                }

                ph.vel -= 0.04 + ph.speedJitter;
                if (ph.vel < -3.0) ph.vel = -3.0;
                ph.y += ph.vel;

                ph.core.entity().teleport(ph.ground.clone().add(0, ph.y, 0));
                ph.tip.entity().teleport(ph.ground.clone().add(0, ph.y - 0.45, 0));
                ph.chainA.entity().teleport(ph.ground.clone().add(0, ph.y + 0.5, 0));
                ph.chainB.entity().teleport(ph.ground.clone().add(0, ph.y + 1.0, 0));

                if (tick % 2 == 0) {
                    w.spawnParticle(Particle.BLOCK, ph.ground.clone().add(0, ph.y, 0), 3, 0.2, 0.2, 0.2, 0.02,
                            Material.IRON_BLOCK.createBlockData());
                }

                if (ph.y <= 0.1) {
                    ph.impacted = true;
                    ph.impactTick = tick;
                    triggerImpactDamage(ph.ground.clone());
                    w.spawnParticle(Particle.BLOCK, ph.ground, 20, 0.8, 0.3, 0.8, 0.2,
                            Material.IRON_BLOCK.createBlockData());
                    w.spawnParticle(Particle.CRIT, ph.ground, 10, 0.6, 0.2, 0.6, 0.2);
                    w.spawnParticle(Particle.SMOKE, ph.ground.clone().add(0, 0.3, 0), 6, 0.5, 0.2, 0.5, 0.03);
                    DisplayBuilder.playSound(ph.ground, Sound.BLOCK_ANVIL_LAND, 1.2f, 0.6f + (float) Math.random() * 0.2f);
                }
            }

            // Hide warnings after second-wave completes
            if (tick == 30) {
                for (BlockDisplayHandle wn : warnings) setScale(wn, 0f, 0f, 0f, 4);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ChainPlumbArray(plugin); }
    }
}
