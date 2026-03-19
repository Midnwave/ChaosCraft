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
 * Devil's Dream — DEVIL'S ARSENAL ATTACKS
 * 13 weapon-shaped BlockDisplay attacks featuring demonic pitchforks,
 * hellfire scythes, cursed swords, and brimstone hammers.
 *
 * Materials: nether_bricks, red_nether_bricks, crimson_planks, netherrack,
 * magma_block, gold_block, blackstone, obsidian
 */
public final class DevilsArsenal {

    private DevilsArsenal() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DemonicPitchfork(plugin));
        registry.register(new HellfireScythe(plugin));
        registry.register(new BrimstoneHammer(plugin));
        registry.register(new SoulBow(plugin));
        registry.register(new NightmareAxe(plugin));
        registry.register(new DemonWhip(plugin));
        registry.register(new CursedSword(plugin));
        registry.register(new BrimstoneSpear(plugin));
        registry.register(new SoulflameStaff(plugin));
        registry.register(new DemonicFlail(plugin));
        registry.register(new NightmareCrossbow(plugin));
        registry.register(new HellfireShield(plugin));
        registry.register(new DevilsTrident(plugin));
    }

    // ================================================================
    // 1. DEMONIC PITCHFORK — 3-pronged fork: handle (4), crossbar (1),
    //    3 prongs (1 each), fork-tip accents (3) = 11 blocks.
    //    Stabs downward toward player.
    // ================================================================
    public static class DemonicPitchfork extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float stabProgress = 0;
        private boolean stabbing = false;

        public DemonicPitchfork(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_pitchfork", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location base = center.clone().add(0, 10, 0);

            // Handle (4 segments)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(0, -i * 1.8, 0), Material.NETHER_BRICKS);
                h.scale(0.5f, 1.8f, 0.5f).glow(120, 30, 30).interpolation(2, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossbar
            BlockDisplayHandle cross = displayBuilder.spawnBlock(
                    base.clone().add(0, -7.5, 0), Material.RED_NETHER_BRICKS);
            cross.scale(3.0f, 0.4f, 0.4f).glow(180, 40, 40);
            parts.add(cross);
            spawnedEntities.add(cross.entity());

            // 3 prongs
            for (int p = -1; p <= 1; p++) {
                BlockDisplayHandle prong = displayBuilder.spawnBlock(
                        base.clone().add(p * 1.0, -9.0, 0), Material.GOLD_BLOCK);
                prong.scale(0.3f, 2.0f, 0.3f).glow(255, 200, 50);
                parts.add(prong);
                spawnedEntities.add(prong.entity());
            }

            // Prong tips
            for (int p = -1; p <= 1; p++) {
                BlockDisplayHandle tip = displayBuilder.spawnBlock(
                        base.clone().add(p * 1.0, -10.5, 0), Material.MAGMA_BLOCK);
                tip.scale(0.2f, 0.6f, 0.2f).glow(255, 120, 20);
                parts.add(tip);
                spawnedEntities.add(tip.entity());
            }

            DisplayBuilder.playSound(base, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Hover for 60 ticks, then stab
            if (ticksAlive == 60) stabbing = true;

            float yOffset;
            if (stabbing && stabProgress < 1.0f) {
                stabProgress += 0.05f;
                yOffset = 10.0f - stabProgress * 8.0f;
            } else if (!stabbing) {
                yOffset = 10.0f + (float) Math.sin(ticksAlive * 0.05) * 0.5f;
            } else {
                yOffset = 2.0f;
            }

            // Reposition all parts
            Location base = c.clone().add(0, yOffset, 0);
            for (int i = 0; i < 4; i++) {
                parts.get(i).entity().teleport(base.clone().add(0, -i * 1.8, 0));
            }
            parts.get(4).entity().teleport(base.clone().add(0, -7.5, 0));
            for (int p = 0; p < 3; p++) {
                parts.get(5 + p).entity().teleport(base.clone().add((p - 1) * 1.0, -9.0, 0));
                parts.get(8 + p).entity().teleport(base.clone().add((p - 1) * 1.0, -10.5, 0));
            }

            // Stab effects
            if (stabbing && stabProgress < 0.5f) {
                c.getWorld().spawnParticle(Particle.FLAME,
                        c.clone().add(0, yOffset - 10, 0), 5, 0.3, 0.3, 0.3, 0.03);
            }

            if (stabbing && stabProgress >= 0.95f && stabProgress < 1.0f) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.4f);
                c.getWorld().spawnParticle(Particle.CRIT, c.clone().add(0, 0.5, 0),
                        15, 1, 0.5, 1, 0.1);
            }

            if (ticksAlive % 20 == 0 && !stabbing) {
                DisplayBuilder.playSound(c.clone().add(0, 10, 0), Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonicPitchfork(plugin); }
    }

    // ================================================================
    // 2. HELLFIRE SCYTHE — Curved blade (4), handle (5), pommel (1)
    //    = 10 blocks. Sweeps horizontally through the player area.
    // ================================================================
    public static class HellfireScythe extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private final List<BlockDisplayHandle> handle = new ArrayList<>();
        private BlockDisplayHandle pommel;
        private double sweepAngle = -Math.PI / 2;

        public HellfireScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_scythe", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(70.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Handle (5 segments — long staff)
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.NETHER_BRICKS);
                h.scale(0.4f, 0.4f, 1.8f).glow(100, 30, 30).interpolation(2, 0);
                handle.add(h);
                spawnedEntities.add(h.entity());
            }

            // Curved blade (4 segments forming a crescent)
            for (int i = 0; i < 4; i++) {
                Material mat = (i == 0 || i == 3) ? Material.NETHERRACK : Material.RED_NETHER_BRICKS;
                BlockDisplayHandle b = displayBuilder.spawnBlock(center, mat);
                float bWidth = (i == 1 || i == 2) ? 2.0f : 1.2f;
                b.scale(bWidth, 0.3f, 0.8f).glow(255, 60, 20).interpolation(2, 0);
                blade.add(b);
                spawnedEntities.add(b.entity());
            }

            // Pommel
            pommel = displayBuilder.spawnBlock(center, Material.GOLD_BLOCK);
            pommel.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 50);
            spawnedEntities.add(pommel.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep 180 degrees
            sweepAngle += 0.04;
            float cosA = (float) Math.cos(sweepAngle);
            float sinA = (float) Math.sin(sweepAngle);
            float height = 2.0f;

            // Handle along the sweep direction
            for (int i = 0; i < handle.size(); i++) {
                double dist = (i - 2) * 1.8;
                handle.get(i).entity().teleport(c.clone().add(cosA * dist, height, sinA * dist));
                handle.get(i).rotate((float) sweepAngle, 0, 1, 0);
            }

            // Blade at the tip (curved perpendicular to handle)
            double tipDist = 5.0;
            for (int i = 0; i < blade.size(); i++) {
                double bladeOffset = (i - 1.5) * 0.8;
                double curveOut = Math.sin((double) i / 3 * Math.PI) * 1.5;
                blade.get(i).entity().teleport(c.clone().add(
                        cosA * tipDist + sinA * bladeOffset,
                        height + curveOut,
                        sinA * tipDist - cosA * bladeOffset));
                blade.get(i).rotate((float) sweepAngle, 0, 1, 0);
            }

            pommel.entity().teleport(c.clone().add(-cosA * 4, height, -sinA * 4));

            // Slash particles at blade
            if (ticksAlive % 2 == 0) {
                Location bladeTip = c.clone().add(cosA * tipDist, height, sinA * tipDist);
                c.getWorld().spawnParticle(Particle.FLAME, bladeTip, 3, 0.3, 0.3, 0.3, 0.02);
                c.getWorld().spawnParticle(Particle.CRIT, bladeTip, 2, 0.2, 0.2, 0.2, 0.05);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireScythe(plugin); }
    }

    // ================================================================
    // 3. BRIMSTONE HAMMER — Head (4 blocks wide), handle (4), cap (2)
    //    = 10. Rises up then slams down with shockwave on impact.
    // ================================================================
    public static class BrimstoneHammer extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> headBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> handleBlocks = new ArrayList<>();
        private BlockDisplayHandle cap1, cap2;
        private float hammerY = 12.0f;
        private boolean slamming = false;
        private boolean impacted = false;

        public BrimstoneHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(50.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(90.0);
            config.setImpactRadius(8.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location base = center.clone().add(0, hammerY, 0);

            // Hammer head (4 blocks — wide rectangle)
            for (int i = 0; i < 4; i++) {
                double x = (i - 1.5) * 1.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(x, 0, 0), Material.RED_NETHER_BRICKS);
                h.scale(1.0f, 2.0f, 1.5f).glow(200, 60, 30).interpolation(2, 0);
                headBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handle (4 segments)
            for (int i = 0; i < 4; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        base.clone().add(0, 2 + i * 1.5, 0), Material.NETHER_BRICKS);
                h.scale(0.5f, 1.5f, 0.5f).glow(100, 40, 30);
                handleBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // End caps
            cap1 = displayBuilder.spawnBlock(base.clone().add(-2, 0, 0), Material.MAGMA_BLOCK);
            cap1.scale(0.8f, 1.5f, 1.0f).glow(255, 120, 20);
            spawnedEntities.add(cap1.entity());
            cap2 = displayBuilder.spawnBlock(base.clone().add(2, 0, 0), Material.MAGMA_BLOCK);
            cap2.scale(0.8f, 1.5f, 1.0f).glow(255, 120, 20);
            spawnedEntities.add(cap2.entity());

            DisplayBuilder.playSound(base, Sound.ENTITY_IRON_GOLEM_HURT, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (impacted) return;

            if (ticksAlive == 50) slamming = true;

            if (slamming) {
                hammerY -= 0.8f;
                if (hammerY <= 1.0f) {
                    hammerY = 1.0f;
                    impacted = true;
                    triggerImpactDamage(c);
                    c.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, c, 2, 0.5, 0.5, 0.5, 0);
                    DisplayBuilder.particleRing(c, 7.0, Particle.FLAME, 28, null);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.3f);
                }
            } else {
                hammerY = 12.0f + (float) Math.sin(ticksAlive * 0.05) * 0.5f;
            }

            Location base = c.clone().add(0, hammerY, 0);
            for (int i = 0; i < headBlocks.size(); i++) {
                headBlocks.get(i).entity().teleport(base.clone().add((i - 1.5) * 1.0, 0, 0));
            }
            for (int i = 0; i < handleBlocks.size(); i++) {
                handleBlocks.get(i).entity().teleport(base.clone().add(0, 2 + i * 1.5, 0));
            }
            cap1.entity().teleport(base.clone().add(-2, 0, 0));
            cap2.entity().teleport(base.clone().add(2, 0, 0));

            if (slamming && !impacted && ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, base, 5, 1, 0.5, 1, 0.03);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneHammer(plugin); }
    }

    // ================================================================
    // 4. SOUL BOW — Large bow shape (6 blocks) + arrow (4) = 10.
    //    Draws back then fires the arrow at the player.
    // ================================================================
    public static class SoulBow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bowParts = new ArrayList<>();
        private final List<BlockDisplayHandle> arrowParts = new ArrayList<>();
        private float drawProgress = 0;
        private boolean fired = false;
        private float arrowZ = 0;

        public SoulBow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_bow", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location bowPos = center.clone().add(0, 3, -6);

            // Bow limbs (curved, 3 per side)
            for (int i = 0; i < 3; i++) {
                double curve = Math.sin((i + 1) * 0.5) * 1.5;
                bowParts.add(spawnP(bowPos, 0, 1.5 + i * 1.0, curve, Material.CRIMSON_PLANKS, 0.3f, 1.0f, 0.3f));
                bowParts.add(spawnP(bowPos, 0, -(0.5 + i * 1.0), curve, Material.CRIMSON_PLANKS, 0.3f, 1.0f, 0.3f));
            }

            // Arrow (4 segments: shaft 2, head 1, fletching 1)
            arrowParts.add(spawnP(bowPos, 0, 0.5, -0.5, Material.BONE_BLOCK, 0.15f, 0.15f, 2.5f));
            arrowParts.add(spawnP(bowPos, 0, 0.5, -3.0, Material.BONE_BLOCK, 0.15f, 0.15f, 2.5f));
            arrowParts.add(spawnP(bowPos, 0, 0.5, 2.0, Material.NETHERRACK, 0.3f, 0.3f, 0.5f));
            arrowParts.add(spawnP(bowPos, 0, 0.5, -5.0, Material.CRIMSON_PLANKS, 0.4f, 0.4f, 0.3f));

            DisplayBuilder.playSound(bowPos, Sound.ITEM_CROSSBOW_LOADING_START, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(80, 160, 180).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (!fired && drawProgress < 1.0f) {
                drawProgress += 0.015f;
                // Draw arrow back
                arrowZ = -drawProgress * 3.0f;
            }

            if (!fired && drawProgress >= 1.0f) {
                fired = true;
                DisplayBuilder.playSound(c.clone().add(0, 3, -6), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.4f);
            }

            if (fired) {
                arrowZ += 0.8f;
                // Arrow trail
                Location arrowLoc = c.clone().add(0, 3.5, -6 + arrowZ);
                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrowLoc, 3, 0.1, 0.1, 0.1, 0.01);
                }
            }

            // Update arrow positions
            Location bowPos = c.clone().add(0, 3, -6);
            float aOffset = fired ? arrowZ : arrowZ;
            arrowParts.get(0).entity().teleport(bowPos.clone().add(0, 0.5, -0.5 + aOffset));
            arrowParts.get(1).entity().teleport(bowPos.clone().add(0, 0.5, -3.0 + aOffset));
            arrowParts.get(2).entity().teleport(bowPos.clone().add(0, 0.5, 2.0 + aOffset));
            arrowParts.get(3).entity().teleport(bowPos.clone().add(0, 0.5, -5.0 + aOffset));

            if (ticksAlive % 25 == 0 && !fired) {
                DisplayBuilder.playSound(bowPos, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulBow(plugin); }
    }

    // ================================================================
    // 5. NIGHTMARE AXE — Axe head (4), handle (5), eye accent (1) = 10.
    //    Spins horizontally like a thrown tomahawk.
    // ================================================================
    public static class NightmareAxe extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private double orbitAngle = 0;
        private double orbitRadius = 6.0;

        public NightmareAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_axe", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Handle (5 blocks)
            for (int i = 0; i < 5; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.NETHER_BRICKS);
                h.scale(0.3f, 0.3f, 1.5f).glow(80, 30, 30).interpolation(2, 0);
                parts.add(h);
                spawnedEntities.add(h.entity());
            }

            // Axe head (4 blocks — wedge shape)
            for (int i = 0; i < 4; i++) {
                Material mat = (i % 2 == 0) ? Material.RED_NETHER_BRICKS : Material.BLACKSTONE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(center, mat);
                float w2 = 1.5f - i * 0.2f;
                b.scale(w2, 2.5f - i * 0.3f, 0.4f).glow(180, 40, 40).interpolation(2, 0);
                parts.add(b);
                spawnedEntities.add(b.entity());
            }

            // Eye accent on the axe head
            BlockDisplayHandle eye = displayBuilder.spawnBlock(center, Material.MAGMA_BLOCK);
            eye.scale(0.5f, 0.5f, 0.5f).glow(255, 120, 20);
            parts.add(eye);
            spawnedEntities.add(eye.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            orbitAngle += 0.08;
            if (orbitRadius > 2.5) orbitRadius -= 0.015;

            double cx = Math.cos(orbitAngle) * orbitRadius;
            double cz = Math.sin(orbitAngle) * orbitRadius;
            Location axePos = c.clone().add(cx, 2, cz);

            float spinAngle = (float)(ticksAlive * 0.2);

            // Handle along the spin axis
            for (int i = 0; i < 5; i++) {
                double offset = (i - 2) * 1.5;
                double ox = Math.cos(spinAngle) * offset;
                double oy = Math.sin(spinAngle) * offset;
                parts.get(i).entity().teleport(axePos.clone().add(ox, 2 + oy, 0));
            }

            // Axe head at one end
            for (int i = 0; i < 4; i++) {
                double headOffset = 4.0 + i * 0.4;
                double ox = Math.cos(spinAngle) * headOffset;
                double oy = Math.sin(spinAngle) * headOffset;
                parts.get(5 + i).entity().teleport(axePos.clone().add(ox, 2 + oy, 0));
                parts.get(5 + i).rotate(spinAngle, 0, 0, 1);
            }

            // Eye on axe head center
            double eyeOffset = 5.0;
            parts.get(9).entity().teleport(axePos.clone().add(
                    Math.cos(spinAngle) * eyeOffset, 2 + Math.sin(spinAngle) * eyeOffset, 0));

            // Spin trail
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, axePos.clone().add(0, 2, 0),
                        3, 0.5, 0.5, 0.5, 0.05);
            }

            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(axePos, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareAxe(plugin); }
    }

    // ================================================================
    // 6. DEMON WHIP — 12 small blocks forming a flexible whip that
    //    lashes across the arena. Segments trail behind the tip.
    //    Crimson themed with fire at the tip.
    // ================================================================
    public static class DemonWhip extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> segments = new ArrayList<>();
        private static final int SEG_COUNT = 12;
        private double whipAngle = 0;

        public DemonWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demon_whip", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            for (int i = 0; i < SEG_COUNT; i++) {
                Material mat = (i < 3) ? Material.CRIMSON_STEM
                        : (i < 8) ? Material.CRIMSON_PLANKS
                        : Material.NETHERRACK;
                BlockDisplayHandle seg = displayBuilder.spawnBlock(center, mat);
                float size = 0.5f - i * 0.03f;
                seg.scale(size, size, 0.8f).glow(150, 30, 30).interpolation(2, 0);
                segments.add(seg);
                spawnedEntities.add(seg.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            whipAngle += 0.06;

            for (int i = 0; i < SEG_COUNT; i++) {
                double segAngle = whipAngle - i * 0.35;
                double radius = 1.0 + i * 0.5;
                double x = Math.cos(segAngle) * radius;
                double z = Math.sin(segAngle) * radius;
                double y = 2.0 + Math.sin(segAngle * 2) * 0.5;

                segments.get(i).entity().teleport(c.clone().add(x, y, z));
                segments.get(i).rotate((float) segAngle, 0, 1, 0);
            }

            // Fire at whip tip
            if (ticksAlive % 3 == 0) {
                BlockDisplayHandle tip = segments.get(SEG_COUNT - 1);
                Location tipLoc = tip.entity().getLocation();
                c.getWorld().spawnParticle(Particle.FLAME, tipLoc, 3, 0.1, 0.1, 0.1, 0.03);
            }

            // Crack sound
            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonWhip(plugin); }
    }

    // ================================================================
    // 7. CURSED SWORD — Blade (5), guard (2), grip (2), pommel (1)
    //    = 10 blocks. Floats upright then slashes downward.
    // ================================================================
    public static class CursedSword extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float slashAngle = 0;
        private boolean slashing = false;

        public CursedSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_sword", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(65.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location swordBase = center.clone().add(0, 6, -3);

            // Blade (5 segments, tapering)
            for (int i = 0; i < 5; i++) {
                float width = 0.8f - i * 0.1f;
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        swordBase.clone().add(0, i * 1.5, 0), Material.RED_NETHER_BRICKS);
                b.scale(width, 1.5f, 0.25f).glow(200, 50, 40).interpolation(2, 0);
                parts.add(b);
                spawnedEntities.add(b.entity());
            }

            // Guard (2)
            parts.add(spawnP(swordBase, -1.0, -0.5, 0, Material.GOLD_BLOCK, 1.0f, 0.4f, 0.4f));
            parts.add(spawnP(swordBase, 1.0, -0.5, 0, Material.GOLD_BLOCK, 1.0f, 0.4f, 0.4f));

            // Grip (2)
            parts.add(spawnP(swordBase, 0, -1.5, 0, Material.NETHER_BRICKS, 0.4f, 1.0f, 0.4f));
            parts.add(spawnP(swordBase, 0, -2.5, 0, Material.NETHER_BRICKS, 0.4f, 1.0f, 0.4f));

            // Pommel
            parts.add(spawnP(swordBase, 0, -3.3, 0, Material.MAGMA_BLOCK, 0.6f, 0.6f, 0.6f));

            DisplayBuilder.playSound(swordBase, Sound.ITEM_TRIDENT_RETURN, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 50, 40).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive == 60) slashing = true;

            if (slashing && slashAngle < Math.PI * 0.75) {
                slashAngle += 0.06f;
            }

            float tilt = slashing ? slashAngle : (float) Math.sin(ticksAlive * 0.03) * 0.1f;

            // Rotate all parts around the guard point
            for (BlockDisplayHandle part : parts) {
                part.rotate(tilt, 1, 0, 0);
            }

            if (slashing && slashAngle > 0 && ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT,
                        c.clone().add(0, 4, -3), 5, 1, 1, 0.5, 0.1);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,
                        c.clone().add(0, 4, -3), 2, 0.5, 0.5, 0.3, 0.02);
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CursedSword(plugin); }
    }

    // ================================================================
    // 8. BRIMSTONE SPEAR — Long shaft (6), head (3), flag (1) = 10.
    //    Hovers then launches at the player like a javelin.
    // ================================================================
    public static class BrimstoneSpear extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private boolean launched = false;
        private float travelZ = -8;

        public BrimstoneSpear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_spear", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 3, travelZ);

            // Shaft (6)
            for (int i = 0; i < 6; i++) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(
                        pos.clone().add(0, 0, -i * 1.2), Material.NETHER_BRICKS);
                s.scale(0.3f, 0.3f, 1.2f).glow(100, 40, 30).interpolation(2, 0);
                parts.add(s);
                spawnedEntities.add(s.entity());
            }

            // Spear head (3 — triangular point)
            parts.add(spawnP(pos, 0, 0, 1.0, Material.NETHERRACK, 0.5f, 0.5f, 1.0f));
            parts.add(spawnP(pos, 0, 0, 2.0, Material.RED_NETHER_BRICKS, 0.35f, 0.35f, 1.0f));
            parts.add(spawnP(pos, 0, 0, 3.0, Material.MAGMA_BLOCK, 0.2f, 0.2f, 0.6f));

            // Flag/pennant
            parts.add(spawnP(pos, 0.3, 0.3, -4, Material.CRIMSON_PLANKS, 0.8f, 0.5f, 0.1f));

            DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_THROW, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 60, 30).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive == 40) {
                launched = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.5f);
            }

            if (launched) {
                travelZ += 0.5f;
            } else {
                // Wobble before launch
                travelZ = -8 + (float) Math.sin(ticksAlive * 0.1) * 0.3f;
            }

            Location pos = c.clone().add(0, 3, travelZ);
            for (int i = 0; i < 6; i++) {
                parts.get(i).entity().teleport(pos.clone().add(0, 0, -i * 1.2));
            }
            parts.get(6).entity().teleport(pos.clone().add(0, 0, 1.0));
            parts.get(7).entity().teleport(pos.clone().add(0, 0, 2.0));
            parts.get(8).entity().teleport(pos.clone().add(0, 0, 3.0));
            parts.get(9).entity().teleport(pos.clone().add(0.3, 0.3, -4));

            if (launched && ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, pos.clone().add(0, 0, 3), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneSpear(plugin); }
    }

    // ================================================================
    // 9. SOULFLAME STAFF — Staff (5 blocks) with soul flame orb (3)
    //    + crystal tip (2) = 10. Hovers, channels energy, fires beam.
    // ================================================================
    public static class SoulflameStaff extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private boolean channeling = false;

        public SoulflameStaff(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soulflame_staff", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(3, 4, 0);

            for (int i = 0; i < 5; i++) {
                parts.add(spawnP(pos, 0, -i * 1.2, 0, Material.NETHER_BRICKS, 0.35f, 1.2f, 0.35f));
            }
            // Orb (3 blocks forming sphere at top)
            parts.add(spawnP(pos, 0, 1.5, 0, Material.SOUL_LANTERN, 1.0f, 1.0f, 1.0f));
            parts.add(spawnP(pos, -0.3, 1.8, 0, Material.SOUL_SAND, 0.6f, 0.6f, 0.6f));
            parts.add(spawnP(pos, 0.3, 1.8, 0, Material.SOUL_SAND, 0.6f, 0.6f, 0.6f));
            // Crystal tips
            parts.add(spawnP(pos, 0, 2.5, 0, Material.AMETHYST_BLOCK, 0.4f, 0.8f, 0.4f));
            parts.add(spawnP(pos, 0, 3.0, 0, Material.BUDDING_AMETHYST, 0.2f, 0.5f, 0.2f));

            DisplayBuilder.playSound(pos, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 0.5f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(80, 180, 200).interpolation(3, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            float bob = (float) Math.sin(ticksAlive * 0.04) * 0.5f;

            if (ticksAlive > 80) channeling = true;

            // Soul flame particles from orb
            if (ticksAlive % 4 == 0) {
                Location orbLoc = c.clone().add(3, 5.5 + bob, 0);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, orbLoc, 3, 0.3, 0.3, 0.3, 0.02);
            }

            // Channel beam toward player
            if (channeling && ticksAlive % 3 == 0) {
                Location orbLoc = c.clone().add(3, 5.5 + bob, 0);
                DisplayBuilder.particleLine(orbLoc, c.clone().add(0, 2, 0),
                        Particle.SOUL_FIRE_FLAME, 3, null);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(3, 5, 0), Sound.BLOCK_BEACON_AMBIENT, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulflameStaff(plugin); }
    }

    // ================================================================
    // 10. DEMONIC FLAIL — Handle (3), chain (4), spiked ball (3) = 10.
    //     Ball swings in circles on the chain, widening arc.
    // ================================================================
    public static class DemonicFlail extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> handleParts = new ArrayList<>();
        private final List<BlockDisplayHandle> chainParts = new ArrayList<>();
        private final List<BlockDisplayHandle> ballParts = new ArrayList<>();
        private double swingAngle = 0;

        public DemonicFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("demonic_flail", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 5, 0);

            for (int i = 0; i < 3; i++) {
                handleParts.add(spawnP(pos, 0, -i * 1.0, 0, Material.NETHER_BRICKS, 0.4f, 1.0f, 0.4f));
            }
            for (int i = 0; i < 4; i++) {
                chainParts.add(spawnP(pos, 0, -3 - i * 0.8, 0, Material.CHAIN, 0.25f, 0.8f, 0.25f));
            }
            ballParts.add(spawnP(pos, 0, -6.5, 0, Material.BLACKSTONE, 1.5f, 1.5f, 1.5f));
            ballParts.add(spawnP(pos, -0.5, -6.5, 0.5, Material.NETHERRACK, 0.5f, 0.8f, 0.5f));
            ballParts.add(spawnP(pos, 0.5, -6.5, -0.5, Material.NETHERRACK, 0.5f, 0.8f, 0.5f));

            DisplayBuilder.playSound(pos, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(100, 40, 40).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location pivot = c.clone().add(0, 5, 0);

            swingAngle += 0.07;
            double swingRadius = 3.0 + Math.sin(ticksAlive * 0.02) * 1.5;
            double ballX = Math.cos(swingAngle) * swingRadius;
            double ballZ = Math.sin(swingAngle) * swingRadius;
            double ballY = -3.0 - Math.abs(Math.sin(swingAngle)) * 2.0;

            // Chain follows from handle to ball
            for (int i = 0; i < chainParts.size(); i++) {
                double t = (double)(i + 1) / (chainParts.size() + 1);
                chainParts.get(i).entity().teleport(pivot.clone().add(
                        ballX * t, -3 + ballY * t * 0.3, ballZ * t));
            }

            // Ball
            Location ballLoc = pivot.clone().add(ballX, ballY, ballZ);
            ballParts.get(0).entity().teleport(ballLoc);
            ballParts.get(1).entity().teleport(ballLoc.clone().add(-0.5, 0, 0.5));
            ballParts.get(2).entity().teleport(ballLoc.clone().add(0.5, 0, -0.5));

            if (ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.CRIT, ballLoc, 3, 0.3, 0.3, 0.3, 0.05);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(pivot, Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DemonicFlail(plugin); }
    }

    // ================================================================
    // 11. NIGHTMARE CROSSBOW — Frame (5), string (2), bolt (3) = 10.
    //     Charges then fires a bolt at the player.
    // ================================================================
    public static class NightmareCrossbow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private boolean fired = false;
        private float boltZ = 0;

        public NightmareCrossbow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_crossbow", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 3, -6);

            // Stock (3)
            for (int i = 0; i < 3; i++) {
                parts.add(spawnP(pos, 0, 0, -i * 1.2, Material.CRIMSON_PLANKS, 0.5f, 0.4f, 1.2f));
            }
            // Limbs (2 — forming the bow part)
            parts.add(spawnP(pos, -2.0, 0, 0, Material.NETHER_BRICKS, 2.0f, 0.3f, 0.3f));
            parts.add(spawnP(pos, 2.0, 0, 0, Material.NETHER_BRICKS, 2.0f, 0.3f, 0.3f));
            // String (2)
            parts.add(spawnP(pos, -1.5, 0, 0.3, Material.COBWEB, 0.1f, 0.1f, 0.5f));
            parts.add(spawnP(pos, 1.5, 0, 0.3, Material.COBWEB, 0.1f, 0.1f, 0.5f));
            // Bolt (3)
            parts.add(spawnP(pos, 0, 0, 1.0, Material.BONE_BLOCK, 0.15f, 0.15f, 2.0f));
            parts.add(spawnP(pos, 0, 0, 3.0, Material.NETHERRACK, 0.25f, 0.25f, 0.5f));
            parts.add(spawnP(pos, 0, 0, -0.5, Material.CRIMSON_PLANKS, 0.3f, 0.3f, 0.2f));

            DisplayBuilder.playSound(pos, Sound.ITEM_CROSSBOW_LOADING_START, 0.8f, 0.4f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(120, 40, 40).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive == 50) {
                fired = true;
                DisplayBuilder.playSound(c, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 0.4f);
            }

            if (fired) {
                boltZ += 0.7f;
                Location boltLoc = c.clone().add(0, 3, -6 + boltZ);
                parts.get(7).entity().teleport(boltLoc.clone().add(0, 0, 1));
                parts.get(8).entity().teleport(boltLoc.clone().add(0, 0, 3));
                parts.get(9).entity().teleport(boltLoc.clone().add(0, 0, -0.5));

                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, boltLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareCrossbow(plugin); }
    }

    // ================================================================
    // 12. HELLFIRE SHIELD — Circular shield (8 around + 2 center) = 10.
    //     Floats in front, then bashes toward the player.
    // ================================================================
    public static class HellfireShield extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float bashProgress = 0;
        private boolean bashing = false;

        public HellfireShield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_shield", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, 3, -5);

            // Rim (8 blocks in circle)
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * 2.0;
                double y = Math.sin(angle) * 2.0;
                Material mat = (i % 2 == 0) ? Material.RED_NETHER_BRICKS : Material.BLACKSTONE;
                BlockDisplayHandle b = displayBuilder.spawnBlock(pos.clone().add(x, y, 0), mat);
                b.scale(1.0f, 1.0f, 0.4f).glow(180, 50, 30).interpolation(2, 0);
                parts.add(b);
                spawnedEntities.add(b.entity());
            }

            // Center boss (2)
            parts.add(spawnP(pos, 0, 0, 0.2, Material.MAGMA_BLOCK, 1.5f, 1.5f, 0.5f));
            parts.add(spawnP(pos, 0, 0, 0.4, Material.GOLD_BLOCK, 0.8f, 0.8f, 0.3f));

            DisplayBuilder.playSound(pos, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(255, 120, 20).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive == 60) bashing = true;

            float zOffset = -5;
            if (bashing) {
                bashProgress += 0.03f;
                zOffset = -5 + bashProgress * 8;
            }

            Location pos = c.clone().add(0, 3, zOffset);
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                parts.get(i).entity().teleport(pos.clone().add(Math.cos(angle) * 2, Math.sin(angle) * 2, 0));
            }
            parts.get(8).entity().teleport(pos.clone().add(0, 0, 0.2));
            parts.get(9).entity().teleport(pos.clone().add(0, 0, 0.4));

            if (bashing && ticksAlive % 3 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME, pos, 5, 1, 1, 0.3, 0.03);
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(c, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new HellfireShield(plugin); }
    }

    // ================================================================
    // 13. DEVIL'S TRIDENT — 3 prongs (2 each = 6), shaft (3),
    //     crossguard (1) = 10. Spins vertically then hurls downward.
    // ================================================================
    public static class DevilsTrident extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> parts = new ArrayList<>();
        private float spinAngle = 0;
        private boolean thrown = false;
        private float throwY = 12;

        public DevilsTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_trident", AttackType.BLOCK_DISPLAY, 1, "modes/devilsdream/attacks"));
            config.setDamage(55.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(300);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(85.0);
            config.setImpactRadius(7.0);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location pos = center.clone().add(0, throwY, 0);

            // Shaft (3)
            for (int i = 0; i < 3; i++) {
                parts.add(spawnP(pos, 0, -i * 1.5, 0, Material.NETHER_BRICKS, 0.4f, 1.5f, 0.4f));
            }
            // Crossguard
            parts.add(spawnP(pos, 0, 0.5, 0, Material.GOLD_BLOCK, 2.5f, 0.3f, 0.3f));
            // 3 prongs (2 segments each)
            for (int p = -1; p <= 1; p++) {
                parts.add(spawnP(pos, p * 0.8, 1.5, 0, Material.RED_NETHER_BRICKS, 0.3f, 1.5f, 0.3f));
                parts.add(spawnP(pos, p * 0.8, 3.0, 0, Material.MAGMA_BLOCK, 0.2f, 1.0f, 0.2f));
            }

            DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_THROW, 0.8f, 0.3f);
        }

        private BlockDisplayHandle spawnP(Location base, double ox, double oy, double oz,
                                          Material mat, float sx, float sy, float sz) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(base.clone().add(ox, oy, oz), mat);
            h.scale(sx, sy, sz).glow(200, 80, 30).interpolation(2, 0);
            spawnedEntities.add(h.entity());
            return h;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive < 60) {
                spinAngle += 0.15f;
                throwY = 12 + (float) Math.sin(ticksAlive * 0.05) * 0.5f;
            } else {
                thrown = true;
                throwY -= 0.6f;
                if (throwY <= 1.0f) {
                    throwY = 1.0f;
                    triggerImpactDamage(c);
                    c.getWorld().spawnParticle(Particle.EXPLOSION, c, 2, 0.5, 0.5, 0.5, 0);
                    DisplayBuilder.particleRing(c, 6.0, Particle.FLAME, 24, null);
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 1.0f, 0.4f);
                }
            }

            // Rotate all parts
            for (BlockDisplayHandle part : parts) {
                part.rotate(spinAngle, 0, 0, 1);
            }

            if (thrown && ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.FLAME,
                        c.clone().add(0, throwY, 0), 5, 0.3, 0.5, 0.3, 0.03);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DevilsTrident(plugin); }
    }
}
