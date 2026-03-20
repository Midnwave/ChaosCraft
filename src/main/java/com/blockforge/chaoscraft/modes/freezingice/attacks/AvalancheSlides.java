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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Freezing Ice — AVALANCHE SLIDES
 * 13 rolling/sliding momentum-based ice attacks.
 * All use ICE palette: BLUE_ICE, PACKED_ICE, ICE, SNOW_BLOCK, WHITE_CONCRETE.
 * Particles: SNOWFLAKE, END_ROD, DUST RGB(220,240,255).
 * NO potion effects. Damage radiuses 5-8 blocks.
 */
public final class AvalancheSlides {
    private AvalancheSlides() {}

    private static final Material[] ICE_MATS = {
        Material.BLUE_ICE, Material.PACKED_ICE, Material.ICE, Material.SNOW_BLOCK, Material.WHITE_CONCRETE
    };

    private static final Particle.DustOptions FROST_DUST =
        new Particle.DustOptions(Color.fromRGB(220, 240, 255), 1.2f);

    private static Material randomIce() {
        return ICE_MATS[ThreadLocalRandom.current().nextInt(ICE_MATS.length)];
    }

    /** Common frost particles at a location. */
    private static void frostParticles(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, count, spread, spread, spread, 0.02);
        DisplayBuilder.dustParticles(loc, count / 2, spread, 220, 240, 255, 1.2f);
    }

    /** Common frost trail behind a moving block. */
    private static void frostTrail(Location loc) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.3, 0.2, 0.3, 0.01);
        loc.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.1, 0.1, 0.1, 0.01);
    }

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SnowBoulder(plugin));
        registry.register(new IceAvalanche(plugin));
        registry.register(new GlacierSlide(plugin));
        registry.register(new FrostTsunami(plugin));
        registry.register(new IceBallBarrage(plugin));
        registry.register(new SnowSlide(plugin));
        registry.register(new FrostRoller(plugin));
        registry.register(new CrystalCascade(plugin));
        registry.register(new IceRam(plugin));
        registry.register(new FrostStampede(plugin));
        registry.register(new AvalancheFunnel(plugin));
        registry.register(new GlacialSurge(plugin));
        registry.register(new IceDebrisFlow(plugin));
    }

    // ================================================================
    // 1. SNOW BOULDER — 6 blocks forming sphere (scale 3.0), rolls toward player accelerating
    // ================================================================
    public static class SnowBoulder extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public SnowBoulder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_boulder", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            // Determine direction toward nearest player
            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            // Spawn 6 blocks forming a sphere cluster at scale 3.0
            // Start 15 blocks behind the target direction
            Location spawnBase = center.clone().add(-dirX * 15, 0.5, -dirZ * 15);
            double[][] offsets = {{0,0,0},{0.8,0,0},{-0.8,0,0},{0,0.8,0},{0,0,0.8},{0,0,-0.8}};
            Material[] mats = {Material.SNOW_BLOCK, Material.SNOW_BLOCK, Material.PACKED_ICE,
                Material.WHITE_CONCRETE, Material.PACKED_ICE, Material.SNOW_BLOCK};
            for (int i = 0; i < 6; i++) {
                Location loc = spawnBase.clone().add(offsets[i][0], offsets[i][1], offsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                h.scale(3.0f, 3.0f, 3.0f).glow(220, 240, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 extra surface detail blocks
            double[][] extra = {{1.2,0.5,0.5},{-1.2,0.5,-0.5},{0.5,1.2,0},{-0.5,-0.2,1.0}};
            for (double[] e : extra) {
                Location loc = spawnBase.clone().add(e[0], e[1], e[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.ICE);
                h.scale(2.0f, 2.0f, 2.0f).glow(200, 230, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
            frostParticles(spawnBase, 40, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Accelerating speed: 0.15 -> 0.6 over duration
            double speed = 0.15 + (tick / (double) config.getDurationTicks()) * 0.45;
            travelDist += speed;

            Location boulderCenter = origin.clone().add(
                dirX * (travelDist - 15), 0.5, dirZ * (travelDist - 15));

            // Roll rotation angle increases with distance
            float rollAngle = (float)(travelDist * 0.5);

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                Location base = bd.getLocation();
                // Offset each block relative to the rolling center
                double ox = bd.getLocation().getX() - base.getX();
                double oy = bd.getLocation().getY() - base.getY();
                double oz = bd.getLocation().getZ() - base.getZ();
                bd.teleport(boulderCenter.clone().add(
                    (i < 6 ? new double[][]{{0,0,0},{0.8,0,0},{-0.8,0,0},{0,0.8,0},{0,0,0.8},{0,0,-0.8}}
                            : new double[][]{{1.2,0.5,0.5},{-1.2,0.5,-0.5},{0.5,1.2,0},{-0.5,-0.2,1.0}})[i < 6 ? i : i - 6][0],
                    (i < 6 ? new double[][]{{0,0,0},{0.8,0,0},{-0.8,0,0},{0,0.8,0},{0,0,0.8},{0,0,-0.8}}
                            : new double[][]{{1.2,0.5,0.5},{-1.2,0.5,-0.5},{0.5,1.2,0},{-0.5,-0.2,1.0}})[i < 6 ? i : i - 6][1],
                    (i < 6 ? new double[][]{{0,0,0},{0.8,0,0},{-0.8,0,0},{0,0.8,0},{0,0,0.8},{0,0,-0.8}}
                            : new double[][]{{1.2,0.5,0.5},{-1.2,0.5,-0.5},{0.5,1.2,0},{-0.5,-0.2,1.0}})[i < 6 ? i : i - 6][2]
                ));
                // Apply rolling rotation
                all.get(i).rotate(rollAngle, (float)dirZ, 0, (float)(-dirX));
                all.get(i).interpolation(3, 0);
            }

            // Update center for damage radius
            setCenter(boulderCenter);

            // Particles every 2 ticks
            if (tick % 2 == 0) {
                frostTrail(boulderCenter);
                DisplayBuilder.particleRing(boulderCenter, 2.5, Particle.SNOWFLAKE, 12, null);
            }

            // Rumble sound every 20 ticks
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(boulderCenter, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SnowBoulder(plugin); }
    }

    // ================================================================
    // 2. ICE AVALANCHE — 18 tumbling blocks advancing as a wall-wave of debris
    // ================================================================
    public static class IceAvalanche extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] blockOffsetX = new double[18];
        private final double[] blockOffsetY = new double[18];
        private final double[] blockRotSpeed = new double[18];
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public IceAvalanche(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_avalanche", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            // perpendicular direction for wall spread
            double perpX = -dirZ;
            double perpZ = dirX;

            Location spawnBase = center.clone().add(-dirX * 18, 0, -dirZ * 18);

            for (int i = 0; i < 18; i++) {
                // Spread across a 12-block wide wall, 3 rows deep
                int row = i / 6;
                int col = i % 6;
                double spreadPerp = (col - 2.5) * 2.0;
                double spreadFwd = row * 1.5;
                blockOffsetX[i] = spreadPerp;
                blockOffsetY[i] = rand.nextDouble(0.2, 2.0);
                blockRotSpeed[i] = rand.nextFloat(0.05f, 0.2f);

                Location loc = spawnBase.clone().add(
                    perpX * spreadPerp + dirX * spreadFwd,
                    blockOffsetY[i],
                    perpZ * spreadPerp + dirZ * spreadFwd
                );
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, randomIce());
                float s = rand.nextFloat(1.2f, 2.5f);
                h.scale(s, s, s).glow(200, 230, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 0.9f, 0.3f);
            frostParticles(spawnBase, 60, 5.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.25 + (tick / (double) config.getDurationTicks()) * 0.3;
            travelDist += speed;

            double perpX = -dirZ;
            double perpZ = dirX;

            Location waveCenter = origin.clone().add(
                dirX * (travelDist - 18), 0, dirZ * (travelDist - 18));

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                int row = i / 6;
                int col = i % 6;
                double spreadPerp = (col - 2.5) * 2.0;
                double spreadFwd = row * 1.5;
                // Tumbling: Y oscillation
                double tumbleY = blockOffsetY[i] + Math.abs(Math.sin(tick * blockRotSpeed[i])) * 1.5;
                Location target = waveCenter.clone().add(
                    perpX * spreadPerp + dirX * spreadFwd,
                    tumbleY,
                    perpZ * spreadPerp + dirZ * spreadFwd
                );
                bd.teleport(target);
                all.get(i).rotate((float)(tick * blockRotSpeed[i]), 1, 0.5f, 0);
                all.get(i).interpolation(3, 0);
            }

            setCenter(waveCenter);

            if (tick % 3 == 0) {
                frostParticles(waveCenter, 15, 4.0);
                c.getWorld().spawnParticle(Particle.END_ROD, waveCenter.clone().add(0, 1, 0), 5, 3, 1, 3, 0.02);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(waveCenter, Sound.BLOCK_GLASS_BREAK, 0.7f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceAvalanche(plugin); }
    }

    // ================================================================
    // 3. GLACIER SLIDE — 16 flat blocks sliding across ground, push damage in front
    // ================================================================
    public static class GlacierSlide extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public GlacierSlide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_slide", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            Location spawnBase = center.clone().add(-dirX * 16, 0, -dirZ * 16);

            // 16 flat slabs in a 4x4 grid, flat on the ground
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) {
                    double perpOff = (col - 1.5) * 2.2;
                    double fwdOff = row * 1.8;
                    Location loc = spawnBase.clone().add(
                        perpX * perpOff + dirX * fwdOff,
                        0.1,
                        perpZ * perpOff + dirZ * fwdOff
                    );
                    Material mat = (row + col) % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(2.2f, 0.5f, 2.2f).glow(180, 220, 255).interpolation(2, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.3f);
            frostParticles(spawnBase, 40, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.3;
            travelDist += speed;

            double perpX = -dirZ;
            double perpZ = dirX;

            Location slideCenter = origin.clone().add(
                dirX * (travelDist - 16), 0, dirZ * (travelDist - 16));

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                int row = i / 4;
                int col = i % 4;
                double perpOff = (col - 1.5) * 2.2;
                double fwdOff = row * 1.8;
                Location target = slideCenter.clone().add(
                    perpX * perpOff + dirX * fwdOff,
                    0.1,
                    perpZ * perpOff + dirZ * fwdOff
                );
                bd.teleport(target);
            }

            // Damage center is at the leading edge
            Location leadEdge = slideCenter.clone().add(dirX * 4, 0, dirZ * 4);
            setCenter(leadEdge);

            if (tick % 3 == 0) {
                // Snow spray at leading edge
                frostParticles(leadEdge.clone().add(0, 0.5, 0), 10, 3.0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, leadEdge, 8, 3, 0.3, 3, 0.02);
            }
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(slideCenter, 6, 2.0, 220, 240, 255, 1.5f);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(slideCenter, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacierSlide(plugin); }
    }

    // ================================================================
    // 4. FROST TSUNAMI — 20 blocks forming tall curved wave, crests and crashes
    // ================================================================
    public static class FrostTsunami extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;
        private boolean crashed = false;

        public FrostTsunami(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_tsunami", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            Location spawnBase = center.clone().add(-dirX * 20, 0, -dirZ * 20);

            // 20 blocks: 5 columns across, 4 high forming a wave crest
            for (int col = 0; col < 5; col++) {
                double perpOff = (col - 2) * 2.5;
                for (int h = 0; h < 4; h++) {
                    // Wave curve: higher in the middle, curving forward at top
                    double curveForward = (h >= 3) ? 2.0 : 0;
                    double heightFactor = h * 2.0;
                    Location loc = spawnBase.clone().add(
                        perpX * perpOff + dirX * curveForward,
                        heightFactor,
                        perpZ * perpOff + dirZ * curveForward
                    );
                    Material mat;
                    if (h == 0) mat = Material.PACKED_ICE;
                    else if (h == 1) mat = Material.BLUE_ICE;
                    else if (h == 2) mat = Material.ICE;
                    else mat = Material.WHITE_CONCRETE;
                    BlockDisplayHandle bh = displayBuilder.spawnBlock(loc, mat);
                    bh.scale(2.5f, 2.0f, 1.5f).glow(200, 230, 255).interpolation(3, 0);
                    all.add(bh);
                    spawnedEntities.add(bh.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.2f, 0.4f);
            frostParticles(spawnBase, 60, 5.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double perpX = -dirZ;
            double perpZ = dirX;

            // Phase 1: Wave advances (ticks 0-180)
            if (tick < 180) {
                double speed = 0.25;
                travelDist += speed;

                Location waveBase = origin.clone().add(
                    dirX * (travelDist - 20), 0, dirZ * (travelDist - 20));

                for (int i = 0; i < all.size(); i++) {
                    BlockDisplay bd = all.get(i).entity();
                    if (!bd.isValid()) continue;
                    int col = i / 4;
                    int h = i % 4;
                    double perpOff = (col - 2) * 2.5;
                    double curveForward = (h >= 3) ? 2.0 : 0;
                    double heightFactor = h * 2.0;
                    // Wave sway
                    double sway = Math.sin(tick * 0.08 + col * 0.5) * 0.3;
                    Location target = waveBase.clone().add(
                        perpX * (perpOff + sway) + dirX * curveForward,
                        heightFactor,
                        perpZ * (perpOff + sway) + dirZ * curveForward
                    );
                    bd.teleport(target);
                }
                setCenter(waveBase.clone().add(0, 2, 0));
            }
            // Phase 2: Crash (ticks 180-220)
            else if (!crashed) {
                crashed = true;
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_SPLASH, 1.5f, 0.3f);
                frostParticles(getCenter(), 80, 6.0);
            }
            if (crashed && tick >= 180) {
                float crashProgress = Math.min(1.0f, (tick - 180) / 40.0f);
                for (int i = 0; i < all.size(); i++) {
                    BlockDisplay bd = all.get(i).entity();
                    if (!bd.isValid()) continue;
                    int h = i % 4;
                    // Blocks fall forward and flatten
                    Location cur = bd.getLocation();
                    double fallY = cur.getY() - (h * 0.15 * crashProgress);
                    bd.teleport(cur.clone().add(dirX * 0.3, -Math.max(0, cur.getY() - 0.2) * crashProgress * 0.1, dirZ * 0.3));
                    all.get(i).scale(2.5f + crashProgress * 1.5f, 2.0f * (1 - crashProgress * 0.7f), 1.5f + crashProgress * 2.0f);
                    all.get(i).interpolation(5, 0);
                }
            }

            if (tick % 3 == 0) {
                frostParticles(getCenter(), 12, 4.0);
                c.getWorld().spawnParticle(Particle.END_ROD, getCenter().clone().add(0, 3, 0), 4, 3, 1, 3, 0.02);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostTsunami(plugin); }
    }

    // ================================================================
    // 5. ICE BALL BARRAGE — 10 spheres rolling from random directions converging on player
    // ================================================================
    public static class IceBallBarrage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] ballDirX = new double[10];
        private final double[] ballDirZ = new double[10];
        private final Location[] ballOrigins = new Location[10];
        private Location target;

        public IceBallBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_ball_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.target = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            ThreadLocalRandom rand = ThreadLocalRandom.current();

            // 10 spheres from random directions 15 blocks out
            for (int i = 0; i < 10; i++) {
                double angle = (2.0 * Math.PI * i) / 10 + rand.nextDouble(-0.3, 0.3);
                double startX = center.getX() + Math.cos(angle) * 18;
                double startZ = center.getZ() + Math.sin(angle) * 18;
                ballOrigins[i] = new Location(w, startX, center.getY() + 0.5, startZ);

                double dx = center.getX() - startX;
                double dz = center.getZ() - startZ;
                double len = Math.sqrt(dx * dx + dz * dz);
                ballDirX[i] = dx / len;
                ballDirZ[i] = dz / len;

                BlockDisplayHandle h = displayBuilder.spawnBlock(ballOrigins[i], randomIce());
                h.scale(2.0f, 2.0f, 2.0f).glow(200, 230, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);
            frostParticles(center, 30, 2.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.2 + (tick / (double) config.getDurationTicks()) * 0.35;

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;

                Location cur = bd.getLocation();
                double newX = cur.getX() + ballDirX[i] * speed;
                double newZ = cur.getZ() + ballDirZ[i] * speed;
                Location next = new Location(c.getWorld(), newX, ballOrigins[i].getY(), newZ);
                bd.teleport(next);

                // Roll rotation
                float roll = (float)(tick * 0.3);
                all.get(i).rotate(roll, (float)ballDirZ[i], 0, (float)(-ballDirX[i]));
                all.get(i).interpolation(3, 0);

                if (tick % 4 == 0) {
                    frostTrail(next);
                }
            }

            // Damage center stays at convergence point
            setCenter(target);

            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(target, 5.0, Particle.SNOWFLAKE, 16, null);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(target, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.7f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceBallBarrage(plugin); }
    }

    // ================================================================
    // 6. SNOW SLIDE — 14 snow blocks sliding diagonally downward
    // ================================================================
    public static class SnowSlide extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public SnowSlide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_slide", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            // Spawn 14 blocks elevated and behind, arranged diagonally
            Location spawnBase = center.clone().add(-dirX * 15, 12, -dirZ * 15);

            for (int i = 0; i < 14; i++) {
                double perpOff = (i % 7 - 3) * 1.5;
                double fwdOff = (i / 7) * 2.0;
                double heightOff = rand.nextDouble(-0.5, 0.5);
                Location loc = spawnBase.clone().add(
                    perpX * perpOff + dirX * fwdOff,
                    heightOff,
                    perpZ * perpOff + dirZ * fwdOff
                );
                Material mat = i % 3 == 0 ? Material.SNOW_BLOCK : (i % 3 == 1 ? Material.WHITE_CONCRETE : Material.PACKED_ICE);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = rand.nextFloat(1.5f, 2.5f);
                h.scale(s, s, s).glow(220, 240, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SNOW_BREAK, 1.0f, 0.4f);
            frostParticles(spawnBase, 50, 4.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.3;
            travelDist += speed;

            double perpX = -dirZ;
            double perpZ = dirX;

            // Slide diagonally down: move forward and descend
            double descentRate = 0.06;
            double currentHeight = Math.max(0, 12 - travelDist * descentRate * 10);

            Location slideCenter = origin.clone().add(
                dirX * (travelDist - 15), currentHeight, dirZ * (travelDist - 15));

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                double perpOff = (i % 7 - 3) * 1.5;
                double fwdOff = (i / 7) * 2.0;
                // Wobble while sliding
                double wobble = Math.sin(tick * 0.15 + i * 0.7) * 0.4;
                Location target = slideCenter.clone().add(
                    perpX * (perpOff + wobble) + dirX * fwdOff,
                    wobble * 0.3,
                    perpZ * (perpOff + wobble) + dirZ * fwdOff
                );
                bd.teleport(target);
                all.get(i).rotate((float)(tick * 0.1 + i * 0.3), 0.5f, 1, 0.3f);
                all.get(i).interpolation(3, 0);
            }

            setCenter(slideCenter);

            if (tick % 3 == 0) {
                frostParticles(slideCenter, 10, 3.0);
                c.getWorld().spawnParticle(Particle.END_ROD, slideCenter, 3, 2, 0.5, 2, 0.01);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(slideCenter, Sound.BLOCK_SNOW_BREAK, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SnowSlide(plugin); }
    }

    // ================================================================
    // 7. FROST ROLLER — 12 blocks forming tall cylinder rolling forward (hard to jump)
    // ================================================================
    public static class FrostRoller extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public FrostRoller(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_roller", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            Location spawnBase = center.clone().add(-dirX * 16, 0, -dirZ * 16);

            // 12 blocks: 4 rings of 3, stacked vertically to form tall cylinder
            // perpendicular axis for cylinder width
            double perpX = -dirZ;
            double perpZ = dirX;

            for (int layer = 0; layer < 4; layer++) {
                for (int seg = 0; seg < 3; seg++) {
                    double angle = (2.0 * Math.PI * seg) / 3;
                    double ox = Math.cos(angle) * 1.5;
                    double oz = Math.sin(angle) * 1.5;
                    Location loc = spawnBase.clone().add(
                        perpX * ox, layer * 2.0, perpZ * oz + dirZ * oz * 0
                    );
                    Material mat = layer % 2 == 0 ? Material.BLUE_ICE : Material.PACKED_ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(2.0f, 2.0f, 2.0f).glow(180, 220, 255).interpolation(2, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.8f, 0.3f);
            frostParticles(spawnBase, 40, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.25;
            travelDist += speed;

            double perpX = -dirZ;
            double perpZ = dirX;

            Location rollerCenter = origin.clone().add(
                dirX * (travelDist - 16), 0, dirZ * (travelDist - 16));

            // Roll: rotate blocks around the perpendicular axis
            float rollAngle = (float)(travelDist * 0.4);

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                int layer = i / 3;
                int seg = i % 3;
                double baseAngle = (2.0 * Math.PI * seg) / 3 + rollAngle;
                double ox = Math.cos(baseAngle) * 1.5;
                double oy = Math.sin(baseAngle) * 1.5;
                Location target = rollerCenter.clone().add(
                    perpX * ox, layer * 2.0 + oy, perpZ * ox
                );
                bd.teleport(target);
                all.get(i).rotate(rollAngle, (float)perpX, 0, (float)perpZ);
                all.get(i).interpolation(3, 0);
            }

            setCenter(rollerCenter.clone().add(0, 3, 0));

            if (tick % 3 == 0) {
                // Frost spray at base
                frostParticles(rollerCenter, 8, 2.0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, rollerCenter.clone().add(0, 4, 0), 5, 1, 2, 1, 0.02);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(rollerCenter, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostRoller(plugin); }
    }

    // ================================================================
    // 8. CRYSTAL CASCADE — 16 blocks in continuous waterfall from height
    // ================================================================
    public static class CrystalCascade extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] fallProgress = new double[16];
        private final double[] fallSpeed = new double[16];
        private final double[] offsetX = new double[16];
        private final double[] offsetZ = new double[16];

        public CrystalCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cascade", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            ThreadLocalRandom rand = ThreadLocalRandom.current();

            // 16 blocks that continuously fall from 15 blocks up, reset when they hit ground
            for (int i = 0; i < 16; i++) {
                offsetX[i] = rand.nextDouble(-3, 3);
                offsetZ[i] = rand.nextDouble(-3, 3);
                fallProgress[i] = rand.nextDouble(0, 15); // Stagger start heights
                fallSpeed[i] = rand.nextDouble(0.3, 0.6);

                Location loc = center.clone().add(offsetX[i], 15 - fallProgress[i], offsetZ[i]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, randomIce());
                float s = rand.nextFloat(1.0f, 2.0f);
                h.scale(s, s, s).glow(200, 240, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
            frostParticles(center.clone().add(0, 15, 0), 40, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            ThreadLocalRandom rand = ThreadLocalRandom.current();

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;

                fallProgress[i] += fallSpeed[i];

                // Reset at ground level
                if (fallProgress[i] >= 15) {
                    fallProgress[i] = 0;
                    offsetX[i] = rand.nextDouble(-3, 3);
                    offsetZ[i] = rand.nextDouble(-3, 3);
                    fallSpeed[i] = rand.nextDouble(0.3, 0.6);

                    // Impact particles
                    Location impact = c.clone().add(offsetX[i], 0, offsetZ[i]);
                    frostParticles(impact, 8, 1.0);
                }

                double currentY = 15 - fallProgress[i];
                // Slight horizontal drift
                double driftX = Math.sin(tick * 0.05 + i) * 0.5;
                double driftZ = Math.cos(tick * 0.05 + i * 1.3) * 0.5;
                Location target = c.clone().add(offsetX[i] + driftX, currentY, offsetZ[i] + driftZ);
                bd.teleport(target);

                // Tumble rotation
                all.get(i).rotate((float)(fallProgress[i] * 0.3), 1, 0.3f, 0.5f);
                all.get(i).interpolation(2, 0);
            }

            if (tick % 4 == 0) {
                // Mist at the base
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 10, 3.0, 220, 240, 255, 1.5f);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 8, 0), 3, 2, 4, 2, 0.01);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrystalCascade(plugin); }
    }

    // ================================================================
    // 9. ICE RAM — 14 blocks forming battering ram, charges forward/retracts/charges 3x
    // ================================================================
    public static class IceRam extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private int chargeCount = 0;
        private double chargeProgress = 0;
        private boolean retracting = false;

        public IceRam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_ram", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            // Battering ram shape: elongated pointed front, wide base
            // 14 blocks: 4 at head (pointed), 6 shaft, 4 at back (wide)
            Location ramBase = center.clone().add(-dirX * 10, 1, -dirZ * 10);

            // Head: 4 blocks forming a pointed tip
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4;
                double ox = Math.cos(angle) * 0.4;
                double oy = Math.sin(angle) * 0.4;
                Location loc = ramBase.clone().add(dirX * 5 + perpX * ox, 0.5 + oy, dirZ * 5 + perpZ * ox);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                h.scale(1.5f, 1.5f, 1.5f).glow(150, 200, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // Shaft: 6 blocks
            for (int i = 0; i < 6; i++) {
                double perpOff = (i % 2 == 0) ? 0.6 : -0.6;
                double fwdOff = (i / 2) * 1.8;
                Location loc = ramBase.clone().add(
                    dirX * fwdOff + perpX * perpOff,
                    1,
                    dirZ * fwdOff + perpZ * perpOff
                );
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                h.scale(1.8f, 2.0f, 1.8f).glow(180, 220, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            // Back: 4 wide blocks
            for (int i = 0; i < 4; i++) {
                double perpOff = (i - 1.5) * 1.2;
                Location loc = ramBase.clone().add(
                    -dirX * 1 + perpX * perpOff,
                    1,
                    -dirZ * 1 + perpZ * perpOff
                );
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SNOW_BLOCK);
                h.scale(2.0f, 2.5f, 2.0f).glow(220, 240, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
            frostParticles(ramBase, 40, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double perpX = -dirZ;
            double perpZ = dirX;

            // 3 charge cycles: charge forward 12 blocks, retract 8 blocks
            int cycleLength = config.getDurationTicks() / 3;
            int cycleTick = tick % cycleLength;
            chargeCount = tick / cycleLength;

            double chargeForward;
            if (cycleTick < cycleLength * 0.6) {
                // Charging forward (60% of cycle)
                double t = cycleTick / (cycleLength * 0.6);
                chargeForward = t * 12.0;
                retracting = false;
            } else {
                // Retracting (40% of cycle)
                double t = (cycleTick - cycleLength * 0.6) / (cycleLength * 0.4);
                chargeForward = 12.0 - t * 8.0;
                if (!retracting) {
                    retracting = true;
                    // Impact effect at max charge
                    Location impact = origin.clone().add(dirX * (chargeCount * 4 + 2), 1, dirZ * (chargeCount * 4 + 2));
                    DisplayBuilder.playSound(impact, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.4f);
                    frostParticles(impact, 30, 3.0);
                }
            }

            double baseOffset = -10 + chargeForward + chargeCount * 4;
            Location ramBase = origin.clone().add(dirX * baseOffset, 0, dirZ * baseOffset);

            // Update head blocks
            for (int i = 0; i < 4; i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                double angle = (Math.PI * 2 * i) / 4;
                double ox = Math.cos(angle) * 0.4;
                double oy = Math.sin(angle) * 0.4;
                bd.teleport(ramBase.clone().add(dirX * 5 + perpX * ox, 0.5 + oy, dirZ * 5 + perpZ * ox));
            }
            // Update shaft blocks
            for (int i = 0; i < 6; i++) {
                BlockDisplay bd = all.get(4 + i).entity();
                if (!bd.isValid()) continue;
                double perpOff = (i % 2 == 0) ? 0.6 : -0.6;
                double fwdOff = (i / 2) * 1.8;
                bd.teleport(ramBase.clone().add(
                    dirX * fwdOff + perpX * perpOff, 1, dirZ * fwdOff + perpZ * perpOff));
            }
            // Update back blocks
            for (int i = 0; i < 4; i++) {
                BlockDisplay bd = all.get(10 + i).entity();
                if (!bd.isValid()) continue;
                double perpOff = (i - 1.5) * 1.2;
                bd.teleport(ramBase.clone().add(
                    -dirX * 1 + perpX * perpOff, 1, -dirZ * 1 + perpZ * perpOff));
            }

            // Damage center at the ram head
            setCenter(ramBase.clone().add(dirX * 5, 1, dirZ * 5));

            if (tick % 3 == 0) {
                Location headLoc = ramBase.clone().add(dirX * 5, 1, dirZ * 5);
                frostParticles(headLoc, 8, 1.5);
                c.getWorld().spawnParticle(Particle.END_ROD, headLoc, 2, 0.5, 0.5, 0.5, 0.02);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceRam(plugin); }
    }

    // ================================================================
    // 10. FROST STAMPEDE — 12 boulders (3 active at once) rolling same path staggered
    // ================================================================
    public static class FrostStampede extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] boulderProgress = new double[12];
        private final boolean[] boulderActive = new boolean[12];
        private Location origin;
        private double dirX, dirZ;

        public FrostStampede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_stampede", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            // Spawn all 12 boulders off-screen behind, initially inactive
            Location spawnHide = center.clone().add(-dirX * 25, -5, -dirZ * 25);
            for (int i = 0; i < 12; i++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(spawnHide.clone(), randomIce());
                float s = ThreadLocalRandom.current().nextFloat(2.0f, 3.0f);
                h.scale(s, s, s).glow(200, 230, 255).interpolation(2, 0);
                all.add(h);
                spawnedEntities.add(h.entity());
                boulderProgress[i] = -1;
                boulderActive[i] = false;
            }

            // First 3 active immediately
            for (int i = 0; i < 3; i++) {
                boulderActive[i] = true;
                boulderProgress[i] = 0;
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.3f);
            frostParticles(center, 40, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double perpX = -dirZ;
            double perpZ = dirX;

            // Activate new boulders in waves of 3
            int waveInterval = 50; // New wave every 50 ticks
            int wave = tick / waveInterval;
            int firstInWave = wave * 3;
            for (int i = firstInWave; i < Math.min(firstInWave + 3, 12); i++) {
                if (!boulderActive[i]) {
                    boulderActive[i] = true;
                    boulderProgress[i] = 0;
                    DisplayBuilder.playSound(origin, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
                }
            }

            Location activeDamageCenter = null;

            for (int i = 0; i < 12; i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;

                if (!boulderActive[i]) continue;

                boulderProgress[i] += 0.35;
                double progress = boulderProgress[i];

                // Each boulder slightly offset perpendicular for variety
                double perpOff = ((i % 3) - 1) * 2.5;
                Location pos = origin.clone().add(
                    dirX * (progress - 20) + perpX * perpOff,
                    0.5,
                    dirZ * (progress - 20) + perpZ * perpOff
                );
                bd.teleport(pos);

                float rollAngle = (float)(progress * 0.4);
                all.get(i).rotate(rollAngle, (float)dirZ, 0, (float)(-dirX));
                all.get(i).interpolation(3, 0);

                if (tick % 4 == 0 && progress > 0 && progress < 40) {
                    frostTrail(pos);
                }

                // Track leading active boulder for damage center
                if (activeDamageCenter == null || progress > 0) {
                    activeDamageCenter = pos;
                }
            }

            if (activeDamageCenter != null) {
                setCenter(activeDamageCenter);
            }

            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(getCenter(), 3.0, Particle.SNOWFLAKE, 12, null);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_RAVAGER_STEP, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new FrostStampede(plugin); }
    }

    // ================================================================
    // 11. AVALANCHE FUNNEL — 16 blocks (8 per side) V-shape closing in on player
    // ================================================================
    public static class AvalancheFunnel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> leftSide = new ArrayList<>();
        private final List<BlockDisplayHandle> rightSide = new ArrayList<>();
        private Location targetLoc;
        private double leftDirX, leftDirZ, rightDirX, rightDirZ;
        private double fwdDirX, fwdDirZ;

        public AvalancheFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_funnel", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.targetLoc = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { fwdDirX = dx / len; fwdDirZ = dz / len; }
                else { fwdDirX = 1; fwdDirZ = 0; }
            } else { fwdDirX = 1; fwdDirZ = 0; }

            double perpX = -fwdDirZ;
            double perpZ = fwdDirX;

            // V-shape: two arms angled 45 degrees inward, each starting 15 blocks out to the side
            // Left arm direction: forward + inward from left
            double angle45 = Math.PI / 4;
            leftDirX = fwdDirX * Math.cos(angle45) - perpX * Math.sin(angle45);
            leftDirZ = fwdDirZ * Math.cos(angle45) - perpZ * Math.sin(angle45);
            rightDirX = fwdDirX * Math.cos(-angle45) - perpX * Math.sin(-angle45);
            rightDirZ = fwdDirZ * Math.cos(-angle45) - perpZ * Math.sin(-angle45);

            // Left arm: 8 blocks starting far left
            Location leftStart = center.clone().add(-perpX * 12 - fwdDirX * 12, 0.5, -perpZ * 12 - fwdDirZ * 12);
            for (int i = 0; i < 8; i++) {
                Location loc = leftStart.clone().add(
                    perpX * (i * 0.8), ThreadLocalRandom.current().nextDouble(0, 1.5), perpZ * (i * 0.8));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, randomIce());
                h.scale(2.0f, 2.0f, 2.0f).glow(200, 230, 255).interpolation(2, 0);
                leftSide.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right arm: 8 blocks starting far right
            Location rightStart = center.clone().add(perpX * 12 - fwdDirX * 12, 0.5, perpZ * 12 - fwdDirZ * 12);
            for (int i = 0; i < 8; i++) {
                Location loc = rightStart.clone().add(
                    -perpX * (i * 0.8), ThreadLocalRandom.current().nextDouble(0, 1.5), -perpZ * (i * 0.8));
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, randomIce());
                h.scale(2.0f, 2.0f, 2.0f).glow(200, 230, 255).interpolation(2, 0);
                rightSide.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.4f);
            frostParticles(leftStart, 30, 3.0);
            frostParticles(rightStart, 30, 3.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.3;
            double perpX = -fwdDirZ;
            double perpZ = fwdDirX;

            // Both arms converge on target
            for (int i = 0; i < leftSide.size(); i++) {
                BlockDisplay bd = leftSide.get(i).entity();
                if (!bd.isValid()) continue;
                Location cur = bd.getLocation();
                // Move toward target center
                double dx = targetLoc.getX() - cur.getX();
                double dz = targetLoc.getZ() - cur.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1) {
                    bd.teleport(cur.clone().add(dx / len * speed, 0, dz / len * speed));
                }
                leftSide.get(i).rotate((float)(tick * 0.1), 0.5f, 1, 0);
                leftSide.get(i).interpolation(3, 0);
            }
            for (int i = 0; i < rightSide.size(); i++) {
                BlockDisplay bd = rightSide.get(i).entity();
                if (!bd.isValid()) continue;
                Location cur = bd.getLocation();
                double dx = targetLoc.getX() - cur.getX();
                double dz = targetLoc.getZ() - cur.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 1) {
                    bd.teleport(cur.clone().add(dx / len * speed, 0, dz / len * speed));
                }
                rightSide.get(i).rotate((float)(tick * 0.1), -0.5f, 1, 0);
                rightSide.get(i).interpolation(3, 0);
            }

            setCenter(targetLoc);

            if (tick % 3 == 0) {
                frostParticles(targetLoc, 10, 3.0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc, 8, 4, 1, 4, 0.02);
            }
            if (tick % 5 == 0) {
                DisplayBuilder.particleRing(targetLoc, 6.0, Particle.END_ROD, 12, null);
            }
            if (tick % 25 == 0) {
                DisplayBuilder.playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AvalancheFunnel(plugin); }
    }

    // ================================================================
    // 12. GLACIAL SURGE — 20 blocks rising tide advancing at ground level
    // ================================================================
    public static class GlacialSurge extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public GlacialSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_surge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            Location spawnBase = center.clone().add(-dirX * 18, 0, -dirZ * 18);

            // 20 blocks: 5 rows of 4 across, low to the ground like a rising flood
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 4; col++) {
                    double perpOff = (col - 1.5) * 2.5;
                    double fwdOff = row * 2.0;
                    // Front rows slightly higher (rising tide effect)
                    double riseH = (4 - row) * 0.3;
                    Location loc = spawnBase.clone().add(
                        perpX * perpOff + dirX * fwdOff,
                        riseH,
                        perpZ * perpOff + dirZ * fwdOff
                    );
                    Material mat;
                    if (row < 2) mat = Material.BLUE_ICE;
                    else if (row < 4) mat = Material.PACKED_ICE;
                    else mat = Material.ICE;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(2.5f, 1.0f + (float)riseH, 2.5f).glow(180, 220, 255).interpolation(3, 0);
                    all.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_SPLASH, 1.0f, 0.3f);
            frostParticles(spawnBase, 50, 5.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.2;
            travelDist += speed;

            // Tide rises over time
            double globalRise = Math.min(2.0, travelDist * 0.04);

            double perpX = -dirZ;
            double perpZ = dirX;

            Location surgeCenter = origin.clone().add(
                dirX * (travelDist - 18), 0, dirZ * (travelDist - 18));

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;
                int row = i / 4;
                int col = i % 4;
                double perpOff = (col - 1.5) * 2.5;
                double fwdOff = row * 2.0;
                double riseH = (4 - row) * 0.3 + globalRise;
                // Undulating motion
                double wave = Math.sin(tick * 0.1 + col * 0.8 + row * 0.5) * 0.4;
                Location target = surgeCenter.clone().add(
                    perpX * perpOff + dirX * fwdOff,
                    riseH + wave,
                    perpZ * perpOff + dirZ * fwdOff
                );
                bd.teleport(target);
                // Scale grows as tide rises
                float scaleY = 1.0f + (float)riseH;
                all.get(i).scale(2.5f, scaleY, 2.5f);
                all.get(i).interpolation(4, 0);
            }

            setCenter(surgeCenter.clone().add(0, globalRise, 0));

            if (tick % 3 == 0) {
                frostParticles(surgeCenter.clone().add(0, globalRise + 0.5, 0), 12, 4.0);
                c.getWorld().spawnParticle(Particle.END_ROD, surgeCenter.clone().add(0, globalRise + 1, 0), 4, 3, 0.3, 3, 0.01);
            }
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(surgeCenter, 8, 3.0, 220, 240, 255, 1.5f);
            }
            if (tick % 30 == 0) {
                DisplayBuilder.playSound(surgeCenter, Sound.ENTITY_GENERIC_SPLASH, 0.6f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new GlacialSurge(plugin); }
    }

    // ================================================================
    // 13. ICE DEBRIS FLOW — 16 mixed blocks tumbling chaotically with random rotations
    // ================================================================
    public static class IceDebrisFlow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> all = new ArrayList<>();
        private final double[] rotSpeedX = new double[16];
        private final double[] rotSpeedY = new double[16];
        private final double[] rotSpeedZ = new double[16];
        private final double[] perpOffset = new double[16];
        private final double[] heightOffset = new double[16];
        private final double[] fwdOffset = new double[16];
        private Location origin;
        private double dirX, dirZ;
        private double travelDist = 0;

        public IceDebrisFlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_debris_flow", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            this.origin = center.clone();
            World w = center.getWorld();
            if (w == null) return;

            Player nearest = null;
            double nearDist = Double.MAX_VALUE;
            for (Player p : w.getPlayers()) {
                double d = p.getLocation().distanceSquared(center);
                if (d < nearDist) { nearDist = d; nearest = p; }
            }
            if (nearest != null) {
                Location pl = nearest.getLocation();
                double dx = pl.getX() - center.getX();
                double dz = pl.getZ() - center.getZ();
                double len = Math.sqrt(dx * dx + dz * dz);
                if (len > 0.1) { dirX = dx / len; dirZ = dz / len; }
                else { dirX = 1; dirZ = 0; }
            } else { dirX = 1; dirZ = 0; }

            double perpX = -dirZ;
            double perpZ = dirX;

            ThreadLocalRandom rand = ThreadLocalRandom.current();
            Location spawnBase = center.clone().add(-dirX * 16, 0, -dirZ * 16);

            for (int i = 0; i < 16; i++) {
                // Random chaotic offsets
                perpOffset[i] = rand.nextDouble(-4, 4);
                heightOffset[i] = rand.nextDouble(0.2, 3.0);
                fwdOffset[i] = rand.nextDouble(-2, 4);
                rotSpeedX[i] = rand.nextDouble(0.05, 0.3);
                rotSpeedY[i] = rand.nextDouble(0.05, 0.3);
                rotSpeedZ[i] = rand.nextDouble(0.05, 0.3);

                Location loc = spawnBase.clone().add(
                    perpX * perpOffset[i] + dirX * fwdOffset[i],
                    heightOffset[i],
                    perpZ * perpOffset[i] + dirZ * fwdOffset[i]
                );
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, randomIce());
                float s = rand.nextFloat(1.0f, 2.5f);
                h.scale(s, s, s).glow(200, 230, 255).interpolation(2, 0);
                // Random initial rotation
                h.rotate(rand.nextFloat(0, (float)(Math.PI * 2)),
                    rand.nextFloat(-1, 1), rand.nextFloat(-1, 1), rand.nextFloat(-1, 1));
                all.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.3f);
            frostParticles(spawnBase, 60, 5.0);
        }

        @Override
        protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double speed = 0.25 + (tick / (double) config.getDurationTicks()) * 0.25;
            travelDist += speed;

            double perpX = -dirZ;
            double perpZ = dirX;

            Location flowCenter = origin.clone().add(
                dirX * (travelDist - 16), 0, dirZ * (travelDist - 16));

            for (int i = 0; i < all.size(); i++) {
                BlockDisplay bd = all.get(i).entity();
                if (!bd.isValid()) continue;

                // Chaotic: each block bounces and tumbles independently
                double bounceY = Math.abs(Math.sin(tick * 0.12 + i * 1.7)) * 2.0;
                double wobblePerp = Math.sin(tick * 0.08 + i * 2.3) * 1.5;
                double wobbleFwd = Math.cos(tick * 0.06 + i * 1.1) * 0.8;

                Location target = flowCenter.clone().add(
                    perpX * (perpOffset[i] + wobblePerp) + dirX * (fwdOffset[i] + wobbleFwd),
                    heightOffset[i] + bounceY,
                    perpZ * (perpOffset[i] + wobblePerp) + dirZ * (fwdOffset[i] + wobbleFwd)
                );
                bd.teleport(target);

                // Chaotic random rotation per block
                float rx = (float)(tick * rotSpeedX[i]);
                float ry = (float)(tick * rotSpeedY[i]);
                float rz = (float)(tick * rotSpeedZ[i]);
                all.get(i).rotate(rx + ry + rz, (float)rotSpeedX[i], (float)rotSpeedY[i], (float)rotSpeedZ[i]);
                all.get(i).interpolation(3, 0);
            }

            setCenter(flowCenter.clone().add(0, 1, 0));

            if (tick % 2 == 0) {
                frostParticles(flowCenter, 10, 4.0);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, flowCenter.clone().add(0, 2, 0), 6, 3, 2, 3, 0.03);
            }
            if (tick % 5 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, flowCenter.clone().add(0, 1, 0), 3, 2, 1, 2, 0.02);
            }
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(flowCenter, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new IceDebrisFlow(plugin); }
    }
}
