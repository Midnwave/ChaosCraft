package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * FreezingIce Mode — BLOCK DISPLAY ATTACKS 11-20 (Weapons & Tools theme)
 *
 * War-machine and bladed-weapon themed ice structures. Each attack uses 30+
 * BlockDisplay entities animated via Transformation (AxisAngle4f rotation,
 * Vector3f scale/translation). Never teleport for rotation — use
 * setInterpolationDuration for smooth animation.
 *
 * Ice palette:
 *  PACKED_ICE, BLUE_ICE, ICE, FROSTED_ICE, SNOW_BLOCK, TINTED_GLASS,
 *  BLUE_STAINED_GLASS, LIGHT_BLUE_CONCRETE, CYAN_CONCRETE, BLUE_CONCRETE,
 *  AMETHYST_BLOCK, CALCITE, QUARTZ_BLOCK, SMOOTH_QUARTZ, DIAMOND_BLOCK,
 *  WHITE_CONCRETE, WHITE_WOOL
 *
 * Particles: SNOWFLAKE, ITEM_SNOWBALL, ELECTRIC_SPARK, CLOUD, FALLING_DUST
 * Sounds: ITEM_TRIDENT_THROW, BLOCK_GLASS_BREAK, BLOCK_GLASS_HIT,
 *         BLOCK_GLASS_PLACE, ITEM_ARMOR_EQUIP_IRON, ENTITY_GENERIC_EXPLODE,
 *         BLOCK_NOTE_BLOCK_CHIME
 *
 * Attacks:
 *  11. IceBallistaVolley      — 43 blocks, war-machine bolt launcher, impact-only
 *  12. GlacierCannonBarrage   — 33 blocks, mounted ice cannon, impact-only
 *  13. FrostHalberd           — 34 blocks, planted polearm spinning, impact + constant
 *  14. CryoCatapult           — 44 blocks, multi-bolt wall rack volley, impact-only
 *  15. FrozenChakramWheel     — 35 blocks, throwing disc figure-8 sweep, constant
 *  16. IceMaceDescend         — 34 blocks, giant warhammer slam, impact-only
 *  17. FrostScytheReap        — 32 blocks, curved blade triple-slash, impact-only
 *  18. HailstormCrossbow      — 39 blocks, oversized crossbow bolt fire, impact-only
 *  19. CryoLanceCharge        — 30 blocks, horizontal lance charging sweep, impact + constant
 *  20. FrozenFlailSwing       — 30 blocks, spiked-ball flail orbit, constant
 *
 * None of these attacks use follow-AI.
 */
