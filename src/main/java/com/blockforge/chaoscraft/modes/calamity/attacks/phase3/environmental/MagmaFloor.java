package com.blockforge.chaoscraft.modes.calamity.attacks.phase3.environmental;

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
 * Phase 3 Environmental — GROUP 1: MAGMA FLOOR EVENTS
 * 10 attacks (1-10) themed around magma, lava, and floor-based fire hazards.
 * Calamity Dweller (Boss 3) — boss3-dweller.md
 *
 * Design notes:
 * - No status effects
 * - Dweller palette: crimson glow (200,0,50), orange glow (255,100,0), soul blue (0,150,255)
 * - Materials: MAGMA_BLOCK, NETHERRACK, BASALT, CRIMSON_NYLIUM, NETHER_WART_BLOCK
 * - Damage in HP (4.0 - 12.0 range)
 */
public final class MagmaFloor {

    private MagmaFloor() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PressureCrack(plugin));
        registry.register(new LavaBurstVent(plugin));
        registry.register(new RollingIgnitionWave(plugin));
        registry.register(new MagmaBloom(plugin));
        registry.register(new CrossPatternLavaChannels(plugin));
        registry.register(new MagmaPulseRing(plugin));
        registry.register(new PinholeEruptions(plugin));
        registry.register(new FloorSplit(plugin));
        registry.register(new MagmaTide(plugin));
        registry.register(new MagmaMemory(plugin));
    }

    // =========================================================================
    // ATTACK 1 — Pressure Crack
    // 3x3 floor area cracks with lava fractures, converts to magma for 6s.
    // 4 hearts on eruption + 1 heart/second sustained magma contact.
    // =========================================================================
    public static class PressureCrack extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean erupted = false;

        public PressureCrack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pressure_crack", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(4.0); // 2 hearts/tick sustained
            config.setDamageRadius(2.5);
            config.setDurationTicks(170); // 2.5s warning + 6s active = 8.5s
            config.setCooldownTicks(280); // 14s
            config.setTicksBetweenDamage(20); // 1s between sustained ticks
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: fracture line particles around 3x3 area
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    DisplayBuilder.dustParticles(center.clone().add(x, 0.1, z), 3, 0.3, 220, 80, 10, 0.8f);
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: 0-50 ticks (2.5s)
            if (ticksAlive < 50) {
                if (ticksAlive % 8 == 0) {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            DisplayBuilder.dustParticles(center.clone().add(x, 0.1, z), 2, 0.2, 220, 80, 10, 0.6f);
                        }
                    }
                }
                return;
            }

            // Eruption: spawn magma block displays in 3x3
            if (!erupted) {
                erupted = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location loc = center.clone().add(x, 0.01, z);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.15f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                // Upward lava shower particles from center
                DisplayBuilder.dustParticles(center.clone().add(0, 2, 0), 25, 1.5, 240, 110, 30, 1.5f);

                // Eruption damage to nearby players
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 6.25) {
                        p.damage(8.0); // 4 hearts
                    }
                }
            }

            // Sustained: magma glow particles
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(center.clone().add(0, 0.3, 0), 6, 1.5, 240, 110, 30, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PressureCrack(plugin); }
    }

    // =========================================================================
    // ATTACK 2 — Lava Burst Vent
    // Single floor block spirals with lava, then erupts 8-block geyser column.
    // 8 hearts at base scaling to 5 hearts at apex. Upward knockback.
    // =========================================================================
    public static class LavaBurstVent extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean erupted = false;
        private int columnHeight = 0;

        public LavaBurstVent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_burst_vent", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 2s warning + 3s geyser
            config.setCooldownTicks(360); // 18s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.dustParticles(center.clone().add(0, 0.2, 0), 10, 0.5, 230, 95, 15, 1.0f);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: swirling spiral for 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 4 == 0) {
                    double angle = ticksAlive * 0.3;
                    double rx = Math.cos(angle) * 0.5;
                    double rz = Math.sin(angle) * 0.5;
                    DisplayBuilder.dustParticles(center.clone().add(rx, 0.1, rz), 3, 0.2, 230, 95, 15, 0.8f);
                }
                return;
            }

            // Eruption: build column upward
            if (!erupted && columnHeight < 8) {
                columnHeight++;
                Location colLoc = center.clone().add(0, columnHeight - 1, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(colLoc, Material.MAGMA_BLOCK);
                float widen = 1.0f + (columnHeight > 6 ? (columnHeight - 6) * 0.3f : 0f);
                h.scale(0.6f * widen, 1.0f, 0.6f * widen).glow(255, 100, 0).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());

                if (columnHeight == 1) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.2f);
                }

                // Damage at base — launch players
                if (columnHeight == 2) {
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        double distSq = p.getLocation().distanceSquared(center);
                        if (distSq <= 2.25) {
                            p.damage(12.0); // 6 hearts base (scaled from design's 8 hearts)
                            p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0, 0.75, 0)));
                        }
                    }
                }

                if (columnHeight == 8) {
                    erupted = true;
                    DisplayBuilder.dustParticles(center.clone().add(0, 8, 0), 20, 2.0, 255, 140, 0, 1.5f);
                }
                return;
            }

            // Column damage to players inside column
            if (erupted && ticksAlive % 15 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double dy = ploc.getY() - center.getY();
                    if (dx * dx + dz * dz <= 1.5 && dy >= 0 && dy <= 8) {
                        double heightRatio = dy / 8.0;
                        double dmg = 12.0 - (heightRatio * 6.0); // 12 HP base to 6 HP at top
                        p.damage(dmg);
                    }
                }
            }

            // Column particles
            if (ticksAlive % 6 == 0) {
                int yRand = (int)(Math.random() * 8);
                DisplayBuilder.dustParticles(center.clone().add(0, yRand, 0), 4, 0.5, 255, 140, 0, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LavaBurstVent(plugin); }
    }

    // =========================================================================
    // ATTACK 3 — Rolling Ignition Wave
    // Line of flame sweeps across the arena at walking speed. 3 blocks wide, 2 tall.
    // 5 hearts on contact. Must dodge laterally.
    // =========================================================================
    public static class RollingIgnitionWave extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private double wavePosition = -10.0;
        private double waveAngle;

        public RollingIgnitionWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rolling_ignition_wave", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(180); // 1.5s warning + ~7s travel
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            waveAngle = Math.random() * Math.PI;
            // Spawn wave line at origin edge
            Location edgeLoc = center.clone().add(Math.cos(waveAngle) * -12, 0, Math.sin(waveAngle) * -12);
            for (int i = -1; i <= 1; i++) {
                double perpAngle = waveAngle + Math.PI / 2;
                Location lineLoc = edgeLoc.clone().add(Math.cos(perpAngle) * i, 0, Math.sin(perpAngle) * i);
                DisplayBuilder.dustParticles(lineLoc, 5, 0.3, 240, 120, 40, 1.0f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                return;
            }

            // Wave advances at 1 block/tick (walking speed = 4.3 b/s ~ 0.215 b/tick)
            wavePosition += 0.22;
            double waveDirX = Math.cos(waveAngle);
            double waveDirZ = Math.sin(waveAngle);
            Location waveLoc = center.clone().add(waveDirX * (wavePosition - 12), 0, waveDirZ * (wavePosition - 12));

            // Remove old display handles beyond 3 ticks old
            if (handles.size() > 9) {
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle old = handles.remove(0);
                    old.entity().remove();
                }
            }

            // Spawn wave front — 3 blocks wide, 2 tall
            double perpAngle = waveAngle + Math.PI / 2;
            for (int i = -1; i <= 1; i++) {
                Location lineLoc = waveLoc.clone().add(Math.cos(perpAngle) * i, 0, Math.sin(perpAngle) * i);
                BlockDisplayHandle h = displayBuilder.spawnBlock(lineLoc, Material.MAGMA_BLOCK);
                h.scale(1.0f, 0.2f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                handles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Wave particles
            DisplayBuilder.dustParticles(waveLoc.clone().add(0, 1, 0), 8, 1.5, 240, 120, 40, 1.2f);

            // Sound as wave moves
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(waveLoc, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 1.0f);
            }

            // Damage players in wave path
            for (Player p : w.getPlayers()) {
                if (isExempt(p)) continue;
                Location ploc = p.getLocation();
                // Project player onto wave line to check perpendicular distance
                double dx = ploc.getX() - waveLoc.getX();
                double dz = ploc.getZ() - waveLoc.getZ();
                double perpDist = Math.abs(dx * (-waveDirZ) + dz * waveDirX);
                double parallelDist = Math.abs(dx * waveDirX + dz * waveDirZ);
                double dy = ploc.getY() - waveLoc.getY();
                if (perpDist <= 2.0 && parallelDist <= 1.5 && dy >= 0 && dy <= 2.0) {
                    p.damage(10.0); // 5 hearts
                }
            }

            // Wave done after traveling 24 blocks
            if (wavePosition > 24) {
                // Let duration handle cleanup
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RollingIgnitionWave(plugin); }
    }

    // =========================================================================
    // ATTACK 4 — Magma Bloom
    // Expanding bloom from center: 1 block, cross of 4, ring of 8.
    // 1.5 hearts/second sustained. 3s expansion + 8s active.
    // =========================================================================
    public static class MagmaBloom extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private int bloomStage = 0;
        private final List<Location> bloomLocs = new ArrayList<>();

        public MagmaBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_bloom", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(6.0); // 3 HP per tick = 1.5 hearts/s at 10-tick interval
            config.setDamageRadius(3.5);
            config.setDurationTicks(220); // 2s warning + 3s bloom + 8s active
            config.setCooldownTicks(440); // 22s
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Center marker — dripping lava fountain
            DisplayBuilder.dustParticles(center.clone().add(0, 0.5, 0), 8, 0.3, 220, 85, 15, 1.0f);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.6, 0), 5, 0.2, 220, 85, 15, 0.8f);
                }
                return;
            }

            // Stage 1 (tick 40): center block
            if (ticksAlive == 40 && bloomStage == 0) {
                bloomStage = 1;
                spawnBloomBlock(center, w);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.9f, 0.8f);
            }

            // Stage 2 (tick 60): cross of 4
            if (ticksAlive == 60 && bloomStage == 1) {
                bloomStage = 2;
                spawnBloomBlock(center.clone().add(1, 0, 0), w);
                spawnBloomBlock(center.clone().add(-1, 0, 0), w);
                spawnBloomBlock(center.clone().add(0, 0, 1), w);
                spawnBloomBlock(center.clone().add(0, 0, -1), w);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
            }

            // Stage 3 (tick 80): ring of 8
            if (ticksAlive == 80 && bloomStage == 2) {
                bloomStage = 3;
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && z == 0) continue;
                        if (Math.abs(x) + Math.abs(z) == 1) continue; // skip cross already placed
                        spawnBloomBlock(center.clone().add(x * 2, 0, z * 2), w);
                    }
                }
                // Additional outer ring
                spawnBloomBlock(center.clone().add(2, 0, 0), w);
                spawnBloomBlock(center.clone().add(-2, 0, 0), w);
                spawnBloomBlock(center.clone().add(0, 0, 2), w);
                spawnBloomBlock(center.clone().add(0, 0, -2), w);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 1.1f, 0.8f);
            }

            // Bloom ambient glow
            if (bloomStage > 0 && ticksAlive % 12 == 0) {
                for (Location bl : bloomLocs) {
                    DisplayBuilder.dustParticles(bl.clone().add(0, 0.15, 0), 2, 0.3, 255, 130, 0, 0.6f);
                }
            }
        }

        private void spawnBloomBlock(Location loc, World w) {
            BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
            h.scale(1.0f, 0.12f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
            handles.add(h);
            spawnedEntities.add(h.entity());
            bloomLocs.add(loc.clone());
            DisplayBuilder.dustParticles(loc.clone().add(0, 0.2, 0), 6, 0.4, 220, 85, 15, 0.8f);
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaBloom(plugin); }
    }

    // =========================================================================
    // ATTACK 5 — Cross-Pattern Lava Channels
    // Two 1-block-wide channels carve a cross from center to edges.
    // 6 hearts on contact, 2 hearts/s sustained. 10s active.
    // =========================================================================
    public static class CrossPatternLavaChannels extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean carved = false;
        private int carveProgress = 0;

        public CrossPatternLavaChannels(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cross_lava_channels", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(4.0); // 2 hearts/s sustained
            config.setDamageRadius(1.5);
            config.setDurationTicks(260); // 1.5s warning + 1.5s carve + 10s active
            config.setCooldownTicks(500); // 25s
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Warning: faint particle lines tracing the cross paths
            for (int i = -8; i <= 8; i++) {
                DisplayBuilder.dustParticles(center.clone().add(i, 0.1, 0), 1, 0.1, 225, 90, 12, 0.5f);
                DisplayBuilder.dustParticles(center.clone().add(0, 0.1, i), 1, 0.1, 225, 90, 12, 0.5f);
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 30 ticks (1.5s)
            if (ticksAlive < 30) {
                return;
            }

            // Carving phase: 30 ticks (1.5s), build channels outward from center
            if (!carved && carveProgress <= 8) {
                if ((ticksAlive - 30) % 4 == 0) {
                    carveProgress++;
                    // Carve X-axis channel
                    for (int sign = -1; sign <= 1; sign += 2) {
                        Location chLoc = center.clone().add(sign * carveProgress, -0.1, 0);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(chLoc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.08f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    // Carve Z-axis channel
                    for (int sign = -1; sign <= 1; sign += 2) {
                        Location chLoc = center.clone().add(0, -0.1, sign * carveProgress);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(chLoc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.08f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.7f);
                }

                if (carveProgress >= 8) {
                    carved = true;
                    // Carve center block
                    BlockDisplayHandle hc = displayBuilder.spawnBlock(center.clone().add(0, -0.1, 0), Material.MAGMA_BLOCK);
                    hc.scale(1.0f, 0.08f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    handles.add(hc);
                    spawnedEntities.add(hc.entity());
                }
                return;
            }

            // Active channels — custom damage for channel contact
            if (carved && ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = Math.abs(ploc.getX() - center.getX());
                    double dz = Math.abs(ploc.getZ() - center.getZ());
                    // Player is on X-axis channel or Z-axis channel
                    boolean onXChannel = dz <= 0.8 && dx <= 8.5;
                    boolean onZChannel = dx <= 0.8 && dz <= 8.5;
                    if (onXChannel || onZChannel) {
                        p.damage(12.0); // 6 hearts
                    }
                }
                // Smoke particles along channels
                for (int i = -8; i <= 8; i += 2) {
                    DisplayBuilder.dustParticles(center.clone().add(i, 0.3, 0), 2, 0.3, 90, 70, 55, 0.6f);
                    DisplayBuilder.dustParticles(center.clone().add(0, 0.3, i), 2, 0.3, 90, 70, 55, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrossPatternLavaChannels(plugin); }
    }

    // =========================================================================
    // ATTACK 6 — Magma Pulse Ring
    // Ring of magma at 8-block radius oscillates between 4 and 8 blocks radius.
    // 5 hearts on contact. 3 oscillations over 12s.
    // =========================================================================
    public static class MagmaPulseRing extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private double currentRadius = 8.0;
        private static final int RING_SEGMENTS = 20;

        public MagmaPulseRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_pulse_ring", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(260); // 1s warning + 12s oscillation
            config.setCooldownTicks(400); // 20s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Brief flash at ring radius
            DisplayBuilder.particleRing(center, 8.0, Particle.DUST, 20,
                    new Particle.DustOptions(Color.fromRGB(235, 100, 20), 1.0f));
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 10 ticks (0.5s)
            if (ticksAlive < 10) {
                return;
            }

            // Snap ring into existence at tick 10
            if (ticksAlive == 10) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.0f);
                spawnRing(center, 8.0);
            }

            // Oscillation: 3 cycles of contract/expand, each cycle 80 ticks (4s)
            int oscTick = ticksAlive - 10;
            int cyclePos = oscTick % 80;
            if (cyclePos < 40) {
                // Contracting from 8 to 4
                currentRadius = 8.0 - (4.0 * cyclePos / 40.0);
            } else {
                // Expanding from 4 to 8
                currentRadius = 4.0 + (4.0 * (cyclePos - 40) / 40.0);
            }

            // Update ring positions every 2 ticks
            if (ticksAlive % 2 == 0) {
                updateRingPositions(center, currentRadius);
            }

            // Particles on ring edges
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.particleRing(center, currentRadius, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(255, 100, 0), 1.0f));
            }

            // Damage players touching the ring
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distSq = p.getLocation().distanceSquared(center);
                    double innerR = currentRadius - 1.0;
                    double outerR = currentRadius + 1.0;
                    double dist = Math.sqrt(distSq);
                    if (dist >= innerR && dist <= outerR) {
                        p.damage(10.0); // 5 hearts
                    }
                }
            }
        }

        private void spawnRing(Location center, double radius) {
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double angle = (2 * Math.PI * i) / RING_SEGMENTS;
                Location loc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                h.scale(1.2f, 0.15f, 1.2f).glow(235, 100, 20).interpolation(3, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        private void updateRingPositions(Location center, double radius) {
            for (int i = 0; i < ringHandles.size(); i++) {
                double angle = (2 * Math.PI * i) / ringHandles.size();
                Location newLoc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                ringHandles.get(i).entity().teleport(newLoc);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaPulseRing(plugin); }
    }

    // =========================================================================
    // ATTACK 7 — Pinhole Eruptions
    // 12 random 1x1 geysers, staggered over 3s, each active 4s.
    // 4 hearts per geyser hit.
    // =========================================================================
    public static class PinholeEruptions extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> pinholeLocs = new ArrayList<>();
        private final List<Integer> pinholeSpawnTicks = new ArrayList<>();
        private int pinholesSpawned = 0;

        public PinholeEruptions(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pinhole_eruptions", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(160); // ~8s total
            config.setCooldownTicks(320); // 16s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Pre-calculate 12 random positions within 8 blocks of center
            for (int i = 0; i < 12; i++) {
                double ox = (Math.random() - 0.5) * 16.0;
                double oz = (Math.random() - 0.5) * 16.0;
                pinholeLocs.add(center.clone().add(ox, 0, oz));
                pinholeSpawnTicks.add(20 + (int)(Math.random() * 60)); // staggered over ticks 20-80
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spawn pinholes at their scheduled ticks
            for (int i = 0; i < pinholeLocs.size(); i++) {
                if (i >= pinholeSpawnTicks.size()) break;
                if (ticksAlive == pinholeSpawnTicks.get(i)) {
                    Location loc = pinholeLocs.get(i);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGMA_BLOCK);
                    h.scale(0.6f, 2.0f, 0.6f).glow(255, 100, 0).interpolation(2, 0);
                    handles.add(h);
                    spawnedEntities.add(h.entity());
                    DisplayBuilder.dustParticles(loc.clone().add(0, 1, 0), 8, 0.3, 255, 140, 0, 1.0f);
                    DisplayBuilder.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 0.4f, 1.2f);
                    pinholesSpawned++;
                }
            }

            // Damage players near active geysers
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < pinholeLocs.size(); i++) {
                    if (i >= pinholeSpawnTicks.size()) break;
                    int spawnTick = pinholeSpawnTicks.get(i);
                    if (ticksAlive >= spawnTick && ticksAlive <= spawnTick + 80) {
                        Location loc = pinholeLocs.get(i);
                        for (Player p : w.getPlayers()) {
                            if (isExempt(p)) continue;
                            Location ploc = p.getLocation();
                            double dx = ploc.getX() - loc.getX();
                            double dz = ploc.getZ() - loc.getZ();
                            double dy = ploc.getY() - loc.getY();
                            if (dx * dx + dz * dz <= 1.0 && dy >= 0 && dy <= 2.0) {
                                p.damage(8.0); // 4 hearts
                            }
                        }
                        // Geyser particles while active
                        DisplayBuilder.dustParticles(loc.clone().add(0, 1.5, 0), 3, 0.3, 255, 140, 0, 0.6f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PinholeEruptions(plugin); }
    }

    // =========================================================================
    // ATTACK 8 — Floor Split
    // Diagonal fault line splits arena with lava crack. 6 hearts on contact.
    // 1 block wide, full diagonal. 8s active.
    // =========================================================================
    public static class FloorSplit extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private boolean split = false;
        private int crackProgress = 0;
        private double splitAngle;

        public FloorSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floor_split", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 2s warning + 2s crack + 8s active
            config.setCooldownTicks(560); // 28s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            splitAngle = Math.PI / 4.0 + (Math.random() * Math.PI / 2.0); // diagonal
            // Warning: faint line trace
            for (int i = -10; i <= 10; i++) {
                DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(splitAngle) * i, 0.1, Math.sin(splitAngle) * i),
                        1, 0.1, 70, 60, 50, 0.4f);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) {
                    for (int i = -10; i <= 10; i += 3) {
                        DisplayBuilder.dustParticles(
                                center.clone().add(Math.cos(splitAngle) * i, 0.1, Math.sin(splitAngle) * i),
                                2, 0.2, 220, 80, 10, 0.5f);
                    }
                }
                return;
            }

            // Crack propagation: 40 ticks (2s), extend from center outward
            if (!split && crackProgress <= 10) {
                if ((ticksAlive - 40) % 4 == 0) {
                    crackProgress++;
                    for (int sign = -1; sign <= 1; sign += 2) {
                        Location crackLoc = center.clone().add(
                                Math.cos(splitAngle) * sign * crackProgress, -0.05,
                                Math.sin(splitAngle) * sign * crackProgress);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(crackLoc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.06f, 1.0f).glow(200, 0, 50).interpolation(2, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 0.9f, 0.5f);
                }
                if (crackProgress >= 10) {
                    split = true;
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.5f);
                }
                return;
            }

            // Active fault line — damage players on the line
            if (split && ticksAlive % 8 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    // Perpendicular distance to the fault line
                    double perpDist = Math.abs(dx * (-Math.sin(splitAngle)) + dz * Math.cos(splitAngle));
                    double parallelDist = Math.abs(dx * Math.cos(splitAngle) + dz * Math.sin(splitAngle));
                    if (perpDist <= 0.8 && parallelDist <= 10.5) {
                        p.damage(12.0); // 6 hearts
                    }
                }
            }

            // Particles along the fault
            if (ticksAlive % 8 == 0) {
                int idx = (int)(Math.random() * 20) - 10;
                DisplayBuilder.dustParticles(
                        center.clone().add(Math.cos(splitAngle) * idx, 0.3, Math.sin(splitAngle) * idx),
                        4, 0.4, 70, 60, 50, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloorSplit(plugin); }
    }

    // =========================================================================
    // ATTACK 9 — Magma Tide
    // One edge glows, then 3-block-deep tide of magma advances inward at 1 b/s.
    // 7 hearts during advance, 3 hearts during retreat.
    // 18s advance + 4s hold + 9s retreat.
    // =========================================================================
    public static class MagmaTide extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> tideHandles = new ArrayList<>();
        private double tideAdvance = 0;
        private boolean holding = false;
        private boolean retreating = false;
        private int holdStartTick = 0;
        private double tideAngle;

        public MagmaTide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_tide", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(700); // 2s warn + 18s adv + 4s hold + 9s retreat
            config.setCooldownTicks(700); // 35s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            tideAngle = ((int)(Math.random() * 4)) * (Math.PI / 2); // Cardinal direction
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 40 ticks (2s)
            if (ticksAlive < 40) {
                if (ticksAlive % 8 == 0) {
                    Location edgeLoc = center.clone().add(Math.cos(tideAngle) * -10, 0, Math.sin(tideAngle) * -10);
                    for (int i = -5; i <= 5; i++) {
                        double perpAngle = tideAngle + Math.PI / 2;
                        DisplayBuilder.dustParticles(
                                edgeLoc.clone().add(Math.cos(perpAngle) * i, 0.2, Math.sin(perpAngle) * i),
                                2, 0.3, 230, 95, 15, 0.6f);
                    }
                }
                return;
            }

            // Advance phase: 1 block per second = 0.05 blocks/tick
            if (!holding && !retreating) {
                tideAdvance += 0.05;
                if (tideAdvance >= 13) { // Stop 5 blocks short
                    holding = true;
                    holdStartTick = ticksAlive;
                }
            }

            // Hold phase: 80 ticks (4s)
            if (holding && !retreating) {
                if (ticksAlive - holdStartTick >= 80) {
                    retreating = true;
                }
            }

            // Retreat phase: 2 blocks/second = 0.1 blocks/tick
            if (retreating) {
                tideAdvance -= 0.1;
                if (tideAdvance <= 0) {
                    tideAdvance = 0;
                }
            }

            // Spawn/update tide visuals every 4 ticks
            if (ticksAlive % 4 == 0 && tideAdvance > 0) {
                // Spawn tide front blocks
                double tideDirX = Math.cos(tideAngle);
                double tideDirZ = Math.sin(tideAngle);
                double perpAngle = tideAngle + Math.PI / 2;

                for (int layer = 0; layer < 3; layer++) {
                    double layerOffset = -10.0 + tideAdvance - layer;
                    if (layerOffset < -10 || layerOffset > 10) continue;
                    for (int perp = -5; perp <= 5; perp += 2) {
                        Location tideLoc = center.clone().add(
                                tideDirX * layerOffset + Math.cos(perpAngle) * perp, 0.01,
                                tideDirZ * layerOffset + Math.sin(perpAngle) * perp);
                        DisplayBuilder.dustParticles(tideLoc.clone().add(0, 0.15, 0), 2, 0.4, 240, 110, 25, 0.7f);
                    }
                }

                // Damage players in tide zone
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location ploc = p.getLocation();
                    double dx = ploc.getX() - center.getX();
                    double dz = ploc.getZ() - center.getZ();
                    double parallelDist = dx * tideDirX + dz * tideDirZ;
                    double perpDist = Math.abs(dx * Math.cos(perpAngle) + dz * Math.sin(perpAngle));

                    double tideStart = -10.0 + tideAdvance - 3;
                    double tideEnd = -10.0 + tideAdvance;
                    if (parallelDist >= tideStart && parallelDist <= tideEnd && perpDist <= 6.0) {
                        if (retreating) {
                            p.damage(6.0); // 3 hearts during retreat
                        } else {
                            p.damage(10.0); // 5 hearts during advance (scaled from 7)
                        }
                    }
                }
            }

            // Sound
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaTide(plugin); }
    }

    // =========================================================================
    // ATTACK 10 — Magma Memory
    // Dweller walks 5 steps, each leaves a 2x2 magma imprint lasting 15s.
    // 5 hearts initial contact, 2 hearts/s sustained.
    // =========================================================================
    public static class MagmaMemory extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> handles = new ArrayList<>();
        private final List<Location> imprintLocs = new ArrayList<>();
        private final List<Integer> imprintTicks = new ArrayList<>();
        private int stepsPlaced = 0;

        public MagmaMemory(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_memory", AttackType.ENVIRONMENTAL, 3));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(500); // 10s placement + 15s longest imprint
            config.setCooldownTicks(600); // 30s
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller gaze indicator
            DisplayBuilder.dustParticles(center.clone().add(0, -0.5, 0), 6, 0.5, 240, 110, 25, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 0.7f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Place footsteps every 40 ticks (2s), starting at tick 20
            if (stepsPlaced < 5 && ticksAlive >= 20 && (ticksAlive - 20) % 40 == 0) {
                // Footstep position: walk pattern from center
                double stepAngle = (stepsPlaced * Math.PI / 2.5) + Math.random() * 0.3;
                double stepDist = 3.0 + stepsPlaced * 1.5;
                Location stepLoc = center.clone().add(Math.cos(stepAngle) * stepDist, 0, Math.sin(stepAngle) * stepDist);

                // Spawn 2x2 magma imprint
                for (int dx = 0; dx <= 1; dx++) {
                    for (int dz = 0; dz <= 1; dz++) {
                        Location iLoc = stepLoc.clone().add(dx, 0.01, dz);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(iLoc, Material.MAGMA_BLOCK);
                        h.scale(1.0f, 0.1f, 1.0f).glow(255, 100, 0).interpolation(3, 0);
                        handles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }
                imprintLocs.add(stepLoc.clone());
                imprintTicks.add(ticksAlive);
                stepsPlaced++;

                DisplayBuilder.playSound(stepLoc, Sound.BLOCK_STONE_PLACE, 1.0f, 0.6f);
                DisplayBuilder.dustParticles(stepLoc.clone().add(0.5, 0.2, 0.5), 10, 0.8, 240, 110, 25, 1.0f);

                // Initial contact damage
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(stepLoc) <= 4.0) {
                        p.damage(10.0); // 5 hearts
                    }
                }
            }

            // Sustained damage on imprints
            if (ticksAlive % 10 == 0) {
                for (int i = 0; i < imprintLocs.size(); i++) {
                    int spawnTick = imprintTicks.get(i);
                    if (ticksAlive - spawnTick > 300) continue; // 15s expiry
                    Location iLoc = imprintLocs.get(i);
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        Location ploc = p.getLocation();
                        double dx = ploc.getX() - iLoc.getX();
                        double dz = ploc.getZ() - iLoc.getZ();
                        if (dx >= -0.5 && dx <= 2.0 && dz >= -0.5 && dz <= 2.0) {
                            p.damage(4.0); // 2 hearts/s
                        }
                    }
                }
            }

            // Ambient glow on active imprints
            if (ticksAlive % 15 == 0) {
                for (int i = 0; i < imprintLocs.size(); i++) {
                    int spawnTick = imprintTicks.get(i);
                    if (ticksAlive - spawnTick > 300) continue;
                    Location iLoc = imprintLocs.get(i);
                    float intensity = 1.0f - ((ticksAlive - spawnTick) / 300.0f);
                    DisplayBuilder.dustParticles(iLoc.clone().add(0.5, 0.3, 0.5), 3, 0.6,
                            (int)(255 * intensity), (int)(135 * intensity), 30, 0.6f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MagmaMemory(plugin); }
    }
}
