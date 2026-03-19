package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — DREAM SHIFT ENVIRONMENTAL ATTACKS
 * 13 reality-warping environmental effects: gravity changes, time stutters,
 * spatial distortions. These manipulate the world around the player.
 */
public class DreamShifts {

    private DreamShifts() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GravityPulse(plugin));
        registry.register(new TimeStutter(plugin));
        registry.register(new SpatialCompression(plugin));
        registry.register(new DirectionScramble(plugin));
        registry.register(new DepthDistortion(plugin));
        registry.register(new EchoStep(plugin));
        registry.register(new MirrorWorld(plugin));
        registry.register(new DreamRewind(plugin));
        registry.register(new SizeShift(plugin));
        registry.register(new ParallelSelf(plugin));
        registry.register(new PhaseFlicker(plugin));
        registry.register(new MemoryBleed(plugin));
        registry.register(new LoopBreak(plugin));
    }

    // ================================================================
    // 1. GRAVITY PULSE — Periodic gravity reversals. Players get
    //    launched upward, then slammed back down. Reverse portal
    //    particles mark the affected area.
    // ================================================================
    public static class GravityPulse extends EnvironmentalAttack {
        private boolean pulseUp = false;
        private int pulseCooldown = 0;

        public GravityPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_pulse", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_SHULKER_TELEPORT, 1.0f, 0.3f);
            // Warning ring
            DisplayBuilder.particleRing(center, 8.0, Particle.REVERSE_PORTAL, 24, null);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            pulseCooldown--;

            // Pulse every 40 ticks
            if (pulseCooldown <= 0) {
                pulseCooldown = 40;
                pulseUp = !pulseUp;

                DisplayBuilder.playSound(c, Sound.ENTITY_SHULKER_BULLET_HIT, 0.8f, pulseUp ? 1.5f : 0.3f);

                // Launch/slam players in radius
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 64) { // 8 block radius
                        Vector vel = p.getVelocity();
                        if (pulseUp) {
                            p.setVelocity(vel.add(new Vector(0, 1.2, 0)));
                        } else {
                            p.setVelocity(vel.add(new Vector(0, -0.8, 0)));
                        }
                    }
                }

                // Visual pulse
                for (double r = 1; r <= 8; r += 2) {
                    DisplayBuilder.particleRing(c.clone().add(0, pulseUp ? 0.5 : 3, 0),
                            r, Particle.REVERSE_PORTAL, (int)(r * 4), null);
                }
            }

            // Ambient particles
            if (ticksAlive % 4 == 0) {
                double angle = Math.random() * Math.PI * 2;
                double dist = Math.random() * 8;
                Location pLoc = c.clone().add(Math.cos(angle) * dist, pulseUp ? 0.2 : 3, Math.sin(angle) * dist);
                Particle p = pulseUp ? Particle.REVERSE_PORTAL : Particle.PORTAL;
                w.spawnParticle(p, pLoc, 5, 0.5, 0.5, 0.5, 0.1);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GravityPulse(plugin); }
    }

    // ================================================================
    // 2. TIME STUTTER — Periodic brief freezes: players get slowed
    //    then released. Screen flashes via particles. Clock ticking.
    // ================================================================
    public static class TimeStutter extends EnvironmentalAttack {
        public TimeStutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("time_stutter", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Stutter every 30 ticks — freeze players briefly
            boolean stuttering = (ticksAlive % 30) < 5;

            if (stuttering) {
                // Slow everyone in radius
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 100) {
                        p.setVelocity(p.getVelocity().multiply(0.1));
                    }
                }

                // Flash particles
                if (ticksAlive % 2 == 0) {
                    w.spawnParticle(Particle.END_ROD, c, 20, 5, 3, 5, 0.01);
                    DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 10, 5.0, 200, 200, 220, 2.0f);
                }
            }

            // Erratic ticking
            if (ticksAlive % (5 + (int)(Math.random() * 15)) == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f,
                        0.3f + (float)(Math.random() * 1.2));
            }

            // Time particles
            if (ticksAlive % 6 == 0) {
                double angle = Math.random() * Math.PI * 2;
                w.spawnParticle(Particle.ENCHANT,
                        c.clone().add(Math.cos(angle) * 5, 2, Math.sin(angle) * 5),
                        5, 0.5, 0.5, 0.5, 0.5);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TimeStutter(plugin); }
    }

    // ================================================================
    // 3. SPATIAL COMPRESSION — The area around the player shrinks.
    //    Ring of particles closes inward, dealing damage as it
    //    constricts. Players pushed toward center.
    // ================================================================
    public static class SpatialCompression extends EnvironmentalAttack {
        private double compressionRadius = 12.0;

        public SpatialCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spatial_compression", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.0f, 0.3f);
            DisplayBuilder.particleRing(center, compressionRadius, Particle.DUST, 36,
                    new Particle.DustOptions(Color.fromRGB(150, 80, 200), 1.5f));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (compressionRadius > 3.0) compressionRadius -= 0.03;

            // Compression ring
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), compressionRadius,
                        Particle.DUST, (int)(compressionRadius * 4),
                        new Particle.DustOptions(Color.fromRGB(150, 80, 200), 1.5f));
                DisplayBuilder.particleRing(c.clone().add(0, 2.0, 0), compressionRadius,
                        Particle.DUST, (int)(compressionRadius * 3),
                        new Particle.DustOptions(Color.fromRGB(100, 50, 160), 1.0f));
            }

            // Push players inward when they're near the edge
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                double dist = p.getLocation().distance(c);
                if (dist > compressionRadius - 1 && dist < compressionRadius + 1) {
                    Vector push = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.3);
                    p.setVelocity(p.getVelocity().add(push));
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 0.5f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SpatialCompression(plugin); }
    }

    // ================================================================
    // 4. DIRECTION SCRAMBLE — Random velocity impulses push players
    //    in unexpected directions. Disorienting particle effects.
    // ================================================================
    public static class DirectionScramble extends EnvironmentalAttack {
        public DirectionScramble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("direction_scramble", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(250);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Random push every 20 ticks
            if (ticksAlive % 20 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Vector pushDir = new Vector(Math.cos(angle) * 0.6, 0.2, Math.sin(angle) * 0.6);

                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 64) {
                        p.setVelocity(p.getVelocity().add(pushDir));
                    }
                }

                // Directional arrow particles
                for (int i = 0; i < 8; i++) {
                    double d = i * 1.0;
                    w.spawnParticle(Particle.REVERSE_PORTAL,
                            c.clone().add(Math.cos(angle) * d, 1.5, Math.sin(angle) * d),
                            3, 0.3, 0.3, 0.3, 0.05);
                }

                DisplayBuilder.playSound(c, Sound.ENTITY_SHULKER_BULLET_HIT, 0.5f,
                        0.5f + (float)(Math.random() * 0.5));
            }

            // Swirling confusion particles
            if (ticksAlive % 4 == 0) {
                double angle = ticksAlive * 0.2;
                w.spawnParticle(Particle.PORTAL,
                        c.clone().add(Math.cos(angle) * 3, 1.5, Math.sin(angle) * 3),
                        3, 0.3, 0.5, 0.3, 0.3);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DirectionScramble(plugin); }
    }

    // ================================================================
    // 5. DEPTH DISTORTION — Ground appears to warp. Blocks of dark
    //    concrete rise and fall at ground level. Dizzying.
    // ================================================================
    public static class DepthDistortion extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> warpBlocks = new ArrayList<>();

        public DepthDistortion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("depth_distortion", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1 + Math.random() * 7;
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist),
                        Material.BLACK_CONCRETE);
                b.scale(1.5f, 0.3f, 1.5f).glow(20, 10, 30).interpolation(5, 0);
                warpBlocks.add(b);
                spawnedEntities.add(b.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < warpBlocks.size(); i++) {
                float warpY = (float) Math.sin(ticksAlive * 0.05 + i * 0.8) * 1.5f;
                Location bLoc = warpBlocks.get(i).entity().getLocation();
                warpBlocks.get(i).entity().teleport(new Location(
                        c.getWorld(), bLoc.getX(), c.getY() + warpY, bLoc.getZ()));
                warpBlocks.get(i).scale(1.5f, 0.3f + Math.abs(warpY) * 0.2f, 1.5f);
            }

            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.SMOKE,
                        c.clone().add(Math.random() * 14 - 7, 0.5, Math.random() * 14 - 7),
                        3, 0.3, 0.2, 0.3, 0.01);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 0.4f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DepthDistortion(plugin); }
    }

    // ================================================================
    // 6. ECHO STEP — Spectral footstep particles follow the player
    //    with a delay, dealing damage when they catch up.
    // ================================================================
    public static class EchoStep extends EnvironmentalAttack {
        private final List<Location> echoPositions = new ArrayList<>();

        public EchoStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_step", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Record current position
            if (ticksAlive % 5 == 0) {
                echoPositions.add(c.clone());
                if (echoPositions.size() > 20) echoPositions.remove(0);
            }

            // Echo footsteps follow 15 ticks behind
            int echoDelay = 3; // 3 entries = 15 ticks
            if (echoPositions.size() > echoDelay) {
                Location echoLoc = echoPositions.get(echoPositions.size() - 1 - echoDelay);
                if (ticksAlive % 5 == 0) {
                    // Ghostly footstep particles
                    w.spawnParticle(Particle.SOUL, echoLoc.clone().add(0, 0.1, 0),
                            5, 0.3, 0.1, 0.3, 0.02);
                    DisplayBuilder.dustParticles(echoLoc.clone().add(0, 0.2, 0),
                            3, 0.3, 120, 80, 200, 1.0f);
                    DisplayBuilder.playSound(echoLoc, Sound.ENTITY_ZOMBIE_STEP, 0.3f, 0.4f);
                }
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); echoPositions.clear(); }
        @Override public AbstractAttack newInstance() { return new EchoStep(plugin); }
    }

    // ================================================================
    // 7. MIRROR WORLD — Particle "reflections" of the player appear
    //    at mirrored positions. Reflection particles do damage.
    // ================================================================
    public static class MirrorWorld extends EnvironmentalAttack {
        public MirrorWorld(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_world", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Create "reflections" — particles at mirrored positions
            if (ticksAlive % 4 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) > 100) continue;

                    // Mirror across center
                    double dx = p.getLocation().getX() - c.getX();
                    double dz = p.getLocation().getZ() - c.getZ();
                    Location mirrorLoc = c.clone().add(-dx, p.getLocation().getY() - c.getY(), -dz);

                    // Ghostly silhouette particles at mirror position
                    w.spawnParticle(Particle.SOUL, mirrorLoc, 5, 0.2, 0.8, 0.2, 0.02);
                    DisplayBuilder.dustParticles(mirrorLoc, 3, 0.3, 150, 150, 200, 1.5f);
                }
            }

            // Mirror shimmer line through center
            if (ticksAlive % 6 == 0) {
                for (int i = -5; i <= 5; i++) {
                    w.spawnParticle(Particle.END_ROD,
                            c.clone().add(i, 0.5, 0), 1, 0, 0.5, 0, 0.01);
                }
            }

            if (ticksAlive % 35 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MirrorWorld(plugin); }
    }

    // ================================================================
    // 8. DREAM REWIND — Players periodically teleported back to where
    //    they were 3 seconds ago. Reverse particles trail backward.
    // ================================================================
    public static class DreamRewind extends EnvironmentalAttack {
        private final List<Location> positionHistory = new ArrayList<>();

        public DreamRewind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_rewind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(60);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Store center position every 10 ticks
            if (ticksAlive % 10 == 0) {
                positionHistory.add(c.clone());
                if (positionHistory.size() > 12) positionHistory.remove(0);
            }

            // Rewind trail particles
            if (ticksAlive % 4 == 0 && positionHistory.size() > 1) {
                int idx = Math.max(0, positionHistory.size() - 4);
                Location pastLoc = positionHistory.get(idx);
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, pastLoc, 5, 0.5, 0.5, 0.5, 0.1);
                DisplayBuilder.dustParticles(pastLoc, 3, 0.5, 200, 100, 50, 1.2f);
            }

            // Ticking countdown sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
            }

            // Ambient time particles
            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2, 0),
                        5, 3, 1, 3, 0.5);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); positionHistory.clear(); }
        @Override public AbstractAttack newInstance() { return new DreamRewind(plugin); }
    }

    // ================================================================
    // 9. SIZE SHIFT — Ground-level blocks pulse between large and small
    //    creating an Alice in Wonderland disorientation effect.
    // ================================================================
    public static class SizeShift extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> shiftBlocks = new ArrayList<>();

        public SizeShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("size_shift", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Material[] mats = {Material.MUSHROOM_STEM, Material.RED_MUSHROOM_BLOCK, Material.BROWN_MUSHROOM_BLOCK};
            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 1 + Math.random() * 6;
                BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist),
                        mats[i % 3]);
                b.scale(1.0f, 1.0f, 1.0f).glow(180, 100, 120).interpolation(5, 0);
                shiftBlocks.add(b);
                spawnedEntities.add(b.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_CHORUS_FLOWER_GROW, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            for (int i = 0; i < shiftBlocks.size(); i++) {
                float scale = 0.3f + (float)(Math.sin(ticksAlive * 0.04 + i * 0.7) + 1.0) * 1.5f;
                shiftBlocks.get(i).scale(scale, scale, scale);
            }

            if (ticksAlive % 8 == 0) {
                c.getWorld().spawnParticle(Particle.ENCHANT,
                        c.clone().add(Math.random() * 12 - 6, 1, Math.random() * 12 - 6),
                        3, 0.3, 0.3, 0.3, 0.3);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHORUS_FLOWER_GROW, 0.3f, 0.5f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SizeShift(plugin); }
    }

    // ================================================================
    // 10. PARALLEL SELF — Particle silhouettes appear at offset
    //     positions from the player, mimicking movement.
    // ================================================================
    public static class ParallelSelf extends EnvironmentalAttack {
        public ParallelSelf(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("parallel_self", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (ticksAlive % 5 == 0) {
                // 3 parallel selves at offset positions
                double[][] offsets = {{3, 0}, {-3, 0}, {0, 3}};
                for (double[] off : offsets) {
                    Location shadowLoc = c.clone().add(off[0], 0, off[1]);
                    // Player silhouette made of dust particles
                    for (double y = 0; y <= 1.8; y += 0.3) {
                        DisplayBuilder.dustParticles(shadowLoc.clone().add(0, y, 0),
                                2, 0.15, 80, 50, 120, 1.2f);
                    }
                }
            }

            // Eerie presence
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ParallelSelf(plugin); }
    }

    // ================================================================
    // 11. PHASE FLICKER — Bursts of portal particles and sounds
    //     as reality phases in and out. Periodic damage pulses.
    // ================================================================
    public static class PhaseFlicker extends EnvironmentalAttack {
        public PhaseFlicker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_flicker", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Flicker on/off
            boolean flickerOn = (ticksAlive % 20) < 8;
            if (flickerOn && ticksAlive % 2 == 0) {
                // Burst of portal particles
                w.spawnParticle(Particle.REVERSE_PORTAL, c.clone().add(0, 1.5, 0),
                        20, 4, 2, 4, 0.1);
                w.spawnParticle(Particle.PORTAL, c.clone().add(0, 1, 0),
                        15, 3, 1.5, 3, 0.5);
            }

            // Phase sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f,
                        0.3f + (float)(Math.random() * 0.5));
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PhaseFlicker(plugin); }
    }

    // ================================================================
    // 12. MEMORY BLEED — Past events "bleed" into the present.
    //     Random particle structures briefly flash into existence.
    // ================================================================
    public static class MemoryBleed extends EnvironmentalAttack {
        public MemoryBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_bleed", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(270);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Brief particle structure flashes every 30 ticks
            if (ticksAlive % 30 < 8) {
                int structureType = (ticksAlive / 30) % 4;
                Location flashLoc = c.clone().add(Math.random() * 8 - 4, 0, Math.random() * 8 - 4);

                if (structureType == 0) {
                    // Phantom ring
                    DisplayBuilder.particleRing(flashLoc.clone().add(0, 1, 0), 2.0,
                            Particle.SOUL, 12, null);
                } else if (structureType == 1) {
                    // Vertical line
                    for (double y = 0; y < 4; y += 0.5) {
                        w.spawnParticle(Particle.END_ROD, flashLoc.clone().add(0, y, 0),
                                1, 0.05, 0.05, 0.05, 0.01);
                    }
                } else if (structureType == 2) {
                    // Cross on ground
                    for (int d = -2; d <= 2; d++) {
                        w.spawnParticle(Particle.SOUL, flashLoc.clone().add(d, 0.2, 0),
                                1, 0.05, 0.05, 0.05, 0.01);
                        w.spawnParticle(Particle.SOUL, flashLoc.clone().add(0, 0.2, d),
                                1, 0.05, 0.05, 0.05, 0.01);
                    }
                } else {
                    // Sphere outline
                    for (int i = 0; i < 8; i++) {
                        double angle = (2 * Math.PI * i) / 8;
                        w.spawnParticle(Particle.REVERSE_PORTAL,
                                flashLoc.clone().add(Math.cos(angle) * 1.5, 1, Math.sin(angle) * 1.5),
                                2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.4f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MemoryBleed(plugin); }
    }

    // ================================================================
    // 13. LOOP BREAK — Teleport players back to the attack center
    //     periodically. They can't escape the dream loop.
    //     Particle "cage" ring marks the loop boundary.
    // ================================================================
    public static class LoopBreak extends EnvironmentalAttack {
        private double loopRadius = 8.0;

        public LoopBreak(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("loop_break", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.5f);
            DisplayBuilder.particleRing(center, loopRadius, Particle.DUST, 32,
                    new Particle.DustOptions(Color.fromRGB(200, 50, 50), 1.5f));
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Shrink loop boundary
            if (loopRadius > 4.0) loopRadius -= 0.01;

            // Draw loop boundary
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), loopRadius, Particle.DUST,
                        (int)(loopRadius * 4),
                        new Particle.DustOptions(Color.fromRGB(200, 50, 50), 1.5f));
                DisplayBuilder.particleRing(c.clone().add(0, 2.0, 0), loopRadius, Particle.DUST,
                        (int)(loopRadius * 3),
                        new Particle.DustOptions(Color.fromRGB(150, 30, 30), 1.0f));
            }

            // Teleport players who leave the boundary back to center
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                if (p.getLocation().distance(c) > loopRadius) {
                    Location tpLoc = c.clone();
                    tpLoc.setY(p.getLocation().getY());
                    tpLoc.setYaw(p.getLocation().getYaw());
                    tpLoc.setPitch(p.getLocation().getPitch());
                    p.teleport(tpLoc);
                    DisplayBuilder.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.5f);
                    w.spawnParticle(Particle.REVERSE_PORTAL, p.getLocation(), 20, 0.5, 1, 0.5, 0.1);
                }
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 0.3f);
            }
        }

        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LoopBreak(plugin); }
    }
}