public final class FreezingIceBlockDisplay2 {
    private FreezingIceBlockDisplay2() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceBallistaVolley(plugin));
        registry.register(new GlacierCannonBarrage(plugin));
        registry.register(new FrostHalberd(plugin));
        registry.register(new CryoCatapult(plugin));
        registry.register(new FrozenChakramWheel(plugin));
        registry.register(new IceMaceDescend(plugin));
        registry.register(new FrostScytheReap(plugin));
        registry.register(new HailstormCrossbow(plugin));
        registry.register(new CryoLanceCharge(plugin));
        registry.register(new FrozenFlailSwing(plugin));
    }

    // ================================================================
    // Helper: find nearest non-exempt survival player within range
    // ================================================================
    private static Player findNearestPlayer(Location center, double range) {
        if (center.getWorld() == null) return null;
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

    // ================================================================
    // #11 — ICE BALLISTA VOLLEY
    // 43 blocks: base platform 6 SMOOTH_QUARTZ, 2 side-arms 5 BLUE_ICE each
    // (10), central bow shaft 4 QUARTZ_BLOCK, bowstring 6 TINTED_GLASS,
    // loaded bolt 5 PACKED_ICE, 2 wheel rings of 6 segments each (12).
    // Frame assembles bottom-up, bowstring draws, bolt fires.
    // Impact-only 7.0r 240 dmg; constant 5.0r 60 dmg/16t around ballista.
    // ================================================================
    public static class IceBallistaVolley extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> baseBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> sideArms = new ArrayList<>();
        private final List<BlockDisplayHandle> bowShaft = new ArrayList<>();
        private final List<BlockDisplayHandle> bowstring = new ArrayList<>();
        private final List<BlockDisplayHandle> boltBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> wheelRings = new ArrayList<>();
        private boolean fired = false;
        private float drawAmount = 0f;
        private float boltTravel = 0f;

        public IceBallistaVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_ballista_volley", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(240.0);
            config.setImpactRadius(7.0);
            config.setDamage(60.0);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(16);
            config.setDamageDelayTicks(35);
            config.setDurationTicks(220);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Base platform: 6 SMOOTH_QUARTZ in 2x3 grid
            for (int x = -1; x <= 1; x++) {
                for (int z = 0; z < 2; z++) {
                    Location loc = center.clone().add(x * 0.8, 0, z * 0.8 - 0.4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                    h.scale(1.0f, 0.4f, 1.0f).glow(220, 230, 240).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    baseBlocks.add(h);
                }
            }

            // 2 side arms — 5 BLUE_ICE each, tapered, angled outward (V shape)
            for (int side = -1; side <= 1; side += 2) {
                for (int s = 0; s < 5; s++) {
                    double ax = side * (0.6 + s * 0.4);
                    double ay = 0.8 + s * 0.35;
                    Location loc = center.clone().add(ax, ay, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float taper = 0.6f - s * 0.06f;
                    h.scale(taper, 0.3f, 0.3f).glow(80, 140, 220).interpolation(8, 2);
                    spawnedEntities.add(h.entity());
                    sideArms.add(h);
                }
            }

            // Central bow shaft: 4 QUARTZ_BLOCK extending forward
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 0.7, s * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.4f, 0.4f, 0.7f).glow(240, 240, 250).interpolation(8, 4);
                spawnedEntities.add(h.entity());
                bowShaft.add(h);
            }

            // Bowstring: 6 TINTED_GLASS thin segments at the back of the bow
            for (int s = 0; s < 6; s++) {
                double sy = 0.3 + s * 0.25;
                Location loc = center.clone().add(0, sy + 0.5, -0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.6f, 0.1f, 0.1f).glow(180, 200, 230).interpolation(6, 6);
                spawnedEntities.add(h.entity());
                bowstring.add(h);
            }

            // Loaded bolt: 5 PACKED_ICE tapered, pointing forward
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(0, 1.2, s * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                float boltTaper = 0.4f - s * 0.05f;
                h.scale(boltTaper, boltTaper, 0.35f).glow(140, 200, 240).interpolation(6, 8);
                spawnedEntities.add(h.entity());
                boltBlocks.add(h);
            }

            // 2 wheel rings (6 segments each) on each side of base
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 6; i++) {
                    double a = (2.0 * Math.PI * i) / 6.0;
                    double px = Math.cos(a) * 0.7;
                    double py = Math.sin(a) * 0.7 + 0.2;
                    Location loc = center.clone().add(side * 1.3, py, px);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                    h.scale(0.3f, 0.3f, 0.3f).glow(230, 230, 240).interpolation(8, 0);
                    spawnedEntities.add(h.entity());
                    wheelRings.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.8f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1.5, 0), 30, 1.5, 1.5, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Draw bowstring back over ticks 40-80 (translate -Z)
            if (tick >= 40 && tick <= 80) {
                drawAmount = (tick - 40) / 40.0f;
                if (tick % 4 == 0) {
                    float pull = -0.8f * drawAmount;
                    for (BlockDisplayHandle h : bowstring) {
                        applyTranslate(h.entity(), 0f, 0f, pull, 6);
                    }
                    for (BlockDisplayHandle h : boltBlocks) {
                        applyTranslate(h.entity(), 0f, 0f, pull * 0.5f, 6);
                    }
                    if (tick % 8 == 0) {
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                                c.clone().add(0, 1.0, -0.4), 4, 0.2, 0.4, 0.2, 0.02);
                        DisplayBuilder.playSound(c, Sound.ITEM_ARMOR_EQUIP_IRON, 0.5f, 1.6f);
                    }
                }
            }

            // Aim toward player — yaw rotation applied to bow shaft + bolt + string
            if (tick == 60) {
                Player target = findNearestPlayer(c, 30);
                if (target != null) {
                    Location pl = target.getLocation();
                    double dx = pl.getX() - c.getX();
                    double dz = pl.getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dx, dz);
                    rotateGroup(bowShaft, yaw, 0f, 1f, 0f, 10);
                    rotateGroup(boltBlocks, yaw, 0f, 1f, 0f, 10);
                }
            }

            // Fire the bolt at tick 90 — bolt translates +Z fast
            if (tick == 90 && !fired) {
                fired = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.6f, 1.4f);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL,
                        c.clone().add(0, 1.2, 0), 40, 0.3, 0.3, 0.3, 0.1);
            }

            // Bolt flies forward ticks 90-110, impact at 110
            if (tick > 90 && tick <= 110) {
                boltTravel += 0.8f;
                if (tick % 2 == 0) {
                    for (BlockDisplayHandle h : boltBlocks) {
                        applyTranslate(h.entity(), 0f, 0f, boltTravel, 2);
                    }
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(0, 1.2, boltTravel), 6, 0.15, 0.15, 0.15, 0.02);
                }
            }

            // Impact at tick 110
            if (tick == 110) {
                Location impact = c.clone().add(0, 1.0, boltTravel);
                triggerImpactDamage(impact);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.5f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        impact, 60, 2.5, 1.5, 2.5, 0.1, Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 50, 3, 1.5, 3, 0.1);
            }

            // Idle ambient snowflakes around ballista
            if (tick > 110 && tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.0, 0), 3, 1.5, 0.5, 1.5, 0.02);
            }

            // Topple at duration end — translate down + scatter
            if (tick > 195 && tick % 3 == 0) {
                float toppleY = -((tick - 195) / 25.0f) * 2.0f;
                for (BlockDisplayHandle h : sideArms) applyTranslate(h.entity(), 0f, toppleY, 0f, 3);
                for (BlockDisplayHandle h : bowShaft) applyTranslate(h.entity(), 0f, toppleY * 0.5f, 0f, 3);
            }
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az, int dur) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
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
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceBallistaVolley(plugin); }
    }

    // ================================================================
    // #12 — GLACIER CANNON BARRAGE
    // 33 blocks: barrel 6 PACKED_ICE, muzzle ring 1 BLUE_ICE,
    // carriage 4 SMOOTH_QUARTZ, 2 cartwheels 6 segments each (12),
    // back-brace 4 QUARTZ, 4 muzzle-spike DIAMOND, 2 trunnion bolts CALCITE.
    // Aims, recoils, releases massive ice-orb projectile. Impact-only 10.0r, 280 dmg.
    // ================================================================
    public static class GlacierCannonBarrage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> barrel = new ArrayList<>();
        private final List<BlockDisplayHandle> muzzleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> carriage = new ArrayList<>();
        private final List<BlockDisplayHandle> wheels = new ArrayList<>();
        private final List<BlockDisplayHandle> backBrace = new ArrayList<>();
        private final List<BlockDisplayHandle> muzzleSpikes = new ArrayList<>();
        private boolean fired = false;
        private float recoil = 0f;
        private float projTravel = 0f;

        public GlacierCannonBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_cannon_barrage", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(280.0);
            config.setImpactRadius(10.0);
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(180);
            config.setCooldownTicks(150);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Carriage: 4 SMOOTH_QUARTZ trunnion
            double[][] carriageOffsets = {{-0.8, 0.4, 0}, {0.8, 0.4, 0}, {-0.4, 0.8, 0}, {0.4, 0.8, 0}};
            for (double[] off : carriageOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.6f, 0.6f, 1.2f).glow(220, 230, 240).interpolation(8, 0);
                spawnedEntities.add(h.entity());
                carriage.add(h);
            }

            // 2 cartwheels — 6 segments each
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 6; i++) {
                    double a = (2.0 * Math.PI * i) / 6.0;
                    double py = Math.sin(a) * 0.7 + 0.3;
                    double pz = Math.cos(a) * 0.7;
                    Location loc = center.clone().add(side * 1.2, py, pz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                    h.scale(0.25f, 0.25f, 0.25f).glow(230, 230, 240).interpolation(8, 2);
                    spawnedEntities.add(h.entity());
                    wheels.add(h);
                }
            }

            // Barrel: 6 PACKED_ICE segments extending forward
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0, 1.3, s * 0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.7f, 0.7f, 0.55f).glow(120, 180, 230).interpolation(8, 6);
                spawnedEntities.add(h.entity());
                barrel.add(h);
            }

            // Muzzle ring: 1 BLUE_ICE hollow ring at the front (scaled wide flat)
            Location muzzleLoc = center.clone().add(0, 1.3, 3.8);
            BlockDisplayHandle muzzle = displayBuilder.spawnBlock(muzzleLoc, Material.BLUE_ICE);
            muzzle.scale(1.0f, 1.0f, 0.2f).glow(80, 140, 220).interpolation(8, 10);
            spawnedEntities.add(muzzle.entity());
            muzzleRing.add(muzzle);

            // Back brace: 4 QUARTZ
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 1.3, -0.6 - s * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.45f, 0.45f, 0.3f).glow(240, 240, 250).interpolation(8, 4);
                spawnedEntities.add(h.entity());
                backBrace.add(h);
            }

            // Muzzle spikes: 4 DIAMOND_BLOCK pointing outward at muzzle
            double[][] spikeOffsets = {{0.7, 0, 0}, {-0.7, 0, 0}, {0, 0.7, 0}, {0, -0.7, 0}};
            for (double[] off : spikeOffsets) {
                Location loc = center.clone().add(off[0], 1.3 + off[1], 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.5f).glow(180, 230, 255).interpolation(8, 12);
                spawnedEntities.add(h.entity());
                muzzleSpikes.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1.5, 2), 25, 1, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Aim phase: tick 25 — yaw toward player
            if (tick == 25) {
                Player target = findNearestPlayer(c, 30);
                if (target != null) {
                    Location pl = target.getLocation();
                    double dx = pl.getX() - c.getX();
                    double dz = pl.getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dx, dz);
                    rotateGroup(barrel, yaw, 0f, 1f, 0f, 25);
                    rotateGroup(muzzleRing, yaw, 0f, 1f, 0f, 25);
                    rotateGroup(muzzleSpikes, yaw, 0f, 1f, 0f, 25);
                }
                DisplayBuilder.playSound(c, Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.7f);
            }

            // Fire at tick 60 — recoil + projectile launch
            if (tick == 60 && !fired) {
                fired = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
                c.getWorld().spawnParticle(Particle.CLOUD,
                        c.clone().add(0, 1.5, 4), 80, 1.5, 1.5, 1.5, 0.3);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(0, 1.5, 4), 60, 1, 1, 1, 0.4);
            }

            // Recoil: barrel snaps back -0.8 ticks 60-65, returns over ticks 65-105
            if (tick >= 60 && tick <= 65 && tick % 2 == 0) {
                recoil = -0.8f * ((tick - 60) / 5.0f);
                for (BlockDisplayHandle h : barrel) {
                    applyTranslate(h.entity(), 0f, 0f, recoil, 2);
                }
            }
            if (tick > 65 && tick <= 105 && tick % 4 == 0) {
                recoil = -0.8f * (1.0f - (tick - 65) / 40.0f);
                for (BlockDisplayHandle h : barrel) {
                    applyTranslate(h.entity(), 0f, 0f, recoil, 4);
                }
            }

            // Ice-orb projectile trail (no separate entity — use particle column traveling forward)
            if (tick >= 60 && tick <= 90) {
                projTravel += 0.7f;
                Location orbLoc = c.clone().add(0, 1.5, 4 + projTravel);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, orbLoc, 10, 0.4, 0.4, 0.4, 0.05);
                c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, orbLoc, 6, 0.3, 0.3, 0.3, 0.02);
            }

            // Impact at tick 90
            if (tick == 90) {
                Location impact = c.clone().add(0, 1.0, 4 + projTravel);
                triggerImpactDamage(impact);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.4f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        impact, 80, 3.5, 1.5, 3.5, 0.1, Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 80, 4, 2, 4, 0.1);
            }

            // Falls apart phase: ticks 155+
            if (tick > 155 && tick % 4 == 0) {
                float fall = -((tick - 155) / 25.0f) * 1.5f;
                for (BlockDisplayHandle h : barrel) applyTranslate(h.entity(), 0f, fall, 0f, 4);
                for (BlockDisplayHandle h : muzzleSpikes) applyTranslate(h.entity(), 0f, fall, 0f, 4);
            }
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az, int dur) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
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
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new GlacierCannonBarrage(plugin); }
    }

    // ================================================================
    // #13 — FROST HALBERD
    // 34 blocks: shaft 12 SMOOTH_QUARTZ vertical, blade 6 BLUE_ICE axe-curve,
    // back-hook 4 PACKED_ICE, spike-tip 5 DIAMOND_BLOCK, pommel 3 CALCITE,
    // haft grip 4 TINTED_GLASS.
    // Falls from Y+18 plant-stab; blade rotates around shaft axis.
    // Impact-only 8.0r 200 dmg at plant; constant 7.5r 70 dmg/8t during spin.
    // ================================================================
    public static class FrostHalberd extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private final List<BlockDisplayHandle> backHook = new ArrayList<>();
        private final List<BlockDisplayHandle> spikeTip = new ArrayList<>();
        private final List<BlockDisplayHandle> pommel = new ArrayList<>();
        private final List<BlockDisplayHandle> haftGrip = new ArrayList<>();
        private boolean planted = false;
        private float spinAngle = 0f;
        private float floatY = 0f;

        public FrostHalberd(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_halberd", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(200.0);
            config.setImpactRadius(8.0);
            config.setDamage(70.0);
            config.setDamageRadius(7.5);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(30);
            config.setDurationTicks(220);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Shaft: 12 SMOOTH_QUARTZ stacked vertical, initially at Y+18, falling
            for (int s = 0; s < 12; s++) {
                Location loc = center.clone().add(0, 18 + s * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.35f, 0.8f, 0.35f).glow(230, 240, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                shaft.add(h);
            }

            // Blade: 6 BLUE_ICE axe-curve at top
            for (int s = 0; s < 6; s++) {
                double curve = Math.sin(s * 0.4) * 1.0;
                Location loc = center.clone().add(0.6 + curve * 0.3, 18 + 10.5 + s * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(1.1f, 0.2f, 1.4f).glow(80, 150, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                blade.add(h);
            }

            // Back-hook: 4 PACKED_ICE curved opposite blade
            for (int s = 0; s < 4; s++) {
                double curve = -Math.sin(s * 0.5) * 0.8;
                Location loc = center.clone().add(-0.6 + curve, 18 + 10.5 + s * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.3f, 0.3f, 0.3f).glow(140, 200, 240).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                backHook.add(h);
            }

            // Spike-tip: 5 DIAMOND_BLOCK tapered above blade
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(0, 18 + 12.5 + s * 0.35, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                float taper = 0.3f - s * 0.04f;
                h.scale(taper, 0.3f, taper).glow(190, 235, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                spikeTip.add(h);
            }

            // Pommel: 3 CALCITE cap at bottom of shaft
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0, 18 - 0.5 - s * 0.25, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.6f, 0.25f, 0.6f).glow(240, 240, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                pommel.add(h);
            }

            // Haft grip: 4 TINTED_GLASS along mid-shaft
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 18 + 4.5 + s * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 0.25f, 0.4f).glow(160, 180, 210).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                haftGrip.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.6f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 22, 0), 30, 1, 3, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Fall phase: ticks 0-30, translate everything down by 18
            if (tick <= 30 && !planted) {
                int step = tick / 5; // 0..6
                float[] dropTable = {0f, -3f, -7f, -11f, -14f, -16f, -18f};
                if (tick % 5 == 0 && step < dropTable.length) {
                    float dropY = dropTable[step];
                    translateAll(dropY);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(0, 18 + dropY + 5, 0), 12, 0.3, 1.5, 0.3, 0.05);
                }
                if (tick == 30) {
                    planted = true;
                    Location impact = c.clone();
                    triggerImpactDamage(impact);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.4f);
                    c.getWorld().spawnParticle(Particle.FALLING_DUST,
                            impact, 50, 2.5, 0.5, 2.5, 0.1, Material.WHITE_CONCRETE.createBlockData());
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 60, 3.5, 1, 3.5, 0.1);
                }
            }

            // Spin phase: ticks 30+, blade rotates around shaft Y-axis every 40t (one rotation)
            if (tick > 30 && tick < 210 && tick % 2 == 0) {
                spinAngle += (float) (Math.PI * 2 / 20.0); // 40t for 360 deg at every-2-tick = 20 steps
                rotateBladeGroup(blade, spinAngle);
                rotateBladeGroup(backHook, spinAngle);

                // Float Y +/- 0.2
                floatY = (float) Math.sin(tick * 0.1) * 0.2f;
                for (BlockDisplayHandle h : shaft) applyTranslate(h.entity(), 0f, -18f + floatY, 0f, 2);
            }

            // ELECTRIC_SPARK along blade edge during spin
            if (tick > 30 && tick % 3 == 0) {
                double a = spinAngle;
                double px = Math.cos(a) * 1.2;
                double pz = Math.sin(a) * 1.2;
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(px, 11.0, pz), 4, 0.2, 0.2, 0.2, 0.05);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(px, 11.0, pz), 3, 0.15, 0.15, 0.15, 0.02);
            }

            if (tick > 30 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 0.8f, 1.2f);
            }
        }

        private void translateAll(float dropY) {
            for (BlockDisplayHandle h : shaft) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
            for (BlockDisplayHandle h : blade) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
            for (BlockDisplayHandle h : backHook) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
            for (BlockDisplayHandle h : spikeTip) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
            for (BlockDisplayHandle h : pommel) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
            for (BlockDisplayHandle h : haftGrip) applyTranslate(h.entity(), 0f, dropY, 0f, 5);
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateBladeGroup(List<BlockDisplayHandle> group, float angle) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, 0f, 1f, 0f),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostHalberd(plugin); }
    }

    // ================================================================
    // #14 — CRYO CATAPULT
    // 44 blocks: frame 4 SMOOTH_QUARTZ, 9 icicle-bolts 3x3 grid (each 4 PACKED_ICE = 36),
    // cross-brace 4 LIGHT_BLUE_CONCRETE.
    // Frame assembles, bolts slot in, then volley fires in 3 waves of 3 bolts.
    // Impact-only 6.0r 110 dmg per bolt landing (stackable).
    // ================================================================
    public static class CryoCatapult extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<List<BlockDisplayHandle>> bolts = new ArrayList<>(); // 9 bolts, each is a list of 4 segments
        private final List<BlockDisplayHandle> crossBrace = new ArrayList<>();
        private int wave = 0;

        public CryoCatapult(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_catapult", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(110.0);
            config.setImpactRadius(6.0);
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(150);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Frame: 4 SMOOTH_QUARTZ corner posts
            double[][] frameOffsets = {{-1.5, 0, -0.5}, {1.5, 0, -0.5}, {-1.5, 2.5, -0.5}, {1.5, 2.5, -0.5}};
            for (double[] off : frameOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.4f, 0.4f, 0.4f).glow(220, 230, 240).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                frame.add(h);
            }

            // 9 bolts in 3x3 grid — each bolt = 4 PACKED_ICE segments tapered
            int delayBase = 8;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    List<BlockDisplayHandle> boltList = new ArrayList<>();
                    double bx = (col - 1) * 0.9;
                    double by = 0.5 + row * 0.8;
                    for (int s = 0; s < 4; s++) {
                        Location loc = center.clone().add(bx, by, -0.3 + s * 0.4);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                        float taper = 0.3f - s * 0.04f;
                        h.scale(taper, taper, 0.35f).glow(140, 200, 240).interpolation(6, delayBase + (row * 3 + col));
                        spawnedEntities.add(h.entity());
                        boltList.add(h);
                    }
                    bolts.add(boltList);
                }
            }

            // Cross-brace: 4 LIGHT_BLUE_CONCRETE
            double[][] braceOffsets = {{-1.5, 1.2, -0.5}, {1.5, 1.2, -0.5}, {0, 1.2, -0.5}, {0, 2.5, -0.5}};
            for (double[] off : braceOffsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(1.5f, 0.2f, 0.2f).glow(120, 200, 240).interpolation(6, 4);
                spawnedEntities.add(h.entity());
                crossBrace.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.9f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 1.5, 0), 25, 1.5, 1, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Volley waves: tick 40 = wave 1 (bolts 0-2), tick 60 = wave 2 (bolts 3-5), tick 80 = wave 3 (bolts 6-8)
            if ((tick == 40 || tick == 60 || tick == 80) && wave < 3) {
                int start = wave * 3;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.4f, 1.2f);
                for (int i = start; i < start + 3 && i < bolts.size(); i++) {
                    List<BlockDisplayHandle> bolt = bolts.get(i);
                    for (BlockDisplayHandle h : bolt) {
                        applyTranslate(h.entity(), 0f, 0f, 18f, 25);
                    }
                }
                wave++;
            }

            // Particle trails behind active bolts during travel
            for (int i = 0; i < Math.min(wave * 3, bolts.size()); i++) {
                int volleyStart = 40 + ((i / 3) * 20);
                int volleyEnd = volleyStart + 25;
                if (tick >= volleyStart && tick <= volleyEnd && tick % 2 == 0) {
                    List<BlockDisplayHandle> bolt = bolts.get(i);
                    for (BlockDisplayHandle h : bolt) {
                        Location loc = h.entity().getLocation();
                        c.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                // Impact when travel completes
                if (tick == volleyEnd) {
                    int colInGrid = i % 3;
                    Location impact = c.clone().add((colInGrid - 1) * 0.9, 0.5, 18);
                    triggerImpactDamage(impact);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.7f);
                    c.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, impact, 30, 1.5, 1, 1.5, 0.1);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 40, 2, 1, 2, 0.1);
                }
            }

            // Empty-frame collapse at end
            if (tick > 175 && tick % 4 == 0) {
                float fall = -((tick - 175) / 20.0f) * 1.0f;
                for (BlockDisplayHandle h : frame) applyTranslate(h.entity(), 0f, fall, 0f, 4);
                for (BlockDisplayHandle h : crossBrace) applyTranslate(h.entity(), 0f, fall, 0f, 4);
            }
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CryoCatapult(plugin); }
    }

    // ================================================================
    // #15 — FROZEN CHAKRAM WHEEL
    // 35 blocks: main ring 16 BLUE_ICE 360 deg circle, inner spokes 6 TINTED_GLASS,
    // outer blade-teeth 8 AMETHYST_BLOCK, central hub 1 PACKED_ICE + 4 DIAMOND caps.
    // Spinning chakram performs figure-8 sweep around spawn point.
    // Constant 7.0r, 110 dmg, 6t interval, 12t delay.
    // ================================================================
    public static class FrozenChakramWheel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mainRing = new ArrayList<>();
        private final List<BlockDisplayHandle> spokes = new ArrayList<>();
        private final List<BlockDisplayHandle> bladeTeeth = new ArrayList<>();
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private float spinAngle = 0f;

        public FrozenChakramWheel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_chakram_wheel", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(110.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(6);
            config.setDamageDelayTicks(12);
            config.setDurationTicks(220);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            double radius = 2.5;

            // Main ring: 16 BLUE_ICE blocks 360 deg
            for (int i = 0; i < 16; i++) {
                double a = (2.0 * Math.PI * i) / 16.0;
                double px = Math.cos(a) * radius;
                double pz = Math.sin(a) * radius;
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.3f, 0.3f, 0.3f).glow(80, 150, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                mainRing.add(h);
            }

            // Inner spokes: 6 TINTED_GLASS thin radial
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                double px = Math.cos(a) * (radius * 0.5);
                double pz = Math.sin(a) * (radius * 0.5);
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.15f, 0.1f, 0.4f).glow(180, 200, 230).interpolation(4, 0);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                spokes.add(h);
            }

            // Outer blade-teeth: 8 AMETHYST_BLOCK
            for (int i = 0; i < 8; i++) {
                double a = (2.0 * Math.PI * i) / 8.0;
                double px = Math.cos(a) * (radius + 0.5);
                double pz = Math.sin(a) * (radius + 0.5);
                Location loc = center.clone().add(px, 3.0, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.15f, 0.8f).glow(180, 130, 220).interpolation(4, 0);
                h.rotate((float) a, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                bladeTeeth.add(h);
            }

            // Central hub: 1 PACKED_ICE + 4 DIAMOND caps
            Location hubLoc = center.clone().add(0, 3.0, 0);
            BlockDisplayHandle hubBlock = displayBuilder.spawnBlock(hubLoc, Material.PACKED_ICE);
            hubBlock.scale(0.6f, 0.6f, 0.6f).glow(140, 200, 240).interpolation(4, 0);
            spawnedEntities.add(hubBlock.entity());
            hub.add(hubBlock);
            double[][] capOffsets = {{0.4, 0, 0}, {-0.4, 0, 0}, {0, 0, 0.4}, {0, 0, -0.4}};
            for (double[] off : capOffsets) {
                Location loc = center.clone().add(off[0], 3.0, off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(190, 235, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                hub.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 1.0f, 1.4f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 3.0, 0), 30, 2, 0.5, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spin fast Y-axis 30 deg/tick (~0.52 rad/tick)
            spinAngle += 0.52f;
            if (tick % 2 == 0) {
                rotateGroup(mainRing, spinAngle, 0f, 1f, 0f);
                rotateGroup(spokes, spinAngle, 0f, 1f, 0f);
                rotateGroup(bladeTeeth, spinAngle, 0f, 1f, 0f);
                rotateGroup(hub, spinAngle, 0f, 1f, 0f);
            }

            // Figure-8 motion — translate the whole chakram via offset
            if (tick % 3 == 0) {
                double t = tick * 0.05;
                double offX = Math.sin(t) * 8.0;
                double offZ = Math.sin(t * 2.0) * 4.0;
                translateAll(offX, 0, offZ);
            }

            // Particles
            if (tick % 2 == 0) {
                double t = tick * 0.05;
                double offX = Math.sin(t) * 8.0;
                double offZ = Math.sin(t * 2.0) * 4.0;
                for (int i = 0; i < 4; i++) {
                    double a = spinAngle + (i * Math.PI / 2);
                    double px = Math.cos(a) * 3.0;
                    double pz = Math.sin(a) * 3.0;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(offX + px, 3.0, offZ + pz), 2, 0.1, 0.1, 0.1, 0.02);
                }
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(offX, 3.0, offZ), 3, 1.5, 0.3, 1.5, 0.05);
            }

            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 0.5f, 1.6f);
            }

            // Disintegrate at end
            if (tick > 205 && tick % 2 == 0) {
                float shrink = 1.0f - ((tick - 205) / 15.0f);
                if (shrink < 0) shrink = 0;
                for (BlockDisplayHandle h : mainRing) shrinkBlock(h.entity(), shrink);
            }
        }

        private void translateAll(double offX, double offY, double offZ) {
            // Note: we apply translation as Vector3f translation in the Transformation
            for (BlockDisplayHandle h : mainRing) translateBlock(h.entity(), (float) offX, (float) offY, (float) offZ);
            for (BlockDisplayHandle h : spokes) translateBlock(h.entity(), (float) offX, (float) offY, (float) offZ);
            for (BlockDisplayHandle h : bladeTeeth) translateBlock(h.entity(), (float) offX, (float) offY, (float) offZ);
            for (BlockDisplayHandle h : hub) translateBlock(h.entity(), (float) offX, (float) offY, (float) offZ);
        }

        private void translateBlock(BlockDisplay e, float x, float y, float z) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(3);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void shrinkBlock(BlockDisplay e, float scale) {
            Transformation t = e.getTransformation();
            Vector3f s = t.getScale();
            e.setInterpolationDuration(2);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    new Vector3f(s.x * scale, s.y * scale, s.z * scale),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
                Transformation t = e.getTransformation();
                e.setInterpolationDuration(2);
                e.setInterpolationDelay(0);
                e.setTransformation(new Transformation(
                        t.getTranslation(),
                        new AxisAngle4f(angle, ax, ay, az),
                        t.getScale(),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenChakramWheel(plugin); }
    }

    // ================================================================
    // #16 — ICE MACE DESCEND
    // 34 blocks: warhammer head 1 PACKED_ICE huge, 8 face-spikes AMETHYST,
    // haft 10 SMOOTH_QUARTZ, pommel 3 DIAMOND, binding rings 4 BLUE_ICE,
    // 8 hammer-cap CALCITE. Drops from Y+22 head-down, huge slam.
    // Impact-only 12.0r, 300 dmg.
    // ================================================================
    public static class IceMaceDescend extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> head = new ArrayList<>();
        private final List<BlockDisplayHandle> spikes = new ArrayList<>();
        private final List<BlockDisplayHandle> haft = new ArrayList<>();
        private final List<BlockDisplayHandle> pommel = new ArrayList<>();
        private final List<BlockDisplayHandle> rings = new ArrayList<>();
        private final List<BlockDisplayHandle> caps = new ArrayList<>();
        private boolean impacted = false;

        public IceMaceDescend(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mace_descend", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(300.0);
            config.setImpactRadius(12.0);
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(120);
            config.setCooldownTicks(150);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Head: 1 huge PACKED_ICE at top of weapon (Y+22)
            Location headLoc = center.clone().add(0, 22, 0);
            BlockDisplayHandle headBlock = displayBuilder.spawnBlock(headLoc, Material.PACKED_ICE);
            headBlock.scale(2.5f, 1.8f, 2.5f).glow(140, 200, 240).interpolation(3, 0);
            spawnedEntities.add(headBlock.entity());
            head.add(headBlock);

            // 8 face-spikes AMETHYST_BLOCK around head
            double[][] spikeOffsets = {
                    {1.4, 0, 0}, {-1.4, 0, 0}, {0, 0, 1.4}, {0, 0, -1.4},
                    {1.0, 0.7, 0}, {-1.0, 0.7, 0}, {0, 0.7, 1.0}, {0, 0.7, -1.0}
            };
            for (double[] off : spikeOffsets) {
                Location loc = center.clone().add(off[0], 22 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(180, 130, 220).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                spikes.add(h);
            }

            // Haft: 10 SMOOTH_QUARTZ stacked above head
            for (int s = 0; s < 10; s++) {
                Location loc = center.clone().add(0, 22 + 2.0 + s * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.35f, 0.6f, 0.35f).glow(230, 240, 250).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                haft.add(h);
            }

            // Pommel: 3 DIAMOND at top of haft
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0, 22 + 8.5 + s * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.5f, 0.4f, 0.5f).glow(190, 235, 255).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                pommel.add(h);
            }

            // Binding rings: 4 BLUE_ICE around haft
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 22 + 3.0 + s * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(1.0f, 0.2f, 1.0f).glow(80, 150, 230).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                rings.add(h);
            }

            // Hammer caps: 8 CALCITE at head corners
            double[][] capOffsets = {
                    {1.3, 0.9, 1.3}, {-1.3, 0.9, 1.3}, {1.3, 0.9, -1.3}, {-1.3, 0.9, -1.3},
                    {1.3, -0.9, 1.3}, {-1.3, -0.9, 1.3}, {1.3, -0.9, -1.3}, {-1.3, -0.9, -1.3}
            };
            for (double[] off : capOffsets) {
                Location loc = center.clone().add(off[0], 22 + off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.4f, 0.4f, 0.4f).glow(240, 240, 250).interpolation(3, 0);
                spawnedEntities.add(h.entity());
                caps.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.6f, 0.5f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 24, 0), 40, 2, 4, 2, 0.1);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop ticks 0-25: accelerating fall from Y+22 -> 0
            if (tick <= 25 && !impacted) {
                int step = tick / 4; // 0..6
                float[] dropTable = {0f, -4f, -8f, -13f, -18f, -21f, -22f};
                if (tick % 4 == 0 && step < dropTable.length) {
                    float dropY = dropTable[step];
                    dropAll(dropY);
                    if (tick > 0) {
                        c.getWorld().spawnParticle(Particle.CLOUD,
                                c.clone().add(0, 22 + dropY + 3, 0), 15, 1.5, 0.5, 1.5, 0.05);
                    }
                }
                if (tick == 25) {
                    impacted = true;
                    Location impact = c.clone();
                    triggerImpactDamage(impact);
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.3f);
                    c.getWorld().spawnParticle(Particle.FALLING_DUST,
                            impact, 120, 5, 1, 5, 0.1, Material.WHITE_CONCRETE.createBlockData());
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 100, 6, 2, 6, 0.2);
                }
            }

            // Shockwave expanding ring 4 blocks/sec ticks 25-60
            if (tick > 25 && tick < 60 && tick % 2 == 0) {
                double r = (tick - 25) * 0.4;
                for (int i = 0; i < 24; i++) {
                    double a = (2.0 * Math.PI * i) / 24.0;
                    double px = Math.cos(a) * r;
                    double pz = Math.sin(a) * r;
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(px, 0.3, pz), 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Break-at-neck phase: head + haft separate ticks 100+
            if (tick > 100 && tick % 4 == 0) {
                float fall = -((tick - 100) / 20.0f) * 1.0f;
                for (BlockDisplayHandle h : head) translateBlock(h.entity(), 0f, -22f + fall, 0f, 4);
                for (BlockDisplayHandle h : haft) translateBlock(h.entity(), 0.5f, -22f + fall * 2.0f, 0f, 4);
            }
        }

        private void dropAll(float dropY) {
            for (BlockDisplayHandle h : head) translateBlock(h.entity(), 0f, dropY, 0f, 4);
            for (BlockDisplayHandle h : spikes) translateBlock(h.entity(), 0f, dropY, 0f, 4);
            for (BlockDisplayHandle h : haft) translateBlock(h.entity(), 0f, dropY, 0f, 4);
            for (BlockDisplayHandle h : pommel) translateBlock(h.entity(), 0f, dropY, 0f, 4);
            for (BlockDisplayHandle h : rings) translateBlock(h.entity(), 0f, dropY, 0f, 4);
            for (BlockDisplayHandle h : caps) translateBlock(h.entity(), 0f, dropY, 0f, 4);
        }

        private void translateBlock(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new IceMaceDescend(plugin); }
    }

    // ================================================================
    // #17 — FROST SCYTHE REAP
    // 32 blocks: curved scythe blade 14 BLUE_ICE arc, guard 4 QUARTZ_BLOCK,
    // shaft 6 SMOOTH_QUARTZ, pommel ring 4 DIAMOND, 4 floating sparkles TINTED_GLASS.
    // Performs 3 sequential 270deg slashes around shared point.
    // Impact-only 7.5r, 140 dmg per slash (3 slashes total).
    // ================================================================
    public static class FrostScytheReap extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> blade = new ArrayList<>();
        private final List<BlockDisplayHandle> guard = new ArrayList<>();
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> pommelRing = new ArrayList<>();
        private final List<BlockDisplayHandle> sparkles = new ArrayList<>();
        private int slashCount = 0;

        public FrostScytheReap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_scythe_reap", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(140.0);
            config.setImpactRadius(7.5);
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(180);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Curved scythe blade: 14 BLUE_ICE in arc (length 10b)
            for (int s = 0; s < 14; s++) {
                double angle = (s / 13.0) * Math.PI * 0.6; // arc segment
                double px = Math.cos(angle) * 4.5 - 4.5;
                double pz = Math.sin(angle) * 4.5;
                Location loc = center.clone().add(px, 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.5f, 0.2f, 0.7f).glow(80, 150, 230).interpolation(4, 0);
                h.rotate((float) angle, 0f, 1f, 0f);
                spawnedEntities.add(h.entity());
                blade.add(h);
            }

            // Guard: 4 QUARTZ_BLOCK disc at blade base
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4.0;
                double px = Math.cos(a) * 0.5;
                double pz = Math.sin(a) * 0.5;
                Location loc = center.clone().add(px, 2.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.QUARTZ_BLOCK);
                h.scale(0.4f, 0.2f, 0.4f).glow(240, 240, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                guard.add(h);
            }

            // Shaft: 6 SMOOTH_QUARTZ vertical
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0, 2.5 - s * 0.4, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.25f, 0.4f, 0.25f).glow(230, 240, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                shaft.add(h);
            }

            // Pommel ring: 4 DIAMOND_BLOCK at shaft bottom
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4.0;
                double px = Math.cos(a) * 0.3;
                double pz = Math.sin(a) * 0.3;
                Location loc = center.clone().add(px, 0.3, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(190, 235, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                pommelRing.add(h);
            }

            // 4 floating sparkle TINTED_GLASS around blade
            for (int i = 0; i < 4; i++) {
                double a = (2.0 * Math.PI * i) / 4.0;
                double px = Math.cos(a) * 1.5;
                double pz = Math.sin(a) * 1.5;
                Location loc = center.clone().add(px, 3.5, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.15f, 0.15f, 0.15f).glow(180, 200, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                sparkles.add(h);
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 1.2f, 0.8f);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center.clone().add(0, 2.5, 0), 30, 2, 1, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // 3 slashes: tick 20 (slash 1), tick 70 (slash 2), tick 120 (slash 3)
            // Each: rotate blade 270 deg around shared point over 30 ticks, pause 10 ticks
            int[] slashStarts = {20, 70, 120};
            for (int i = 0; i < slashStarts.length; i++) {
                int start = slashStarts[i];
                if (tick >= start && tick <= start + 30 && tick % 2 == 0) {
                    float progress = (tick - start) / 30.0f;
                    float slashAngle = progress * (float) (Math.PI * 1.5); // 270 deg
                    // Each slash goes opposite direction alternately
                    float dir = (i % 2 == 0) ? 1.0f : -1.0f;
                    for (BlockDisplayHandle h : blade) {
                        BlockDisplay e = h.entity();
                        Transformation t = e.getTransformation();
                        e.setInterpolationDuration(2);
                        e.setInterpolationDelay(0);
                        e.setTransformation(new Transformation(
                                t.getTranslation(),
                                new AxisAngle4f(slashAngle * dir, 0f, 1f, 0f),
                                t.getScale(),
                                new AxisAngle4f(0, 0, 1, 0)
                        ));
                    }
                    // Particles along slash path
                    double a = slashAngle * dir;
                    for (int p = 0; p < 4; p++) {
                        double r = 1.0 + p * 1.0;
                        double px = Math.cos(a) * r;
                        double pz = Math.sin(a) * r;
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                                c.clone().add(px, 2.5, pz), 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                // Mid-slash impact
                if (tick == start + 15) {
                    slashCount++;
                    Player target = findNearestPlayer(c, 12);
                    Location impactLoc = (target != null) ? target.getLocation() : c.clone().add(2, 0, 2);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.4f, 1.2f - i * 0.15f);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc, 40, 2, 1, 2, 0.15);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, impactLoc, 30, 2, 1, 2, 0.2);
                }
            }

            // Final shatter at end
            if (tick == 165) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
                for (BlockDisplayHandle h : blade) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(10);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrostScytheReap(plugin); }
    }

    // ================================================================
    // #18 — HAILSTORM CROSSBOW
    // 39 blocks: stock body 4 SMOOTH_QUARTZ, 2 limb-arms 6 BLUE_ICE each (12),
    // bow-string 8 TINTED_GLASS, loaded bolt 6 PACKED_ICE, trigger 2 LIGHT_BLUE_CONCRETE,
    // sight 3 DIAMOND_BLOCK, 2 grip-handles 2 each (4).
    // Materializes mid-air, tracks player, draws string, fires bolt.
    // Impact-only 7.0r, 260 dmg.
    // ================================================================
    public static class HailstormCrossbow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stock = new ArrayList<>();
        private final List<BlockDisplayHandle> limbs = new ArrayList<>();
        private final List<BlockDisplayHandle> string = new ArrayList<>();
        private final List<BlockDisplayHandle> bolt = new ArrayList<>();
        private final List<BlockDisplayHandle> trigger = new ArrayList<>();
        private final List<BlockDisplayHandle> sight = new ArrayList<>();
        private final List<BlockDisplayHandle> grips = new ArrayList<>();
        private boolean fired = false;
        private float boltTravel = 0f;

        public HailstormCrossbow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hailstorm_crossbow", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(260.0);
            config.setImpactRadius(7.0);
            config.setDamage(0.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(200);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Stock body: 4 SMOOTH_QUARTZ
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 4, s * 0.8 - 1.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.8f, 0.6f, 0.9f).glow(230, 240, 250).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                stock.add(h);
            }

            // 2 limb-arms: 6 BLUE_ICE each
            for (int side = -1; side <= 1; side += 2) {
                for (int s = 0; s < 6; s++) {
                    double ax = side * (0.5 + s * 0.35);
                    Location loc = center.clone().add(ax, 4, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                    float taper = 0.6f - s * 0.07f;
                    h.scale(taper, 0.2f, 0.3f).glow(80, 150, 230).interpolation(6, 2);
                    spawnedEntities.add(h.entity());
                    limbs.add(h);
                }
            }

            // Bowstring: 8 TINTED_GLASS thin segments
            for (int s = 0; s < 8; s++) {
                double sx = (s - 3.5) * 0.4;
                Location loc = center.clone().add(sx, 4, -0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.TINTED_GLASS);
                h.scale(0.4f, 0.1f, 0.1f).glow(180, 200, 230).interpolation(6, 4);
                spawnedEntities.add(h.entity());
                string.add(h);
            }

            // Loaded bolt: 6 PACKED_ICE tapered
            for (int s = 0; s < 6; s++) {
                Location loc = center.clone().add(0, 4.1, s * 0.4 - 0.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                float taper = 0.3f - s * 0.03f;
                h.scale(taper, taper, 0.3f).glow(140, 200, 240).interpolation(6, 6);
                spawnedEntities.add(h.entity());
                bolt.add(h);
            }

            // Trigger: 2 LIGHT_BLUE_CONCRETE
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(0, 3.5 - s * 0.2, -0.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.LIGHT_BLUE_CONCRETE);
                h.scale(0.3f, 0.3f, 0.15f).glow(120, 200, 240).interpolation(6, 8);
                spawnedEntities.add(h.entity());
                trigger.add(h);
            }

            // Sight: 3 DIAMOND_BLOCK on top
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0, 4.5 + s * 0.2, s * 0.3 - 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.2f, 0.2f, 0.2f).glow(190, 235, 255).interpolation(6, 10);
                spawnedEntities.add(h.entity());
                sight.add(h);
            }

            // 2 grip-handles, 2 blocks each
            for (int side = -1; side <= 1; side += 2) {
                for (int s = 0; s < 2; s++) {
                    Location loc = center.clone().add(side * 0.4, 3.7 - s * 0.3, -0.9);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_CONCRETE);
                    h.scale(0.25f, 0.3f, 0.2f).glow(80, 180, 230).interpolation(6, 12);
                    spawnedEntities.add(h.entity());
                    grips.add(h);
                }
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.2f, 0.9f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 4, 0), 25, 1.5, 1, 1.5, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Track player with yaw — tick 30
            if (tick == 30) {
                Player target = findNearestPlayer(c, 30);
                if (target != null) {
                    Location pl = target.getLocation();
                    double dx = pl.getX() - c.getX();
                    double dz = pl.getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dx, dz);
                    rotateGroup(stock, yaw, 0f, 1f, 0f, 30);
                    rotateGroup(limbs, yaw, 0f, 1f, 0f, 30);
                    rotateGroup(bolt, yaw, 0f, 1f, 0f, 30);
                    rotateGroup(string, yaw, 0f, 1f, 0f, 30);
                }
            }

            // String draws back ticks 60-85 (translate -Z)
            if (tick >= 60 && tick <= 85 && tick % 3 == 0) {
                float draw = -0.8f * ((tick - 60) / 25.0f);
                for (BlockDisplayHandle h : string) applyTranslate(h.entity(), 0f, 0f, draw, 3);
                if (tick % 6 == 0) {
                    c.getWorld().spawnParticle(Particle.GLOW,
                            c.clone().add(0, 4, -0.3 + draw), 4, 0.3, 0.1, 0.3, 0.05);
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.4f, 1.6f);
                }
            }

            // Fire at tick 100
            if (tick == 100 && !fired) {
                fired = true;
                DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.6f, 1.3f);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                        c.clone().add(0, 4, 0), 40, 0.4, 0.4, 0.4, 0.2);
            }

            // Bolt travels ticks 100-120
            if (tick > 100 && tick <= 120) {
                boltTravel += 0.9f;
                if (tick % 2 == 0) {
                    for (BlockDisplayHandle h : bolt) applyTranslate(h.entity(), 0f, 0f, boltTravel, 2);
                    Location trail = c.clone().add(0, 4, boltTravel);
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE, trail, 5, 0.2, 0.2, 0.2, 0.05);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, trail, 3, 0.15, 0.15, 0.15, 0.05);
                }
            }

            // Impact at tick 120
            if (tick == 120) {
                Location impact = c.clone().add(0, 3.5, boltTravel);
                triggerImpactDamage(impact);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.8f, 0.6f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        impact, 60, 2.5, 1.5, 2.5, 0.1, Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 50, 3, 1.5, 3, 0.1);
            }

            // Limbs spring forward (loose) ticks 175+, blocks fall
            if (tick > 175 && tick % 4 == 0) {
                float fall = -((tick - 175) / 25.0f) * 2.0f;
                for (BlockDisplayHandle h : limbs) applyTranslate(h.entity(), 0f, fall, 0.5f, 4);
                for (BlockDisplayHandle h : stock) applyTranslate(h.entity(), 0f, fall, 0f, 4);
                for (BlockDisplayHandle h : grips) applyTranslate(h.entity(), 0f, fall, 0f, 4);
            }
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        private void rotateGroup(List<BlockDisplayHandle> group, float angle, float ax, float ay, float az, int dur) {
            for (BlockDisplayHandle h : group) {
                BlockDisplay e = h.entity();
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
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new HailstormCrossbow(plugin); }
    }

    // ================================================================
    // #19 — CRYO LANCE CHARGE
    // 30 blocks: lance shaft 12 SMOOTH_QUARTZ horizontal, lance tip 5 DIAMOND_BLOCK tapered,
    // guard collar 6 BLUE_ICE ring, mid-shaft binding 4 PACKED_ICE rings, pommel 3 CALCITE.
    // Materializes horizontal, then charges forward 12 blocks sweeping.
    // Impact-only 7.0r 180 dmg at charge end; constant 6.5r 80 dmg/10t during charge.
    // ================================================================
    public static class CryoLanceCharge extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shaft = new ArrayList<>();
        private final List<BlockDisplayHandle> tip = new ArrayList<>();
        private final List<BlockDisplayHandle> guard = new ArrayList<>();
        private final List<BlockDisplayHandle> bindings = new ArrayList<>();
        private final List<BlockDisplayHandle> pommel = new ArrayList<>();
        private boolean charged = false;
        private float chargeDist = 0f;

        public CryoLanceCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryo_lance_charge", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(180.0);
            config.setImpactRadius(7.0);
            config.setDamage(80.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(10);
            config.setDamageDelayTicks(25);
            config.setDurationTicks(160);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Lance shaft: 12 SMOOTH_QUARTZ horizontal along Z axis
            for (int s = 0; s < 12; s++) {
                Location loc = center.clone().add(0, 2.0, -3 + s * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.35f, 0.35f, 0.7f).glow(230, 240, 250).interpolation(6, 0);
                spawnedEntities.add(h.entity());
                shaft.add(h);
            }

            // Lance tip: 5 DIAMOND_BLOCK tapered, forward end
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(0, 2.0, 5.5 + s * 0.35);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                float taper = 0.4f - s * 0.06f;
                h.scale(taper, taper, 0.35f).glow(190, 235, 255).interpolation(6, 2);
                spawnedEntities.add(h.entity());
                tip.add(h);
            }

            // Guard collar: 6 BLUE_ICE ring near back end
            for (int i = 0; i < 6; i++) {
                double a = (2.0 * Math.PI * i) / 6.0;
                double px = Math.cos(a) * 0.7;
                double py = Math.sin(a) * 0.7 + 2.0;
                Location loc = center.clone().add(px, py, -2.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.3f, 0.3f, 0.25f).glow(80, 150, 230).interpolation(6, 4);
                spawnedEntities.add(h.entity());
                guard.add(h);
            }

            // Mid-shaft bindings: 4 PACKED_ICE rings (scaled torus-style)
            for (int s = 0; s < 4; s++) {
                Location loc = center.clone().add(0, 2.0, -1.5 + s * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(0.6f, 0.2f, 0.2f).glow(140, 200, 240).interpolation(6, 6);
                spawnedEntities.add(h.entity());
                bindings.add(h);
            }

            // Pommel: 3 CALCITE cap at far back
            for (int s = 0; s < 3; s++) {
                Location loc = center.clone().add(0, 2.0, -3.4 - s * 0.25);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.45f, 0.45f, 0.25f).glow(240, 240, 250).interpolation(6, 8);
                spawnedEntities.add(h.entity());
                pommel.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 1.0f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 2.0, 0), 30, 3, 0.5, 1, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Aim phase: rotate toward player at tick 25
            if (tick == 25) {
                Player target = findNearestPlayer(c, 30);
                if (target != null) {
                    Location pl = target.getLocation();
                    double dx = pl.getX() - c.getX();
                    double dz = pl.getZ() - c.getZ();
                    float yaw = (float) Math.atan2(dx, dz);
                    rotateAll(yaw, 25);
                }
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.2f);
            }

            // Charge: ticks 35-95, translate forward 12 blocks
            if (tick >= 35 && tick <= 95 && tick % 2 == 0) {
                chargeDist += 0.4f;
                translateAll(chargeDist);
                if (tick % 4 == 0) {
                    c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            c.clone().add(0, 2.0, chargeDist + 6), 6, 0.3, 0.3, 0.3, 0.05);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            c.clone().add(0, 2.0, chargeDist + 6), 4, 0.2, 0.2, 0.2, 0.05);
                }
                if (tick % 10 == 0) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 0.7f, 1.0f);
                }
            }

            // Impact at end of charge — tick 95
            if (tick == 95 && !charged) {
                charged = true;
                Location impact = c.clone().add(0, 2.0, chargeDist + 6);
                triggerImpactDamage(impact);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.5f);
                c.getWorld().spawnParticle(Particle.FALLING_DUST,
                        impact, 70, 3, 1.5, 3, 0.1, Material.WHITE_CONCRETE.createBlockData());
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impact, 60, 3.5, 2, 3.5, 0.15);
            }

            // Lance scatters at end
            if (tick > 135 && tick % 4 == 0) {
                float fall = -((tick - 135) / 25.0f) * 2.0f;
                for (BlockDisplayHandle h : shaft) applyTranslate(h.entity(), 0f, fall, chargeDist, 4);
                for (BlockDisplayHandle h : tip) applyTranslate(h.entity(), 0f, fall, chargeDist + 1, 4);
            }
        }

        private void translateAll(float z) {
            for (BlockDisplayHandle h : shaft) applyTranslate(h.entity(), 0f, 0f, z, 3);
            for (BlockDisplayHandle h : tip) applyTranslate(h.entity(), 0f, 0f, z, 3);
            for (BlockDisplayHandle h : guard) applyTranslate(h.entity(), 0f, 0f, z, 3);
            for (BlockDisplayHandle h : bindings) applyTranslate(h.entity(), 0f, 0f, z, 3);
            for (BlockDisplayHandle h : pommel) applyTranslate(h.entity(), 0f, 0f, z, 3);
        }

        private void rotateAll(float angle, int dur) {
            for (BlockDisplayHandle h : shaft) rotateBlock(h.entity(), angle, dur);
            for (BlockDisplayHandle h : tip) rotateBlock(h.entity(), angle, dur);
            for (BlockDisplayHandle h : guard) rotateBlock(h.entity(), angle, dur);
            for (BlockDisplayHandle h : bindings) rotateBlock(h.entity(), angle, dur);
            for (BlockDisplayHandle h : pommel) rotateBlock(h.entity(), angle, dur);
        }

        private void rotateBlock(BlockDisplay e, float angle, int dur) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(dur);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    t.getTranslation(),
                    new AxisAngle4f(angle, 0f, 1f, 0f),
                    t.getScale(),
                    new AxisAngle4f(0, 0, 1, 0)
            ));
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new CryoLanceCharge(plugin); }
    }

    // ================================================================
    // #20 — FROZEN FLAIL SWING
    // 30 blocks: handle 5 SMOOTH_QUARTZ, chain 8 ICE loose links, spiked ball
    // 1 PACKED_ICE core + 12 AMETHYST_BLOCK radial spikes, pommel 2 DIAMOND,
    // 2 chain rings BLUE_ICE.
    // Ball orbits center at R=6, chain follows.
    // Constant 8.0r, 100 dmg, 8t interval, 18t delay.
    // ================================================================
    public static class FrozenFlailSwing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> handle = new ArrayList<>();
        private final List<BlockDisplayHandle> chain = new ArrayList<>();
        private final List<BlockDisplayHandle> ball = new ArrayList<>();
        private final List<BlockDisplayHandle> ballCore = new ArrayList<>();
        private final List<BlockDisplayHandle> pommel = new ArrayList<>();
        private final List<BlockDisplayHandle> chainRings = new ArrayList<>();
        private float orbitAngle = 0f;

        public FrozenFlailSwing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_flail_swing", AttackType.BLOCK_DISPLAY, 1, MODE_PATH));
            config.setDamage(100.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(8);
            config.setDamageDelayTicks(18);
            config.setDurationTicks(220);
            config.setCooldownTicks(140);
            config.setEnabled(true);
        }

        @Override
        protected void onSpawn(Location center) {
            center.setYaw(0);
            center.setPitch(0);
            World w = center.getWorld();
            if (w == null) return;

            // Handle: 5 SMOOTH_QUARTZ stacked vertical (initially up at Y+10 dropping to Y+2)
            for (int s = 0; s < 5; s++) {
                Location loc = center.clone().add(0, 10 + s * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SMOOTH_QUARTZ);
                h.scale(0.4f, 0.5f, 0.4f).glow(230, 240, 250).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                handle.add(h);
            }

            // Pommel: 2 DIAMOND_BLOCK at top
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(0, 12.6 + s * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DIAMOND_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(190, 235, 255).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                pommel.add(h);
            }

            // Chain: 8 ICE loose links — initially trailing
            for (int s = 0; s < 8; s++) {
                Location loc = center.clone().add(0, 10 - (s + 1) * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(0.25f, 0.25f, 0.25f).glow(180, 220, 240).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                chain.add(h);
            }

            // 2 chain rings BLUE_ICE
            for (int s = 0; s < 2; s++) {
                Location loc = center.clone().add(0, 9.5 - s * 2.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(0.4f, 0.15f, 0.4f).glow(80, 150, 230).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                chainRings.add(h);
            }

            // Ball core: 1 PACKED_ICE big
            Location ballLoc = center.clone().add(0, 5, 0);
            BlockDisplayHandle ballBlock = displayBuilder.spawnBlock(ballLoc, Material.PACKED_ICE);
            ballBlock.scale(1.4f, 1.4f, 1.4f).glow(140, 200, 240).interpolation(4, 0);
            spawnedEntities.add(ballBlock.entity());
            ballCore.add(ballBlock);

            // 12 AMETHYST_BLOCK spikes radial around ball
            for (int i = 0; i < 12; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 12);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double px = Math.sin(phi) * Math.cos(theta) * 1.0;
                double py = Math.cos(phi) * 1.0;
                double pz = Math.sin(phi) * Math.sin(theta) * 1.0;
                Location loc = ballLoc.clone().add(px, py, pz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.35f, 0.9f, 0.35f).glow(180, 130, 220).interpolation(4, 0);
                spawnedEntities.add(h.entity());
                ball.add(h);
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ARMOR_EQUIP_IRON, 1.4f, 0.7f);
            w.spawnParticle(Particle.SNOWFLAKE, center.clone().add(0, 5, 0), 30, 2, 2, 2, 0.05);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drop phase: ticks 0-20, handle settles down
            if (tick <= 20 && tick % 4 == 0) {
                float dropY = -((tick / 20.0f)) * 8.0f;
                for (BlockDisplayHandle h : handle) applyTranslate(h.entity(), 0f, dropY, 0f, 4);
                for (BlockDisplayHandle h : pommel) applyTranslate(h.entity(), 0f, dropY, 0f, 4);
            }

            // Orbit phase: ball orbits center at R=6, chain follows
            if (tick > 20 && tick % 2 == 0) {
                orbitAngle += 0.18f; // fast swing
                double r = 6.0;
                double bx = Math.cos(orbitAngle) * r;
                double bz = Math.sin(orbitAngle) * r;
                for (BlockDisplayHandle h : ballCore) {
                    applyTranslate(h.entity(), (float) bx, -3f, (float) bz, 2);
                }
                // Spikes follow ball position (relative offset preserved by translation)
                for (BlockDisplayHandle h : ball) {
                    applyTranslate(h.entity(), (float) bx, -3f, (float) bz, 2);
                }
                // Chain segments interpolated between handle and ball
                for (int i = 0; i < chain.size(); i++) {
                    float t = (i + 1) / (float) (chain.size() + 1);
                    float cx = (float) bx * t;
                    float cz = (float) bz * t;
                    float cy = -3f * t + (1 - t) * 0f;
                    applyTranslate(chain.get(i).entity(), cx, cy, cz, 2);
                }
                for (int i = 0; i < chainRings.size(); i++) {
                    float t = 0.3f + i * 0.3f;
                    applyTranslate(chainRings.get(i).entity(),
                            (float) bx * t, -3f * t, (float) bz * t, 2);
                }
            }

            // Particles around ball
            if (tick > 20 && tick % 2 == 0) {
                double r = 6.0;
                double bx = Math.cos(orbitAngle) * r;
                double bz = Math.sin(orbitAngle) * r;
                Location ballPos = c.clone().add(bx, 2, bz);
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, ballPos, 5, 1, 0.5, 1, 0.05);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE,
                        c.clone().add(bx * 0.5, 3, bz * 0.5), 3, 0.2, 0.2, 0.2, 0.02);
            }

            // Sound each pass (every ~35t for full orbit)
            if (tick > 20 && tick % 18 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.0f, 0.9f);
            }

            // Over-spin shatter at end
            if (tick == 205) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.6f);
                for (BlockDisplayHandle h : ball) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(10);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
                for (BlockDisplayHandle h : chain) {
                    BlockDisplay e = h.entity();
                    Transformation t = e.getTransformation();
                    e.setInterpolationDuration(10);
                    e.setInterpolationDelay(0);
                    e.setTransformation(new Transformation(
                            t.getTranslation(),
                            new AxisAngle4f().set(t.getLeftRotation()),
                            new Vector3f(0f, 0f, 0f),
                            new AxisAngle4f().set(t.getRightRotation())
                    ));
                }
            }
        }

        private void applyTranslate(BlockDisplay e, float x, float y, float z, int duration) {
            Transformation t = e.getTransformation();
            e.setInterpolationDuration(duration);
            e.setInterpolationDelay(0);
            e.setTransformation(new Transformation(
                    new Vector3f(x - 0.5f, y - 0.5f, z - 0.5f),
                    new AxisAngle4f().set(t.getLeftRotation()),
                    t.getScale(),
                    new AxisAngle4f().set(t.getRightRotation())
            ));
        }

        @Override
        protected void onCleanup() { super.onCleanup(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenFlailSwing(plugin); }
    }
}
