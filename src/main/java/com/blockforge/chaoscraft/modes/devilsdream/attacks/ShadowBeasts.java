package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — SHADOW BEAST ATTACKS
 * 13 creature-shaped BlockDisplay attacks featuring demonic beasts,
 * nightmare hounds, skull spiders, and abyssal leviathans.
 *
 * Each creature is built from 10+ block displays to form a recognizable shape.
 * Color palette: black_concrete, coal_block, obsidian, crying_obsidian,
 * blackstone, tinted_glass, wither_rose, bone_block
 */
public final class ShadowBeasts {

    private ShadowBeasts() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new NightmareHound(plugin));
        registry.register(new DemonWings(plugin));
        registry.register(new SkullSpider(plugin));
        registry.register(new SerpentCoil(plugin));
        registry.register(new GraspingHand(plugin));
        registry.register(new NightmareCentipede(plugin));
        registry.register(new DemonicSkull(plugin));
        registry.register(new ShadowRaven(plugin));
        registry.register(new NightmareBear(plugin));
        registry.register(new AbyssalJellyfish(plugin));
        registry.register(new BoneDragon(plugin));
        registry.register(new DemonHorns(plugin));
        registry.register(new SoulLeviathan(plugin));
    }

    // ================================================================
    // 1. NIGHTMARE HOUND — 12-block dog/wolf shape: body (3 long),
    //    4 legs, head, snout, tail, 2 ears. Charges toward player.
    //    Made of coal blocks with red glowing eyes.
    // ================================================================
    public static class NightmareHound extends BlockDisplayAttack {
        private BlockDisplayHandle body1, body2, body3;
        private BlockDisplayHandle head, snout;
        private BlockDisplayHandle legFL, legFR, legBL, legBR;
        private BlockDisplayHandle tail, earL, earR;
        private double chargeAngle = 0;
        private double circleRadius = 8.0;

        public NightmareHound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_hound", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location spawn = center.clone().add(circleRadius, 0, 0);

            // Body segments (3 blocks, elongated)
            body1 = spawnPart(spawn, 0, 1.5, 0, Material.COAL_BLOCK, 1.5f, 1.2f, 1.0f);
            body2 = spawnPart(spawn, 0, 1.5, -1.2, Material.BLACK_CONCRETE, 1.3f, 1.0f, 1.2f);
            body3 = spawnPart(spawn, 0, 1.5, -2.4, Material.COAL_BLOCK, 1.2f, 0.9f, 1.0f);

            // Head (larger, forward)
            head = spawnPart(spawn, 0, 2.0, 1.0, Material.COAL_BLOCK, 1.4f, 1.4f, 1.2f);
            snout = spawnPart(spawn, 0, 1.8, 1.8, Material.BLACK_CONCRETE, 0.8f, 0.6f, 0.8f);

            // Ears
            earL = spawnPart(spawn, -0.5, 2.8, 1.0, Material.COAL_BLOCK, 0.3f, 0.6f, 0.3f);
            earR = spawnPart(spawn, 0.5, 2.8, 1.0, Material.COAL_BLOCK, 0.3f, 0.6f, 0.3f);

            // 4 Legs
            legFL = spawnPart(spawn, -0.5, 0.5, 0.3, Material.BLACK_CONCRETE, 0.4f, 1.2f, 0.4f);
            legFR = spawnPart(spawn, 0.5, 0.5, 0.3, Material.BLACK_CONCRETE, 0.4f, 1.2f, 0.4f);
            legBL = spawnPart(spawn, -0.5, 0.5, -2.0, Material.BLACK_CONCRETE, 0.4f, 1.2f, 0.4f);
            legBR = spawnPart(spawn, 0.5, 0.5, -2.0, Material.BLACK_CONCRETE, 0.4f, 1.2f, 0.4f);

            // Tail
            tail = spawnPart(spawn, 0, 2.0, -3.2, Material.BLACK_CONCRETE, 0.25f, 0.25f, 1.2f);

            DisplayBuilder.playSound(spawn, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnPart(Location base, double ox, double oy, double oz,
                                             Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(30, 10, 10).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Circle around player, getting closer
            chargeAngle += 0.05;
            if (circleRadius > 3.0) circleRadius -= 0.02;

            double cx = Math.cos(chargeAngle) * circleRadius;
            double cz = Math.sin(chargeAngle) * circleRadius;
            Location houndPos = c.clone().add(cx, 0, cz);

            // Face direction of movement
            double facingAngle = chargeAngle + Math.PI / 2;
            float cosF = (float) Math.cos(facingAngle);
            float sinF = (float) Math.sin(facingAngle);

            // Galloping leg animation
            float legBob = (float) Math.sin(ticksAlive * 0.3) * 0.4f;

            // Update all parts relative to hound position
            movePart(body1, houndPos, 0, 1.5, 0);
            movePart(body2, houndPos, -sinF * 1.2, 1.5, cosF * 1.2);
            movePart(body3, houndPos, -sinF * 2.4, 1.5, cosF * 2.4);
            movePart(head, houndPos, sinF * 1.0, 2.0, -cosF * 1.0);
            movePart(snout, houndPos, sinF * 1.8, 1.8, -cosF * 1.8);
            movePart(earL, houndPos, sinF * 1.0 - cosF * 0.5, 2.8, -cosF * 1.0 - sinF * 0.5);
            movePart(earR, houndPos, sinF * 1.0 + cosF * 0.5, 2.8, -cosF * 1.0 + sinF * 0.5);
            movePart(legFL, houndPos, cosF * 0.5 + sinF * 0.3, 0.5 + legBob, -sinF * 0.5 + cosF * 0.3);
            movePart(legFR, houndPos, -cosF * 0.5 + sinF * 0.3, 0.5 - legBob, sinF * 0.5 + cosF * 0.3);
            movePart(legBL, houndPos, cosF * 0.5 - sinF * 2.0, 0.5 - legBob, -sinF * 0.5 - cosF * 2.0);
            movePart(legBR, houndPos, -cosF * 0.5 - sinF * 2.0, 0.5 + legBob, sinF * 0.5 - cosF * 2.0);
            movePart(tail, houndPos, -sinF * 3.2, 2.0, cosF * 3.2);

            // Shadow particles
            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, houndPos.clone().add(0, 0.5, 0),
                        5, 0.5, 0.2, 0.5, 0.02);
            }

            // Growling
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(houndPos, Sound.ENTITY_WOLF_GROWL, 0.7f, 0.3f);
            }
        }

        private void movePart(BlockDisplayHandle part, Location base, double ox, double oy, double oz) {
            part.entity().teleport(base.clone().add(ox, oy, oz));
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareHound(plugin); }
    }

    // ================================================================
    // 2. DEMON WINGS — 2 large wing structures (7 blocks each = 14)
    //    that flap open and closed. Made of black concrete with
    //    tinted glass membranes. Hovering above player.
    // ================================================================
    public static class DemonWings extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private float flapAngle = 0;

        public DemonWings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demon_wings", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location wingBase = center.clone().add(0, 5, 0);

            // Left wing — bone structure + membrane
            // Arm bones (3 segments)
            leftWing.add(spawnWingPart(wingBase, -1.0, 0, 0, Material.COAL_BLOCK, 2.0f, 0.4f, 0.4f));
            leftWing.add(spawnWingPart(wingBase, -3.0, 0.5, 0, Material.COAL_BLOCK, 2.0f, 0.3f, 0.3f));
            leftWing.add(spawnWingPart(wingBase, -5.0, 0.3, 0, Material.BLACK_CONCRETE, 1.5f, 0.25f, 0.25f));
            // Membrane (4 panels)
            leftWing.add(spawnWingPart(wingBase, -1.5, -0.5, 0, Material.TINTED_GLASS, 1.5f, 1.8f, 0.1f));
            leftWing.add(spawnWingPart(wingBase, -3.0, -0.3, 0, Material.TINTED_GLASS, 1.5f, 2.2f, 0.1f));
            leftWing.add(spawnWingPart(wingBase, -4.5, -0.5, 0, Material.TINTED_GLASS, 1.5f, 1.8f, 0.1f));
            leftWing.add(spawnWingPart(wingBase, -5.5, -0.3, 0, Material.TINTED_GLASS, 1.0f, 1.2f, 0.1f));

            // Right wing — mirror
            rightWing.add(spawnWingPart(wingBase, 1.0, 0, 0, Material.COAL_BLOCK, 2.0f, 0.4f, 0.4f));
            rightWing.add(spawnWingPart(wingBase, 3.0, 0.5, 0, Material.COAL_BLOCK, 2.0f, 0.3f, 0.3f));
            rightWing.add(spawnWingPart(wingBase, 5.0, 0.3, 0, Material.BLACK_CONCRETE, 1.5f, 0.25f, 0.25f));
            rightWing.add(spawnWingPart(wingBase, 1.5, -0.5, 0, Material.TINTED_GLASS, 1.5f, 1.8f, 0.1f));
            rightWing.add(spawnWingPart(wingBase, 3.0, -0.3, 0, Material.TINTED_GLASS, 1.5f, 2.2f, 0.1f));
            rightWing.add(spawnWingPart(wingBase, 4.5, -0.5, 0, Material.TINTED_GLASS, 1.5f, 1.8f, 0.1f));
            rightWing.add(spawnWingPart(wingBase, 5.5, -0.3, 0, Material.TINTED_GLASS, 1.0f, 1.2f, 0.1f));

            DisplayBuilder.playSound(wingBase, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.4f);
        }

        private BlockDisplayHandle spawnWingPart(Location base, double ox, double oy, double oz,
                                                 Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(40, 10, 50).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location wingBase = c.clone().add(0, 5, 0);

            // Flap animation
            flapAngle = (float) Math.sin(ticksAlive * 0.08) * 0.6f;

            // Left wing parts — rotate downward when flapping
            for (int i = 0; i < leftWing.size(); i++) {
                double baseX = -(1.0 + i * 0.7);
                double baseY = (i < 3) ? (i == 1 ? 0.5 : (i == 2 ? 0.3 : 0)) : -(0.3 + (i - 3) * 0.1);
                // Apply flap rotation
                double rotatedY = baseY * Math.cos(flapAngle) - Math.abs(baseX) * Math.sin(flapAngle) * 0.3;
                leftWing.get(i).entity().teleport(wingBase.clone().add(baseX, rotatedY, 0));
                leftWing.get(i).rotate(-flapAngle * 0.5f, 0, 0, 1);
            }

            // Right wing — mirror
            for (int i = 0; i < rightWing.size(); i++) {
                double baseX = (1.0 + i * 0.7);
                double baseY = (i < 3) ? (i == 1 ? 0.5 : (i == 2 ? 0.3 : 0)) : -(0.3 + (i - 3) * 0.1);
                double rotatedY = baseY * Math.cos(flapAngle) - Math.abs(baseX) * Math.sin(flapAngle) * 0.3;
                rightWing.get(i).entity().teleport(wingBase.clone().add(baseX, rotatedY, 0));
                rightWing.get(i).rotate(flapAngle * 0.5f, 0, 0, 1);
            }

            // Wind particles on downstroke
            if (flapAngle < -0.3 && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, wingBase.clone().add(0, -2, 0),
                        8, 3, 0.5, 1, 0.03);
            }

            // Flap sounds
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(wingBase, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.4f);
            }

            // Shadow dust
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(wingBase, 4, 3.0, 40, 10, 50, 1.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonWings(plugin); }
    }

    // ================================================================
    // 3. SKULL SPIDER — Spider shape: body (2 blocks), head/skull (1),
    //    8 bent legs (1 each), mandibles (2) = 13 blocks.
    //    Scuttles toward player with leg animation.
    // ================================================================
    public static class SkullSpider extends BlockDisplayAttack {
        private BlockDisplayHandle body, abdomen, skull;
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private BlockDisplayHandle mandibleL, mandibleR;
        private double moveAngle = 0;
        private double distance = 7.0;

        public SkullSpider(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("skull_spider", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location spawnPos = center.clone().add(distance, 0, 0);

            // Skull (head)
            skull = spawnPart(spawnPos, 0, 1.8, 1.0, Material.BONE_BLOCK, 1.4f, 1.2f, 1.0f);
            // Body
            body = spawnPart(spawnPos, 0, 1.5, 0, Material.BLACK_CONCRETE, 1.6f, 1.0f, 1.2f);
            // Abdomen
            abdomen = spawnPart(spawnPos, 0, 1.6, -1.5, Material.COAL_BLOCK, 2.0f, 1.5f, 2.0f);

            // 8 legs (4 per side)
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    double zOff = -1.0 + i * 0.7;
                    legs.add(spawnPart(spawnPos, side * 1.5, 0.5, zOff,
                            Material.BLACK_CONCRETE, 0.25f, 1.2f, 0.25f));
                }
            }

            // Mandibles
            mandibleL = spawnPart(spawnPos, -0.4, 1.5, 1.6, Material.BONE_BLOCK, 0.2f, 0.2f, 0.6f);
            mandibleR = spawnPart(spawnPos, 0.4, 1.5, 1.6, Material.BONE_BLOCK, 0.2f, 0.2f, 0.6f);

            DisplayBuilder.playSound(spawnPos, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnPart(Location base, double ox, double oy, double oz,
                                             Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(20, 10, 10).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            moveAngle += 0.04;
            if (distance > 2.5) distance -= 0.03;

            double cx = Math.cos(moveAngle) * distance;
            double cz = Math.sin(moveAngle) * distance;
            Location pos = c.clone().add(cx, 0, cz);

            double facing = moveAngle + Math.PI / 2;
            float cosF = (float) Math.cos(facing);
            float sinF = (float) Math.sin(facing);

            // Body parts
            skull.entity().teleport(pos.clone().add(sinF * 1.0, 1.8, -cosF * 1.0));
            body.entity().teleport(pos.clone().add(0, 1.5, 0));
            abdomen.entity().teleport(pos.clone().add(-sinF * 1.5, 1.6, cosF * 1.5));

            // Scuttling legs
            for (int i = 0; i < 8; i++) {
                int side = (i < 4) ? -1 : 1;
                int legIdx = i % 4;
                double zOff = -1.0 + legIdx * 0.7;
                float legAnim = (float) Math.sin(ticksAlive * 0.4 + i * 0.8) * 0.5f;
                double lx = side * (1.5 + Math.abs(legAnim));
                double ly = 0.5 + Math.abs(legAnim) * 0.3;

                legs.get(i).entity().teleport(pos.clone().add(
                        cosF * lx - sinF * zOff, ly, sinF * lx + cosF * zOff));
                legs.get(i).rotate(legAnim * 0.3f, 0, 0, 1);
            }

            // Mandibles
            float mandibleAnim = (float) Math.sin(ticksAlive * 0.15) * 0.3f;
            mandibleL.entity().teleport(pos.clone().add(
                    sinF * 1.6 + cosF * (-0.4 - mandibleAnim), 1.5,
                    -cosF * 1.6 + sinF * (-0.4 - mandibleAnim)));
            mandibleR.entity().teleport(pos.clone().add(
                    sinF * 1.6 + cosF * (0.4 + mandibleAnim), 1.5,
                    -cosF * 1.6 + sinF * (0.4 + mandibleAnim)));

            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, pos.clone().add(0, 0.3, 0),
                        3, 0.5, 0.1, 0.5, 0.01);
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(pos, Sound.ENTITY_SPIDER_AMBIENT, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SkullSpider(plugin); }
    }

    // ================================================================
    // 4. SERPENT COIL — 16-block serpent body coiled in a spiral,
    //    slowly uncoiling and recoiling. Head has distinct jaw.
    //    Slithers in wave motion.
    // ================================================================
    public static class SerpentCoil extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private static final int SEG_COUNT = 16;

        public SerpentCoil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("serpent_coil", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SEG_COUNT; i++) {
                Material mat = (i == 0) ? Material.BONE_BLOCK // Head
                        : (i % 3 == 0) ? Material.COAL_BLOCK
                        : Material.BLACK_CONCRETE;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(center, mat);
                float scale = (i == 0) ? 1.5f : Math.max(0.5f, 1.3f - i * 0.05f);
                seg.scale(scale, scale * 0.8f, scale).glow(20, 40, 20).interpolation(2, 0);
                segments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double coilRadius = 3.0 + Math.sin(ticksAlive * 0.02) * 1.5;

            for (int i = 0; i < SEG_COUNT; i++) {
                double angle = ticksAlive * 0.04 - i * 0.4;
                double r = coilRadius - i * 0.15;
                if (r < 0.5) r = 0.5;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                double y = 1.0 + Math.sin(angle * 2 + i * 0.3) * 0.8 + i * 0.15;

                segments.get(i).entity().teleport(c.clone().add(x, y, z));
                segments.get(i).rotate((float) angle, 0, 1, 0);
            }

            // Hiss particles from head
            if (ticksAlive % 6 == 0) {
                Location headLoc = segments.get(0).entity().getLocation();
                c.getWorld().spawnParticle(Particle.SMOKE, headLoc, 3, 0.2, 0.2, 0.2, 0.02);
                DisplayBuilder.dustParticles(headLoc, 2, 0.3, 20, 80, 20, 1.0f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SerpentCoil(plugin); }
    }

    // ================================================================
    // 5. GRASPING HAND — Giant hand reaching from ground: palm (2),
    //    5 fingers (2 blocks each) = 12 blocks. Fingers close into
    //    a fist over time, crushing players inside.
    // ================================================================
    public static class GraspingHand extends BlockDisplayAttack {
        private BlockDisplayHandle palm1, palm2;
        private final List<BlockDisplayHandle> fingers = new ArrayList<>(); // 10 blocks (5 fingers × 2 segments)
        private float gripProgress = 0;

        public GraspingHand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grasping_hand", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Palm (2 blocks forming wide base)
            palm1 = displayBuilder.spawnBlock(center.clone().add(-0.8, 0.5, 0), Material.COAL_BLOCK);
            palm1.scale(2.0f, 0.8f, 2.5f).glow(30, 20, 20).interpolation(3, 0);
            spawnedEntities.add(palm1.entity());
            palm2 = displayBuilder.spawnBlock(center.clone().add(0.8, 0.5, 0), Material.COAL_BLOCK);
            palm2.scale(2.0f, 0.8f, 2.5f).glow(30, 20, 20).interpolation(3, 0);
            spawnedEntities.add(palm2.entity());

            // 5 fingers, 2 segments each
            double[] fingerX = {-1.5, -0.75, 0, 0.75, 1.5};
            for (int f = 0; f < 5; f++) {
                // Base segment
                BlockDisplayHandle base = displayBuilder.spawnBlock(
                        center.clone().add(fingerX[f], 1.5, 1.2), Material.BLACK_CONCRETE);
                base.scale(0.5f, 2.0f, 0.5f).glow(30, 20, 20).interpolation(4, 0);
                fingers.add(base);
                spawnedEntities.add(base.entity());

                // Tip segment
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        center.clone().add(fingerX[f], 3.5, 1.2), Material.OBSIDIAN);
                float tipScale = (f == 0) ? 0.6f : 0.4f; // Thumb is thicker
                tip.scale(tipScale, 1.5f, tipScale).glow(40, 20, 40).interpolation(4, 0);
                fingers.add(tip);
                spawnedEntities.add(tip.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: fingers rise (0-60)
            // Phase 2: fingers close (60-200)
            if (ticksAlive > 60 && gripProgress < 1.0f) {
                gripProgress += 0.005f;
            }

            double[] fingerX = {-1.5, -0.75, 0, 0.75, 1.5};
            for (int f = 0; f < 5; f++) {
                // Curl fingers inward (rotate toward center and down)
                float curl = gripProgress * 1.2f;
                double curledZ = 1.2 - Math.sin(curl) * 2.0;
                double curledY = 1.5 + Math.cos(curl) * 2.0 - 0.5;
                double tipCurledZ = curledZ - Math.sin(curl * 1.3) * 1.2;
                double tipCurledY = curledY + Math.cos(curl * 1.3) * 1.5 - 0.3;

                // Fingers also move toward center X
                double xShift = fingerX[f] * (1.0 - gripProgress * 0.5);

                fingers.get(f * 2).entity().teleport(c.clone().add(xShift, curledY, curledZ));
                fingers.get(f * 2).rotate(-curl, 1, 0, 0);
                fingers.get(f * 2 + 1).entity().teleport(c.clone().add(xShift, tipCurledY, tipCurledZ));
                fingers.get(f * 2 + 1).rotate(-curl * 1.3f, 1, 0, 0);
            }

            // Crushing particles when gripping
            if (gripProgress > 0.5f && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 5, 1.0, 60, 20, 20, 1.5f);
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(0, 2, 0),
                        3, 0.5, 0.5, 0.5, 0.02);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GraspingHand(plugin); }
    }

    // ================================================================
    // 6. NIGHTMARE CENTIPEDE — 18 small body segments in a long line
    //    that weaves in a sine pattern. Legs on each segment animate.
    //    Wraps around the player area.
    // ================================================================
    public static class NightmareCentipede extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bodySegments = new ArrayList<>();
        private static final int BODY_COUNT = 18;

        public NightmareCentipede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_centipede", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < BODY_COUNT; i++) {
                Material mat = (i == 0) ? Material.BONE_BLOCK
                        : (i % 2 == 0) ? Material.BLACK_CONCRETE
                        : Material.COAL_BLOCK;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(center, mat);
                float headScale = (i == 0) ? 1.0f : 0.7f;
                seg.scale(headScale, 0.5f, headScale).glow(50, 20, 20).interpolation(2, 0);
                bodySegments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_SILVERFISH_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < BODY_COUNT; i++) {
                double t = ticksAlive * 0.05 - i * 0.25;
                double x = Math.cos(t) * 5.0;
                double z = Math.sin(t * 1.3) * 5.0;
                double y = 0.5 + Math.sin(t * 3) * 0.3;

                bodySegments.get(i).entity().teleport(c.clone().add(x, y, z));
                bodySegments.get(i).rotate((float) t, 0, 1, 0);
            }

            if (ticksAlive % 5 == 0) {
                Location headLoc = bodySegments.get(0).entity().getLocation();
                c.getWorld().spawnParticle(Particle.SMOKE, headLoc, 2, 0.2, 0.1, 0.2, 0.01);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_SILVERFISH_STEP, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareCentipede(plugin); }
    }

    // ================================================================
    // 7. DEMONIC SKULL — Large skull face: cranium (4), jaw (2),
    //    eye sockets (2 black concrete), horns (2 each side = 4) = 12.
    //    Floats above player, jaw opens/closes, eyes glow.
    // ================================================================
    public static class DemonicSkull extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cranium = new ArrayList<>();
        private BlockDisplayHandle jawLeft, jawRight;
        private BlockDisplayHandle eyeLeft, eyeRight;
        private final List<BlockDisplayHandle> horns = new ArrayList<>();
        private float jawAngle = 0;

        public DemonicSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_skull", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location skullCenter = center.clone().add(0, 5, 0);

            // Cranium (4 blocks — upper skull)
            cranium.add(spawnPart(skullCenter, -0.8, 0.5, 0, Material.BONE_BLOCK, 1.8f, 1.5f, 2.0f));
            cranium.add(spawnPart(skullCenter, 0.8, 0.5, 0, Material.BONE_BLOCK, 1.8f, 1.5f, 2.0f));
            cranium.add(spawnPart(skullCenter, 0, 1.5, 0, Material.BONE_BLOCK, 3.0f, 1.0f, 2.0f));
            cranium.add(spawnPart(skullCenter, 0, 0, 0.8, Material.BONE_BLOCK, 2.5f, 1.0f, 0.5f));

            // Jaw (2 blocks)
            jawLeft = spawnPart(skullCenter, -0.6, -0.8, 0.3, Material.BONE_BLOCK, 1.2f, 0.6f, 1.0f);
            jawRight = spawnPart(skullCenter, 0.6, -0.8, 0.3, Material.BONE_BLOCK, 1.2f, 0.6f, 1.0f);

            // Eye sockets (dark insets)
            eyeLeft = spawnPart(skullCenter, -0.7, 0.3, 0.9, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.3f);
            eyeRight = spawnPart(skullCenter, 0.7, 0.3, 0.9, Material.CRYING_OBSIDIAN, 0.6f, 0.6f, 0.3f);
            eyeLeft.glow(200, 30, 30);
            eyeRight.glow(200, 30, 30);

            // Horns (2 per side, curving upward)
            horns.add(spawnPart(skullCenter, -1.8, 1.2, 0, Material.BLACKSTONE, 0.4f, 1.5f, 0.4f));
            horns.add(spawnPart(skullCenter, -2.2, 2.5, 0, Material.POLISHED_BLACKSTONE, 0.3f, 1.2f, 0.3f));
            horns.add(spawnPart(skullCenter, 1.8, 1.2, 0, Material.BLACKSTONE, 0.4f, 1.5f, 0.4f));
            horns.add(spawnPart(skullCenter, 2.2, 2.5, 0, Material.POLISHED_BLACKSTONE, 0.3f, 1.2f, 0.3f));

            DisplayBuilder.playSound(skullCenter, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnPart(Location base, double ox, double oy, double oz,
                                             Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 180, 150).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            float bob = (float) Math.sin(ticksAlive * 0.04) * 0.5f;
            Location skullCenter = c.clone().add(0, 5 + bob, 0);

            // Jaw opening/closing
            jawAngle = (float) Math.sin(ticksAlive * 0.06) * 0.4f;
            jawLeft.entity().teleport(skullCenter.clone().add(-0.6, -0.8 - jawAngle, 0.3));
            jawRight.entity().teleport(skullCenter.clone().add(0.6, -0.8 - jawAngle, 0.3));

            // Eye glow pulse
            int eyeR = 150 + (int)(Math.sin(ticksAlive * 0.1) * 55);
            eyeLeft.glow(eyeR, 20, 20);
            eyeRight.glow(eyeR, 20, 20);

            // Soul fire from eyes
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        skullCenter.clone().add(-0.7, 0.3, 1.2), 2, 0.1, 0.1, 0.1, 0.02);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        skullCenter.clone().add(0.7, 0.3, 1.2), 2, 0.1, 0.1, 0.1, 0.02);
            }

            // Dark breath from jaw when open
            if (jawAngle > 0.2f && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE,
                        skullCenter.clone().add(0, -1, 1), 5, 0.3, 0.3, 0.3, 0.03);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(skullCenter, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonicSkull(plugin); }
    }

    // ================================================================
    // 8. SHADOW RAVEN — Bird shape: body (2), head (1), 2 wings
    //    (4 each = 8), tail (1), beak (1) = 13 blocks.
    //    Swoops in circles above the player, diving periodically.
    // ================================================================
    public static class ShadowRaven extends BlockDisplayAttack {
        private BlockDisplayHandle body1, body2, head, beak, tailBlock;
        private final List<BlockDisplayHandle> leftWing = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWing = new ArrayList<>();
        private float altitude = 8.0f;
        private boolean diving = false;

        public ShadowRaven(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_raven", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(5, altitude, 0);

            body1 = spawnP(pos, 0, 0, 0, Material.COAL_BLOCK, 1.0f, 0.8f, 1.5f);
            body2 = spawnP(pos, 0, 0, -1.2, Material.BLACK_CONCRETE, 0.8f, 0.6f, 1.0f);
            head = spawnP(pos, 0, 0.3, 1.2, Material.COAL_BLOCK, 0.8f, 0.8f, 0.8f);
            beak = spawnP(pos, 0, 0.1, 1.8, Material.BLACKSTONE, 0.3f, 0.2f, 0.5f);
            tailBlock = spawnP(pos, 0, 0.2, -2.2, Material.BLACK_CONCRETE, 0.5f, 0.15f, 1.2f);

            // Wings (4 segments each)
            for (int i = 0; i < 4; i++) {
                leftWing.add(spawnP(pos, -(1.0 + i * 1.2), 0.1 * i, -0.3, Material.BLACK_CONCRETE,
                        1.2f, 0.15f, 1.0f - i * 0.15f));
                rightWing.add(spawnP(pos, 1.0 + i * 1.2, 0.1 * i, -0.3, Material.BLACK_CONCRETE,
                        1.2f, 0.15f, 1.0f - i * 0.15f));
            }

            DisplayBuilder.playSound(pos, Sound.ENTITY_PARROT_FLY, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(20, 15, 30).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double circleAngle = ticksAlive * 0.06;
            double radius = 6.0;

            // Periodic dive
            if (ticksAlive % 120 == 60) diving = true;
            if (diving) {
                altitude -= 0.3f;
                if (altitude < 1.5f) { altitude = 1.5f; diving = false; }
            } else if (altitude < 8.0f) {
                altitude += 0.1f;
            }

            double bx = Math.cos(circleAngle) * radius;
            double bz = Math.sin(circleAngle) * radius;
            Location pos = c.clone().add(bx, altitude, bz);

            float facing = (float) circleAngle;
            body1.entity().teleport(pos);
            body2.entity().teleport(pos.clone().add(-Math.sin(facing) * 1.2, 0, Math.cos(facing) * 1.2));
            head.entity().teleport(pos.clone().add(Math.sin(facing) * 1.2, 0.3, -Math.cos(facing) * 1.2));
            beak.entity().teleport(pos.clone().add(Math.sin(facing) * 1.8, 0.1, -Math.cos(facing) * 1.8));
            tailBlock.entity().teleport(pos.clone().add(-Math.sin(facing) * 2.2, 0.2, Math.cos(facing) * 2.2));

            float flap = (float) Math.sin(ticksAlive * 0.15) * 0.5f;
            for (int i = 0; i < 4; i++) {
                double wingX = (1.0 + i * 1.2);
                double wingY = 0.1 * i + flap * (i + 1) * 0.3;
                leftWing.get(i).entity().teleport(pos.clone().add(
                        Math.cos(facing) * (-wingX), wingY, Math.sin(facing) * (-wingX)));
                rightWing.get(i).entity().teleport(pos.clone().add(
                        Math.cos(facing) * wingX, wingY, Math.sin(facing) * wingX));
            }

            if (ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE, pos, 2, 0.3, 0.1, 0.3, 0.01);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(pos, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ShadowRaven(plugin); }
    }

    // ================================================================
    // 9. NIGHTMARE BEAR — Bear shape: torso (3), head (1), snout (1),
    //    4 legs (1 each), 2 arms (1 each), ears (2) = 14.
    //    Rears up on hind legs and swipes.
    // ================================================================
    public static class NightmareBear extends BlockDisplayAttack {
        private BlockDisplayHandle torso1, torso2, torso3;
        private BlockDisplayHandle head, snout, earL, earR;
        private BlockDisplayHandle armL, armR;
        private BlockDisplayHandle legFL, legFR, legBL, legBR;
        private boolean rearing = false;
        private float rearProgress = 0;

        public NightmareBear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_bear", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(70.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(5, 0, 0);

            torso1 = spawnP(pos, 0, 1.5, 0, Material.COAL_BLOCK, 2.5f, 2.0f, 1.8f);
            torso2 = spawnP(pos, 0, 1.5, -1.5, Material.BLACK_CONCRETE, 2.2f, 1.8f, 1.5f);
            torso3 = spawnP(pos, 0, 1.5, -2.8, Material.COAL_BLOCK, 1.8f, 1.5f, 1.2f);

            head = spawnP(pos, 0, 2.5, 1.5, Material.COAL_BLOCK, 2.0f, 1.8f, 1.5f);
            snout = spawnP(pos, 0, 2.0, 2.3, Material.BLACK_CONCRETE, 1.0f, 0.8f, 0.8f);
            earL = spawnP(pos, -0.7, 3.5, 1.5, Material.COAL_BLOCK, 0.5f, 0.5f, 0.3f);
            earR = spawnP(pos, 0.7, 3.5, 1.5, Material.COAL_BLOCK, 0.5f, 0.5f, 0.3f);

            armL = spawnP(pos, -1.5, 1.0, 0.5, Material.BLACK_CONCRETE, 0.8f, 2.0f, 0.8f);
            armR = spawnP(pos, 1.5, 1.0, 0.5, Material.BLACK_CONCRETE, 0.8f, 2.0f, 0.8f);

            legFL = spawnP(pos, -0.8, 0.5, 0.5, Material.BLACK_CONCRETE, 0.7f, 1.2f, 0.7f);
            legFR = spawnP(pos, 0.8, 0.5, 0.5, Material.BLACK_CONCRETE, 0.7f, 1.2f, 0.7f);
            legBL = spawnP(pos, -0.8, 0.5, -2.5, Material.BLACK_CONCRETE, 0.7f, 1.2f, 0.7f);
            legBR = spawnP(pos, 0.8, 0.5, -2.5, Material.BLACK_CONCRETE, 0.7f, 1.2f, 0.7f);

            DisplayBuilder.playSound(pos, Sound.ENTITY_POLAR_BEAR_WARNING, 1.0f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(25, 15, 15).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rear up periodically
            if (ticksAlive % 80 == 0) rearing = true;
            if (rearing) {
                rearProgress += 0.03f;
                if (rearProgress >= 1.0f) { rearProgress = 1.0f; rearing = false; }
            } else if (rearProgress > 0) {
                rearProgress -= 0.02f;
                if (rearProgress < 0) rearProgress = 0;
            }

            float rearHeight = rearProgress * 2.0f;
            Location pos = c.clone().add(4 - ticksAlive * 0.01, 0, 0);

            // Swipe animation when reared
            float swipeAngle = rearing ? (float) Math.sin(ticksAlive * 0.3) * 0.5f : 0;

            torso1.entity().teleport(pos.clone().add(0, 1.5 + rearHeight, 0));
            head.entity().teleport(pos.clone().add(0, 2.5 + rearHeight * 1.5, 1.5));
            armL.entity().teleport(pos.clone().add(-1.5 - swipeAngle, 1.0 + rearHeight, 0.5));
            armR.entity().teleport(pos.clone().add(1.5 + swipeAngle, 1.0 + rearHeight, 0.5));

            // Roar particles when rearing
            if (rearing && ticksAlive % 4 == 0) {
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                        pos.clone().add(0, 3 + rearHeight, 2), 3, 0.3, 0.3, 0.3, 0.03);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(pos, Sound.ENTITY_POLAR_BEAR_WARNING, 0.6f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareBear(plugin); }
    }

    // ================================================================
    // 10. ABYSSAL JELLYFISH — Bell/dome (6 blocks) with 8 trailing
    //     tentacles below. Floats above player, tentacles sway.
    //     Made of tinted glass for translucent look.
    // ================================================================
    public static class AbyssalJellyfish extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bell = new ArrayList<>();
        private final List<BlockDisplayHandle> tentacles = new ArrayList<>();

        public AbyssalJellyfish(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_jellyfish", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 7, 0);

            // Bell dome (6 blocks in dome shape)
            bell.add(spawnP(pos, 0, 1.0, 0, Material.TINTED_GLASS, 2.5f, 1.5f, 2.5f)); // Top
            bell.add(spawnP(pos, -1.0, 0, 0, Material.TINTED_GLASS, 1.5f, 1.2f, 2.0f));
            bell.add(spawnP(pos, 1.0, 0, 0, Material.TINTED_GLASS, 1.5f, 1.2f, 2.0f));
            bell.add(spawnP(pos, 0, 0, -1.0, Material.TINTED_GLASS, 2.0f, 1.2f, 1.5f));
            bell.add(spawnP(pos, 0, 0, 1.0, Material.TINTED_GLASS, 2.0f, 1.2f, 1.5f));
            bell.add(spawnP(pos, 0, -0.3, 0, Material.CRYING_OBSIDIAN, 1.5f, 0.5f, 1.5f)); // Inner glow

            // 8 tentacles
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 1.2;
                double z = Math.sin(angle) * 1.2;
                Material mat = (i % 2 == 0) ? Material.PURPLE_STAINED_GLASS : Material.BLUE_STAINED_GLASS;
                BlockDisplayHandle tent = displayBuilder.spawnBlock(pos.clone().add(x, -2, z), mat);
                tent.scale(0.2f, 3.0f, 0.2f).glow(120, 60, 180).interpolation(3, 0);
                tentacles.add(tent);
                spawnedEntities.add(tent.entity());
            }

            DisplayBuilder.playSound(pos, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(120, 60, 180).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle bob
            float bob = (float) Math.sin(ticksAlive * 0.04) * 0.8f;
            Location pos = c.clone().add(0, 7 + bob, 0);

            // Pulse bell (contract/expand)
            float pulse = 1.0f + (float) Math.sin(ticksAlive * 0.08) * 0.15f;
            for (BlockDisplayHandle b : bell) {
                // Just bob the bell
            }

            // Tentacle sway
            for (int i = 0; i < tentacles.size(); i++) {
                double angle = (2 * Math.PI * i) / 8;
                float sway = (float) Math.sin(ticksAlive * 0.06 + i * 0.8) * 1.0f;
                double x = Math.cos(angle) * (1.2 + sway * 0.3);
                double z = Math.sin(angle) * (1.2 + sway * 0.3);
                tentacles.get(i).entity().teleport(pos.clone().add(x, -2 - bob * 0.5, z));
                tentacles.get(i).rotate(sway * 0.2f, 0, 0, 1);
            }

            // Bioluminescent particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(pos, 3, 2.0, 120, 60, 180, 1.0f);
                c.getWorld().spawnParticle(Particle.END_ROD, pos.clone().add(0, -3, 0),
                        2, 1.0, 1.5, 1.0, 0.01);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(pos, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AbyssalJellyfish(plugin); }
    }

    // ================================================================
    // 11. BONE DRAGON — Dragon shape: spine (6), skull (1), jaw (1),
    //     2 wings (2 each), tail (2), ribs (2) = 16 blocks.
    //     Flies in circles above, breathing smoke.
    // ================================================================
    public static class BoneDragon extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spine = new ArrayList<>();
        private BlockDisplayHandle skull, jaw;
        private final List<BlockDisplayHandle> leftWingBone = new ArrayList<>();
        private final List<BlockDisplayHandle> rightWingBone = new ArrayList<>();
        private BlockDisplayHandle tail1, tail2;
        private BlockDisplayHandle ribLeft, ribRight;

        public BoneDragon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_dragon", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(70.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 8, 0);

            // Spine (6 vertebrae)
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle vert = displayBuilder.spawnBlock(
                        pos.clone().add(0, 0, -i * 1.2), Material.BONE_BLOCK);
                float s = 1.2f - i * 0.08f;
                vert.scale(s, s, 1.2f).glow(200, 190, 170).interpolation(2, 0);
                spine.add(vert);
                spawnedEntities.add(vert.entity());
            }

            skull = spawnP(pos, 0, 0.3, 1.5, Material.BONE_BLOCK, 1.8f, 1.5f, 1.5f);
            jaw = spawnP(pos, 0, -0.5, 1.8, Material.BONE_BLOCK, 1.2f, 0.4f, 1.0f);

            // Wings (2 bones each)
            leftWingBone.add(spawnP(pos, -2.0, 0.5, -1, Material.BONE_BLOCK, 2.5f, 0.3f, 0.3f));
            leftWingBone.add(spawnP(pos, -4.0, 1.0, -1.5, Material.BONE_BLOCK, 2.0f, 0.25f, 0.25f));
            rightWingBone.add(spawnP(pos, 2.0, 0.5, -1, Material.BONE_BLOCK, 2.5f, 0.3f, 0.3f));
            rightWingBone.add(spawnP(pos, 4.0, 1.0, -1.5, Material.BONE_BLOCK, 2.0f, 0.25f, 0.25f));

            tail1 = spawnP(pos, 0, 0, -7, Material.BONE_BLOCK, 0.6f, 0.6f, 1.5f);
            tail2 = spawnP(pos, 0, 0.2, -8.5, Material.BONE_BLOCK, 0.4f, 0.4f, 1.2f);

            ribLeft = spawnP(pos, -1.0, -0.3, -2, Material.BONE_BLOCK, 0.2f, 1.0f, 2.0f);
            ribRight = spawnP(pos, 1.0, -0.3, -2, Material.BONE_BLOCK, 0.2f, 1.0f, 2.0f);

            DisplayBuilder.playSound(pos, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 190, 170).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double circleAngle = ticksAlive * 0.03;
            double radius = 8.0;
            float bob = (float) Math.sin(ticksAlive * 0.06) * 0.8f;
            double cx = Math.cos(circleAngle) * radius;
            double cz = Math.sin(circleAngle) * radius;
            Location dragonPos = c.clone().add(cx, 8 + bob, cz);

            float facing = (float)(circleAngle + Math.PI / 2);
            float cosF = (float) Math.cos(facing);
            float sinF = (float) Math.sin(facing);

            // Undulating spine
            for (int i = 0; i < spine.size(); i++) {
                double segAngle = circleAngle - i * 0.15;
                double sx = Math.cos(segAngle) * radius;
                double sz = Math.sin(segAngle) * radius;
                float segBob = (float) Math.sin(ticksAlive * 0.06 + i * 0.3) * 0.5f;
                spine.get(i).entity().teleport(c.clone().add(sx, 8 + segBob, sz));
            }

            skull.entity().teleport(dragonPos.clone().add(sinF * 1.5, 0.3, -cosF * 1.5));
            jaw.entity().teleport(dragonPos.clone().add(sinF * 1.8, -0.5, -cosF * 1.8));

            // Wing flap
            float flap = (float) Math.sin(ticksAlive * 0.1) * 0.5f;
            for (int i = 0; i < 2; i++) {
                float wingX = 2.0f + i * 2.0f;
                leftWingBone.get(i).entity().teleport(
                        dragonPos.clone().add(cosF * (-wingX), 0.5 + flap * (i + 1), sinF * (-wingX)));
                rightWingBone.get(i).entity().teleport(
                        dragonPos.clone().add(cosF * wingX, 0.5 + flap * (i + 1), sinF * wingX));
            }

            // Smoke breath
            if (ticksAlive % 5 == 0) {
                Location breathLoc = skull.entity().getLocation().clone().add(sinF * 1, -0.2, -cosF * 1);
                c.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, breathLoc,
                        3, 0.2, 0.2, 0.2, 0.03);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(dragonPos, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BoneDragon(plugin); }
    }

    // ================================================================
    // 12. DEMON HORNS — 2 massive curved horns (6 blocks each = 12)
    //     erupting from the ground, curving inward toward the player.
    //     Grow block by block, then pulse with dark energy.
    // ================================================================
    public static class DemonHorns extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftHorn = new ArrayList<>();
        private final List<BlockDisplayHandle> rightHorn = new ArrayList<>();
        private int blocksGrown = 0;

        public DemonHorns(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demon_horns", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < 6; i++) {
                // Left horn curves right and inward
                double angle = i * 0.25;
                double lx = -3.0 + Math.sin(angle) * i * 0.3;
                double ly = i * 1.2;
                double lz = -Math.cos(angle) * i * 0.15;
                float scale = 1.5f - i * 0.15f;
                Material mat = (i < 2) ? Material.BLACKSTONE
                        : (i < 4) ? Material.POLISHED_BLACKSTONE
                        : Material.OBSIDIAN;
                BlockDisplayHandle lb = displayBuilder.spawnBlock(center.clone().add(lx, -2, lz), mat);
                lb.scale(scale, 1.2f, scale).glow(40, 20, 40).interpolation(4, 0);
                lb.scale(0.01f, 0.01f, 0.01f); // Hidden initially
                leftHorn.add(lb);
                spawnedEntities.add(lb.entity());

                // Right horn (mirrored)
                BlockDisplayHandle rb = displayBuilder.spawnBlock(center.clone().add(-lx, -2, lz), mat);
                rb.scale(scale, 1.2f, scale).glow(40, 20, 40).interpolation(4, 0);
                rb.scale(0.01f, 0.01f, 0.01f);
                rightHorn.add(rb);
                spawnedEntities.add(rb.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Grow one segment every 10 ticks
            if (ticksAlive % 10 == 0 && blocksGrown < 6) {
                int i = blocksGrown;
                double angle = i * 0.25;
                double lx = -3.0 + Math.sin(angle) * i * 0.3;
                double ly = i * 1.2;
                double lz = -Math.cos(angle) * i * 0.15;
                float scale = 1.5f - i * 0.15f;

                leftHorn.get(i).entity().teleport(c.clone().add(lx, ly, lz));
                leftHorn.get(i).animateTo(
                        new Vector3f(-scale / 2, -0.6f, -scale / 2),
                        new AxisAngle4f(-(float)(angle * 0.3), 0, 0, 1),
                        new Vector3f(scale, 1.2f, scale), 8);

                rightHorn.get(i).entity().teleport(c.clone().add(-lx, ly, lz));
                rightHorn.get(i).animateTo(
                        new Vector3f(-scale / 2, -0.6f, -scale / 2),
                        new AxisAngle4f((float)(angle * 0.3), 0, 0, 1),
                        new Vector3f(scale, 1.2f, scale), 8);

                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_PLACE, 0.7f, 0.4f + i * 0.05f);
                c.getWorld().spawnParticle(Particle.SMOKE, c.clone().add(lx, ly, lz),
                        5, 0.3, 0.3, 0.3, 0.02);
                blocksGrown++;
            }

            // Dark energy pulse after fully grown
            if (blocksGrown >= 6 && ticksAlive % 8 == 0) {
                int glowPulse = (int)(40 + Math.sin(ticksAlive * 0.1) * 30);
                for (BlockDisplayHandle h : leftHorn) h.glow(glowPulse, 10, glowPulse + 10);
                for (BlockDisplayHandle h : rightHorn) h.glow(glowPulse, 10, glowPulse + 10);

                // Energy arc between horn tips
                if (ticksAlive % 16 == 0) {
                    Location tipL = c.clone().add(-1.5, 6, -0.5);
                    Location tipR = c.clone().add(1.5, 6, -0.5);
                    DisplayBuilder.particleLine(tipL, tipR, Particle.SOUL_FIRE_FLAME, 4, null);
                    DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.4f, 0.6f);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonHorns(plugin); }
    }

    // ================================================================
    // 13. SOUL LEVIATHAN — Massive whale/leviathan shape: body (5),
    //     head (2), tail (3), fins (2), eye (1) = 13 blocks.
    //     Swims through the air above the player.
    // ================================================================
    public static class SoulLeviathan extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private BlockDisplayHandle headTop, headFront;
        private BlockDisplayHandle tail1, tail2, tailFin;
        private BlockDisplayHandle finLeft, finRight;
        private BlockDisplayHandle eye;

        public SoulLeviathan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_leviathan", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 8, 0);

            // Body segments (5, largest in middle)
            float[] sizes = {2.5f, 3.2f, 3.5f, 3.0f, 2.2f};
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle seg = displayBuilder.spawnBlock(
                        pos.clone().add(0, 0, -i * 2), Material.SOUL_SOIL);
                seg.scale(sizes[i], sizes[i] * 0.6f, 2.0f).glow(60, 80, 100).interpolation(2, 0);
                body.add(seg);
                spawnedEntities.add(seg.entity());
            }

            headTop = spawnP(pos, 0, 0.5, 2.0, Material.SOUL_SAND, 3.0f, 2.0f, 2.5f);
            headFront = spawnP(pos, 0, -0.2, 3.5, Material.SOUL_SOIL, 2.0f, 1.2f, 1.5f);

            tail1 = spawnP(pos, 0, 0, -10, Material.SOUL_SAND, 1.5f, 1.0f, 2.0f);
            tail2 = spawnP(pos, 0, 0, -12, Material.SOUL_SOIL, 0.8f, 0.6f, 1.5f);
            tailFin = spawnP(pos, 0, 0.5, -13.5, Material.CYAN_STAINED_GLASS, 3.0f, 0.15f, 1.5f);

            finLeft = spawnP(pos, -2.5, -0.3, -2, Material.CYAN_STAINED_GLASS, 2.5f, 0.15f, 1.5f);
            finRight = spawnP(pos, 2.5, -0.3, -2, Material.CYAN_STAINED_GLASS, 2.5f, 0.15f, 1.5f);

            eye = spawnP(pos, -1.2, 0.5, 2.5, Material.SEA_LANTERN, 0.5f, 0.5f, 0.3f);

            DisplayBuilder.playSound(pos, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(60, 80, 100).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double swimAngle = ticksAlive * 0.025;
            double radius = 10.0;
            float bob = (float) Math.sin(ticksAlive * 0.04) * 1.0f;

            double fx = Math.cos(swimAngle);
            double fz = Math.sin(swimAngle);

            Location levPos = c.clone().add(fx * radius, 8 + bob, fz * radius);
            float facing = (float)(swimAngle + Math.PI / 2);
            float cosF = (float) Math.cos(facing);
            float sinF = (float) Math.sin(facing);

            // Undulating body
            for (int i = 0; i < body.size(); i++) {
                double segAngle = swimAngle - i * 0.12;
                float segBob = (float) Math.sin(ticksAlive * 0.04 + i * 0.4) * 0.6f;
                body.get(i).entity().teleport(c.clone().add(
                        Math.cos(segAngle) * radius, 8 + segBob, Math.sin(segAngle) * radius));
            }

            headTop.entity().teleport(levPos.clone().add(sinF * 2, 0.5, -cosF * 2));
            headFront.entity().teleport(levPos.clone().add(sinF * 3.5, -0.2, -cosF * 3.5));
            eye.entity().teleport(levPos.clone().add(sinF * 2.5 + cosF * (-1.2), 0.5, -cosF * 2.5 + sinF * (-1.2)));

            // Fin sway
            float finFlap = (float) Math.sin(ticksAlive * 0.08) * 0.5f;
            finLeft.entity().teleport(levPos.clone().add(cosF * (-2.5), -0.3 + finFlap, sinF * (-2.5)));
            finRight.entity().teleport(levPos.clone().add(cosF * 2.5, -0.3 - finFlap, sinF * 2.5));

            // Soul particles
            if (ticksAlive % 5 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL, levPos, 3, 2, 1, 2, 0.02);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, levPos.clone().add(0, -1, 0),
                        2, 1.5, 0.5, 1.5, 0.01);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(levPos, Sound.ENTITY_ELDER_GUARDIAN_AMBIENT, 0.5f, 0.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulLeviathan(plugin); }
    }
}
