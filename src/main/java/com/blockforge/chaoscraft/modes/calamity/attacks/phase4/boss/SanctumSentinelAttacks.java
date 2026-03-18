package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss;

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
 * Phase 4D Mini Boss 4 — THE SANCTUM SENTINEL
 * The Dragon's own creation — a golden knight-construct assembled from the Sanctum floor.
 * Gold/gilded blackstone/purpur theme. Slow but devastating.
 * Unique Mechanic: Awakening at 50% HP (speed + attack boost).
 * 20 attacks total.
 * NO status effects — damage only.
 */
public final class SanctumSentinelAttacks {

    private SanctumSentinelAttacks() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GroundSlam(plugin));
        registry.register(new ShieldReflect(plugin));
        registry.register(new GoldenBeamCannon(plugin));
        registry.register(new ArenaStomp(plugin));
        registry.register(new GoldSpikeArray(plugin));
        registry.register(new CrushingMarch(plugin));
        registry.register(new SanctumAnchor(plugin));
        registry.register(new BlockThrow(plugin));
        registry.register(new GildedStompCombo(plugin));
        registry.register(new GoldenFortification(plugin));
        registry.register(new VoidCorruptionPunch(plugin));
        registry.register(new TowerShieldBash(plugin));
        registry.register(new SanctumRenovation(plugin));
        registry.register(new EyeOfTheSentinel(plugin));
        registry.register(new PillarFormation(plugin));
        registry.register(new ResonantShockwave(plugin));
        registry.register(new GoldenFuneral(plugin));
        registry.register(new IronWill(plugin));
        registry.register(new CollapsePrelude(plugin));
        registry.register(new TheCollapse(plugin));
    }

    // ================================================================
    // 1. GROUND SLAM — Massive overhead arm slam with shockwave
    // ================================================================
    public static class GroundSlam extends BossAttack {
        private final List<BlockDisplayHandle> slamHandles = new ArrayList<>();
        private boolean slammed = false;

        public GroundSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_ground_slam", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Oversized arm raised overhead
            BlockDisplayHandle arm = displayBuilder.spawnBlock(
                center.clone().add(0, 6, 0), Material.GOLD_BLOCK);
            arm.scale(1.5f, 2.5f, 1.5f).glow(255, 200, 0).interpolation(3, 0);
            slamHandles.add(arm);
            spawnedEntities.add(arm.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 10, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Arm raised with TOTEM particle rain (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !slammed) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 6, 0), 8, 2.0);
                }
            }
            // Slam down (tick 40)
            else if (ticksAlive == 40 && !slammed) {
                slammed = true;
                // Arm crashes to ground
                slamHandles.get(0).entity().teleport(center.clone().add(0, 0, 0));

                // Shockwave ring at 3-block and 6-block radii
                for (int ring = 0; ring < 2; ring++) {
                    float radius = ring == 0 ? 3 : 6;
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        BlockDisplayHandle wave = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius),
                            Material.CRACKED_DEEPSLATE_TILES);
                        wave.scale(0.8f, 0.1f, 0.8f).glow(100, 80, 60).interpolation(1, 0);
                        slamHandles.add(wave);
                        spawnedEntities.add(wave.entity());
                    }
                }

                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 30, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.7f);
            }
            // Recovery (40-60 ticks = 1 second)
            else if (slammed && ticksAlive < 60) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 4, 4.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundSlam(plugin); }
    }

    // ================================================================
    // 2. SHIELD REFLECT — Front-facing reflective gold panels
    // ================================================================
    public static class ShieldReflect extends BossAttack {
        private final List<BlockDisplayHandle> shieldHandles = new ArrayList<>();
        private boolean shieldActive = false;
        private int shieldStartTick = 0;

        public ShieldReflect(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_shield_reflect", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arms raise in front — defensive posture with gold panels
            for (int y = 0; y < 4; y++) {
                for (int x = -1; x <= 1; x++) {
                    BlockDisplayHandle panel = displayBuilder.spawnBlock(
                        center.clone().add(x, y, 1.5), Material.GOLD_BLOCK);
                    panel.scale(0.9f, 0.9f, 0.3f).glow(255, 200, 0).interpolation(2, 0);
                    shieldHandles.add(panel);
                    spawnedEntities.add(panel.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Posture shift (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !shieldActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 1.5), 6, 2.0);
                }
            }
            // Shield active (tick 20) — 60 ticks (3 seconds)
            else if (ticksAlive == 20 && !shieldActive) {
                shieldActive = true;
                shieldStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.8f);
            }
            // Shield pulsing (20-80 ticks)
            else if (shieldActive && ticksAlive - shieldStartTick < 60) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 1.5), 6, 2.0);
                }
            }
            // Shield drops (tick 80)
            else if (shieldActive && ticksAlive - shieldStartTick == 60) {
                shieldActive = false;
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_USE, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShieldReflect(plugin); }
    }

    // ================================================================
    // 3. GOLDEN BEAM CANNON — Thick golden beam from chest cavity
    // ================================================================
    public static class GoldenBeamCannon extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int beamStartTick = 0;

        public GoldenBeamCannon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_golden_beam_cannon", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Golden beam builds inside chest — TOTEM particles converge inward
            BlockDisplayHandle chestGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            chestGlow.scale(1.5f, 1.5f, 1.5f).glow(255, 200, 0).interpolation(4, 0);
            beamHandles.add(chestGlow);
            spawnedEntities.add(chestGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Charge-up (0-50 ticks = 2.5 seconds)
            if (ticksAlive < 50 && !beamActive) {
                if (ticksAlive % 8 == 0) {
                    // Particles converge inward from outside
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI * 2 / 4) * i;
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(angle) * 5, 2, Math.sin(angle) * 5), 4, 1.0);
                    }
                }
            }
            // Beam fires (tick 50) — sustained for 60 ticks (3 seconds)
            else if (ticksAlive == 50 && !beamActive) {
                beamActive = true;
                beamStartTick = ticksAlive;
                // Thick golden beam column
                for (int seg = 0; seg < 20; seg++) {
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(
                        center.clone().add(0, 2, seg * 1.2), Material.GOLD_BLOCK);
                    beamSeg.scale(0.6f, 0.5f, 1.0f).glow(255, 200, 0).interpolation(1, 0);
                    beamHandles.add(beamSeg);
                    spawnedEntities.add(beamSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 2.0f, 0.4f);
            }
            // Beam sweeps slowly (50-110 ticks)
            else if (beamActive && ticksAlive - beamStartTick < 60) {
                float sweepAngle = ((ticksAlive - beamStartTick) / 60.0f) * 0.4f;
                for (int seg = 1; seg < beamHandles.size(); seg++) {
                    int idx = seg - 1;
                    float dist = idx * 1.2f;
                    beamHandles.get(seg).entity().teleport(
                        center.clone().add(
                            Math.sin(sweepAngle) * dist, 2, Math.cos(sweepAngle) * dist));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 12), 6, 3.0);
                }
            }
            // Beam ends — recoil (tick 110)
            else if (beamActive && ticksAlive - beamStartTick == 60) {
                beamActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldenBeamCannon(plugin); }
    }

    // ================================================================
    // 4. ARENA STOMP — Seismic shockwave ring + floor cracks
    // ================================================================
    public static class ArenaStomp extends BossAttack {
        private final List<BlockDisplayHandle> stompHandles = new ArrayList<>();
        private boolean stompFired = false;
        private int stompStartTick = 0;

        public ArenaStomp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_arena_stomp", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // First stomp — small tremor (warning)
            BlockDisplayHandle foot = displayBuilder.spawnBlock(
                center.clone().add(1, 0, 0), Material.GILDED_BLACKSTONE);
            foot.scale(1.5f, 0.5f, 1.5f).glow(200, 160, 0).interpolation(2, 0);
            stompHandles.add(foot);
            spawnedEntities.add(foot.entity());
            DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // First stomp tremor (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !stompFired) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 6, 5.0);
                }
            }
            // Second stomp — full force (tick 30)
            else if (ticksAlive == 30 && !stompFired) {
                stompFired = true;
                stompStartTick = ticksAlive;
                // Expanding shockwave ring
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 0.15, Math.sin(angle) * 3),
                        Material.GOLD_BLOCK);
                    wave.scale(0.8f, 0.15f, 0.8f).glow(255, 200, 0).interpolation(1, 0);
                    stompHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 25, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.5f, 0.5f);
            }
            // Shockwave expands (30-60 ticks = 1.5 seconds to 18 blocks)
            else if (stompFired && ticksAlive - stompStartTick < 30) {
                float radius = 3 + ((ticksAlive - stompStartTick) / 30.0f) * 15;
                for (int i = 1; i < stompHandles.size(); i++) {
                    int idx = i - 1;
                    double angle = (Math.PI * 2 / 16) * idx;
                    stompHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.15, Math.sin(angle) * radius));
                }
            }
            // Floor crack lines (tick 60)
            else if (stompFired && ticksAlive - stompStartTick == 30) {
                // Radial crack pattern
                for (int ray = 0; ray < 6; ray++) {
                    double angle = (Math.PI * 2 / 6) * ray;
                    for (int seg = 1; seg <= 12; seg++) {
                        BlockDisplayHandle crack = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * seg, 0.03, Math.sin(angle) * seg),
                            Material.CRACKED_DEEPSLATE_TILES);
                        crack.scale(0.5f, 0.04f, 0.3f).glow(100, 80, 60).interpolation(2, 0);
                        spawnedEntities.add(crack.entity());
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaStomp(plugin); }
    }

    // ================================================================
    // 5. GOLD SPIKE ARRAY — 6 gold spike pillars erupt from ground
    // ================================================================
    public static class GoldSpikeArray extends BossAttack {
        private final List<BlockDisplayHandle> spikeHandles = new ArrayList<>();
        private boolean spikesErupted = false;
        private final Location[] spikeLocations = new Location[6];

        public GoldSpikeArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_gold_spike_array", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(10.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 6 gold rings on ground
            for (int i = 0; i < 6; i++) {
                double ox = (Math.random() - 0.5) * 20;
                double oz = (Math.random() - 0.5) * 20;
                spikeLocations[i] = center.clone().add(ox, 0, oz);
                for (int ring = 0; ring < 6; ring++) {
                    double angle = (Math.PI * 2 / 6) * ring;
                    BlockDisplayHandle circle = displayBuilder.spawnBlock(
                        spikeLocations[i].clone().add(Math.cos(angle) * 2, 0.05, Math.sin(angle) * 2),
                        Material.GOLD_BLOCK);
                    circle.scale(0.3f, 0.05f, 0.3f).glow(255, 200, 0).interpolation(3, 0);
                    spikeHandles.add(circle);
                    spawnedEntities.add(circle.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Circles accumulate (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !spikesErupted) {
                if (ticksAlive % 8 == 0) {
                    for (Location loc : spikeLocations) {
                        if (loc != null) DisplayBuilder.cyanDust(loc.clone().add(0, 0.2, 0), 5, 2.0);
                    }
                }
            }
            // Spikes erupt (tick 40)
            else if (ticksAlive == 40 && !spikesErupted) {
                spikesErupted = true;
                for (Location spikeLoc : spikeLocations) {
                    if (spikeLoc == null) continue;
                    // 3-block tall gold spike
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle spike = displayBuilder.spawnBlock(
                            spikeLoc.clone().add(0, y, 0), Material.GOLD_BLOCK);
                        spike.scale(0.6f, 0.9f, 0.6f).glow(255, 200, 0).interpolation(2, 0);
                        spikeHandles.add(spike);
                        spawnedEntities.add(spike.entity());
                    }
                    DisplayBuilder.cyanDust(spikeLoc.clone().add(0, 1.5, 0), 8, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.6f);
            }
            // Spikes persist (40-400 ticks = 18 seconds)
            else if (spikesErupted && ticksAlive < 400) {
                if (ticksAlive % 20 == 0) {
                    for (Location loc : spikeLocations) {
                        if (loc != null) DisplayBuilder.cyanDust(loc.clone().add(0, 1.5, 0), 3, 1.0);
                    }
                }
            }
            // Spikes retract (tick 400)
            else if (spikesErupted && ticksAlive == 400) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldSpikeArray(plugin); }
    }

    // ================================================================
    // 6. CRUSHING MARCH — Speed-boosted march with deadly footsteps
    // ================================================================
    public static class CrushingMarch extends BossAttack {
        private final List<BlockDisplayHandle> marchHandles = new ArrayList<>();
        private boolean marchActive = false;
        private int marchStartTick = 0;
        private int stepsPlaced = 0;

        public CrushingMarch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_crushing_march", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(12.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Rhythmic step buildup
            BlockDisplayHandle sentinelFoot = displayBuilder.spawnBlock(
                center.clone().add(0, 0, 0), Material.GOLD_BLOCK);
            sentinelFoot.scale(1.2f, 0.4f, 1.2f).glow(255, 200, 0).interpolation(2, 0);
            marchHandles.add(sentinelFoot);
            spawnedEntities.add(sentinelFoot.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Tempo buildup (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !marchActive) {
                if (ticksAlive % 15 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 6, 1.5);
                    DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.4f);
                }
            }
            // March activates (tick 60) — 160 ticks (8 seconds) of crushing steps
            else if (ticksAlive == 60 && !marchActive) {
                marchActive = true;
                marchStartTick = ticksAlive;
            }
            // Active march — footstep every 10 ticks (0.5 sec per step)
            else if (marchActive && ticksAlive - marchStartTick < 160) {
                int marchTick = ticksAlive - marchStartTick;
                if (marchTick % 10 == 0 && stepsPlaced < 16) {
                    stepsPlaced++;
                    float stepZ = stepsPlaced * 1.2f;
                    float stepX = (float) Math.sin(stepsPlaced * 0.5) * 2;

                    // Target circle appears 10 ticks before impact
                    Location stepLoc = center.clone().add(stepX, 0.05, stepZ);
                    BlockDisplayHandle target = displayBuilder.spawnBlock(stepLoc, Material.GOLD_BLOCK);
                    target.scale(1.0f, 0.08f, 1.0f).glow(255, 150, 0).interpolation(1, 0);
                    marchHandles.add(target);
                    spawnedEntities.add(target.entity());

                    // Impact at the location
                    BlockDisplayHandle impact = displayBuilder.spawnBlock(
                        stepLoc.clone().add(0, 0.1, 0), Material.GILDED_BLACKSTONE);
                    impact.scale(1.5f, 0.2f, 1.5f).glow(200, 160, 0).interpolation(1, 0);
                    marchHandles.add(impact);
                    spawnedEntities.add(impact.entity());

                    DisplayBuilder.cyanDust(stepLoc, 8, 2.0);
                    DisplayBuilder.playSound(stepLoc, Sound.ENTITY_IRON_GOLEM_STEP, 1.2f, 0.4f);
                    DisplayBuilder.playSound(stepLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                }
            }
            // March ends (tick 220)
            else if (marchActive && ticksAlive - marchStartTick == 160) {
                marchActive = false;
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 6, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrushingMarch(plugin); }
    }

    // ================================================================
    // 7. SANCTUM ANCHOR — Gold veins root players + beam combo setup
    // ================================================================
    public static class SanctumAnchor extends BossAttack {
        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();
        private boolean rootActive = false;

        public SanctumAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_sanctum_anchor", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Fists drive into ground — gold veins spread
            for (int ray = 0; ray < 8; ray++) {
                double angle = (Math.PI * 2 / 8) * ray;
                for (int seg = 1; seg <= 12; seg++) {
                    BlockDisplayHandle vein = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * seg, 0.03, Math.sin(angle) * seg),
                        Material.GOLD_BLOCK);
                    vein.scale(0.3f, 0.04f, 0.3f).glow(255, 200, 0).interpolation(3, 0);
                    anchorHandles.add(vein);
                    spawnedEntities.add(vein.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Veins spread (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !rootActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.1, 0), 10, 10.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
                }
            }
            // Root triggers (tick 40) — 80 ticks (4 seconds)
            else if (ticksAlive == 40 && !rootActive) {
                rootActive = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 20, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
            }
            // Root sustained (40-120 ticks)
            else if (rootActive && ticksAlive < 120) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 12.0);
                }
            }
            // Root releases (tick 120)
            else if (rootActive && ticksAlive == 120) {
                rootActive = false;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SanctumAnchor(plugin); }
    }

    // ================================================================
    // 8. BLOCK THROW — Rips gold block from self and hurls it
    // ================================================================
    public static class BlockThrow extends BossAttack {
        private final List<BlockDisplayHandle> throwHandles = new ArrayList<>();
        private boolean thrown = false;

        public BlockThrow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_block_throw", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Gold block ripped from shoulder, held aloft
            BlockDisplayHandle block = displayBuilder.spawnBlock(
                center.clone().add(1, 4, 0), Material.GOLD_BLOCK);
            block.scale(1.0f, 1.0f, 1.0f).glow(255, 200, 0).interpolation(3, 0);
            throwHandles.add(block);
            spawnedEntities.add(block.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Hold aloft with orbiting particles (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !thrown) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(1, 4, 0), 5, 1.0);
                }
            }
            // Throw (tick 30)
            else if (ticksAlive == 30 && !thrown) {
                thrown = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
            }
            // Block travels (30-50 ticks)
            else if (thrown && ticksAlive < 50) {
                float travel = (ticksAlive - 30) * 1.0f;
                float height = 4 - travel * 0.15f;
                if (height < 0.5f) height = 0.5f;
                throwHandles.get(0).entity().teleport(
                    center.clone().add(1, height, travel));
            }
            // Block impacts and embeds (tick 50)
            else if (thrown && ticksAlive == 50) {
                Location impactLoc = center.clone().add(1, 0.1, 20);
                // Embed in ground as obstacle
                BlockDisplayHandle embed = displayBuilder.spawnBlock(impactLoc, Material.GOLD_BLOCK);
                embed.scale(1.0f, 1.0f, 1.0f).glow(200, 160, 0).interpolation(2, 0);
                throwHandles.add(embed);
                spawnedEntities.add(embed.entity());

                DisplayBuilder.cyanDust(impactLoc, 15, 3.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlockThrow(plugin); }
    }

    // ================================================================
    // 9. GILDED STOMP COMBO — Triple stomp in triangle pattern
    // ================================================================
    public static class GildedStompCombo extends BossAttack {
        private final List<BlockDisplayHandle> comboHandles = new ArrayList<>();
        private int stompsCompleted = 0;

        public GildedStompCombo(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_gilded_stomp_combo", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No extra telegraph — fires as part of movement
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 stomps, 12 ticks apart, in triangle pattern
            if (stompsCompleted < 3 && ticksAlive == stompsCompleted * 12) {
                stompsCompleted++;
                // Triangle positions
                double[][] offsets = {{0, 0}, {2, 3}, {-2, 3}};
                int idx = stompsCompleted - 1;
                Location stompLoc = center.clone().add(offsets[idx][0], 0, offsets[idx][1]);

                // Impact crater
                BlockDisplayHandle crater = displayBuilder.spawnBlock(
                    stompLoc.clone().add(0, 0.05, 0), Material.CRACKED_DEEPSLATE_TILES);
                crater.scale(2.0f, 0.1f, 2.0f).glow(100, 80, 60).interpolation(1, 0);
                comboHandles.add(crater);
                spawnedEntities.add(crater.entity());

                // Gold impact ring
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(
                        stompLoc.clone().add(Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2),
                        Material.GOLD_BLOCK);
                    ring.scale(0.4f, 0.1f, 0.4f).glow(255, 200, 0).interpolation(1, 0);
                    comboHandles.add(ring);
                    spawnedEntities.add(ring.entity());
                }

                DisplayBuilder.cyanDust(stompLoc, 12, 2.0);
                float pitch = 0.5f + (stompsCompleted - 1) * 0.05f;
                DisplayBuilder.playSound(stompLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, pitch);
                DisplayBuilder.playSound(stompLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.6f);
            }
            // Recovery (36-66 ticks = 1.5 seconds)
            else if (stompsCompleted == 3 && ticksAlive < 66) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GildedStompCombo(plugin); }
    }

    // ================================================================
    // 10. GOLDEN FORTIFICATION — Self-buff: 40% damage reduction, no attacks
    // ================================================================
    public static class GoldenFortification extends BossAttack {
        private final List<BlockDisplayHandle> fortHandles = new ArrayList<>();
        private boolean fortActive = false;
        private int fortStartTick = 0;

        public GoldenFortification(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_golden_fortification", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(1100);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Body shimmers — gold surfaces gleam
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                BlockDisplayHandle shimmer = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(angle) * 2, 0.1, Math.sin(angle) * 2),
                    Material.GOLD_BLOCK);
                shimmer.scale(0.5f, 0.1f, 0.5f).glow(255, 220, 0).interpolation(3, 0);
                fortHandles.add(shimmer);
                spawnedEntities.add(shimmer.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Shimmer buildup (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !fortActive) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 8, 2.0);
                }
            }
            // Fortification activates (tick 30) — 120 ticks (6 seconds)
            else if (ticksAlive == 30 && !fortActive) {
                fortActive = true;
                fortStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.8f);
            }
            // Fortified state (30-150 ticks)
            else if (fortActive && ticksAlive - fortStartTick < 120) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 2.5);
                }
            }
            // Fortification ends (tick 150) — immediate follow-up attack
            else if (fortActive && ticksAlive - fortStartTick == 120) {
                fortActive = false;
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_USE, 1.2f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldenFortification(plugin); }
    }

    // ================================================================
    // 11. VOID CORRUPTION PUNCH — Void-infused fist (if Void Shard dead)
    // ================================================================
    public static class VoidCorruptionPunch extends BossAttack {
        private final List<BlockDisplayHandle> punchHandles = new ArrayList<>();
        private boolean punchFired = false;

        public VoidCorruptionPunch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_void_corruption_punch", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Right fist darkens — absorbs void energy
            BlockDisplayHandle fist = displayBuilder.spawnBlock(
                center.clone().add(1.5, 3, 0), Material.CRYING_OBSIDIAN);
            fist.scale(1.0f, 1.0f, 1.0f).glow(120, 0, 200).interpolation(3, 0);
            punchHandles.add(fist);
            spawnedEntities.add(fist.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Void absorption (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !punchFired) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(1.5, 3, 0), 6, 1.5);
                }
            }
            // Punch delivers (tick 40)
            else if (ticksAlive == 40 && !punchFired) {
                punchFired = true;
                // Impact point with void zone
                Location impactLoc = center.clone().add(0, 0, 3);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (dx * dx + dz * dz > 6) continue;
                        BlockDisplayHandle zone = displayBuilder.spawnBlock(
                            impactLoc.clone().add(dx, 0.03, dz), Material.CRYING_OBSIDIAN);
                        zone.scale(0.9f, 0.06f, 0.9f).glow(120, 0, 200).interpolation(2, 0);
                        punchHandles.add(zone);
                        spawnedEntities.add(zone.entity());
                    }
                }
                // Move fist to impact position
                punchHandles.get(0).entity().teleport(impactLoc.clone().add(0, 1, 0));

                DisplayBuilder.cyanDust(impactLoc, 20, 3.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.4f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);
            }
            // Fist returns to gold (40-100 ticks = 3 seconds)
            else if (punchFired && ticksAlive < 100) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 3), 4, 2.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCorruptionPunch(plugin); }
    }

    // ================================================================
    // 12. TOWER SHIELD BASH — Straight-line charge with knockback
    // ================================================================
    public static class TowerShieldBash extends BossAttack {
        private final List<BlockDisplayHandle> bashHandles = new ArrayList<>();
        private boolean bashed = false;

        public TowerShieldBash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_tower_shield_bash", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sentinel presents broadest profile
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            body.scale(2.5f, 3.5f, 1.0f).glow(255, 200, 0).interpolation(2, 0);
            bashHandles.add(body);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Turn to face (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !bashed) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 2.0);
                }
            }
            // Charge forward (tick 20)
            else if (ticksAlive == 20 && !bashed) {
                bashed = true;
                // Charge trail over 6 blocks
                for (int i = 0; i < 6; i++) {
                    Location trailLoc = center.clone().add(0, 0.1, i);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailLoc, Material.GOLD_BLOCK);
                    trail.scale(2.0f, 0.1f, 0.8f).glow(255, 180, 0).interpolation(1, 0);
                    bashHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                // Move sentinel body to end position
                bashHandles.get(0).entity().teleport(center.clone().add(0, 2, 6));

                DisplayBuilder.cyanDust(center.clone().add(0, 2, 3), 15, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 2.0f, 0.4f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TowerShieldBash(plugin); }
    }

    // ================================================================
    // 13. SANCTUM RENOVATION — Replaces arena floor + spawns 8 spike pillars
    // ================================================================
    public static class SanctumRenovation extends BossAttack {
        private final List<BlockDisplayHandle> renovHandles = new ArrayList<>();
        private boolean renovated = false;

        public SanctumRenovation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_sanctum_renovation", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Fists planted on ground — TOTEM floods floor
            DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 20, 16.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Floor flood (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !renovated) {
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 12, 16.0);
                }
            }
            // Renovation completes (tick 40) — pristine floor + spike arrays
            else if (ticksAlive == 40 && !renovated) {
                renovated = true;
                // Pristine floor tiles in a wide area
                for (int x = -8; x <= 8; x += 4) {
                    for (int z = -8; z <= 8; z += 4) {
                        BlockDisplayHandle tile = displayBuilder.spawnBlock(
                            center.clone().add(x, 0.02, z), Material.GILDED_BLACKSTONE);
                        tile.scale(3.5f, 0.03f, 3.5f).glow(200, 160, 0).interpolation(3, 0);
                        renovHandles.add(tile);
                        spawnedEntities.add(tile.entity());
                    }
                }
                // 8 new spike pillars at random positions
                for (int spike = 0; spike < 8; spike++) {
                    double ox = (Math.random() - 0.5) * 24;
                    double oz = (Math.random() - 0.5) * 24;
                    for (int y = 0; y < 2; y++) {
                        BlockDisplayHandle s = displayBuilder.spawnBlock(
                            center.clone().add(ox, y, oz), Material.GOLD_BLOCK);
                        s.scale(0.5f, 0.9f, 0.5f).glow(255, 200, 0).interpolation(2, 0);
                        spawnedEntities.add(s.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SanctumRenovation(plugin); }
    }

    // ================================================================
    // 14. EYE OF THE SENTINEL — AoE targeting scan + true sight
    // ================================================================
    public static class EyeOfTheSentinel extends BossAttack {
        private final List<BlockDisplayHandle> eyeHandles = new ArrayList<>();
        private boolean scanFired = false;

        public EyeOfTheSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_eye_of_the_sentinel", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Golden eyes flare
            for (int eye = 0; eye < 2; eye++) {
                float offX = eye == 0 ? -0.5f : 0.5f;
                BlockDisplayHandle eyeGlow = displayBuilder.spawnBlock(
                    center.clone().add(offX, 3.5, 0.5), Material.GOLD_BLOCK);
                eyeGlow.scale(0.3f, 0.3f, 0.3f).glow(255, 255, 0).interpolation(2, 0);
                eyeHandles.add(eyeGlow);
                spawnedEntities.add(eyeGlow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Targeting sequence (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !scanFired) {
                if (ticksAlive % 6 == 0) {
                    // Eye beams radiate outward
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI * 2 / 4) * i;
                        DisplayBuilder.cyanDust(
                            center.clone().add(Math.cos(angle) * 6, 3.5, Math.sin(angle) * 6), 3, 1.0);
                    }
                }
            }
            // Scan fires (tick 30) — all players scanned
            else if (ticksAlive == 30 && !scanFired) {
                scanFired = true;
                // Radial scan wave
                for (int ring = 0; ring < 4; ring++) {
                    float radius = 4 + ring * 4;
                    for (int i = 0; i < 8; i++) {
                        double angle = (Math.PI * 2 / 8) * i;
                        BlockDisplayHandle scan = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius),
                            Material.GOLD_BLOCK);
                        scan.scale(0.5f, 0.05f, 0.5f).glow(255, 255, 0).interpolation(1, 0);
                        eyeHandles.add(scan);
                        spawnedEntities.add(scan.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 1.2f);
            }
            // True sight active (30-330 ticks = 15 seconds)
            else if (scanFired && ticksAlive < 330) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3.5, 0), 4, 1.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeOfTheSentinel(plugin); }
    }

    // ================================================================
    // 15. PILLAR FORMATION — 4 arena-edge pillars constrict playable space
    // ================================================================
    public static class PillarFormation extends BossAttack {
        private final List<BlockDisplayHandle> pillarHandles = new ArrayList<>();
        private boolean pillarsRisen = false;

        public PillarFormation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_pillar_formation", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(600);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 4 perimeter points glow
            double[][] cardinals = {{0, 14}, {0, -14}, {14, 0}, {-14, 0}};
            for (double[] dir : cardinals) {
                BlockDisplayHandle glow = displayBuilder.spawnBlock(
                    center.clone().add(dir[0], 0.05, dir[1]), Material.GOLD_BLOCK);
                glow.scale(2.0f, 0.1f, 2.0f).glow(255, 200, 0).interpolation(3, 0);
                pillarHandles.add(glow);
                spawnedEntities.add(glow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_STEP, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Glow buildup (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pillarsRisen) {
                if (ticksAlive % 8 == 0) {
                    for (BlockDisplayHandle glow : pillarHandles) {
                        DisplayBuilder.cyanDust(glow.entity().getLocation(), 6, 2.0);
                    }
                }
            }
            // Pillars rise (tick 40)
            else if (ticksAlive == 40 && !pillarsRisen) {
                pillarsRisen = true;
                double[][] cardinals = {{0, 14}, {0, -14}, {14, 0}, {-14, 0}};
                for (double[] dir : cardinals) {
                    Location pillarBase = center.clone().add(dir[0], 0, dir[1]);
                    for (int y = 0; y < 5; y++) {
                        for (int w2 = -1; w2 <= 1; w2++) {
                            double perpX = dir[1] == 0 ? 0 : w2;
                            double perpZ = dir[0] == 0 ? 0 : w2;
                            BlockDisplayHandle block = displayBuilder.spawnBlock(
                                pillarBase.clone().add(perpX, y, perpZ), Material.PURPUR_BLOCK);
                            block.scale(0.9f, 0.9f, 0.9f).glow(180, 120, 200).interpolation(2, 0);
                            pillarHandles.add(block);
                            spawnedEntities.add(block.entity());
                        }
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 2.0f, 0.5f);
            }
            // Pillars persist (40-440 ticks = 20 seconds)
            else if (pillarsRisen && ticksAlive < 440) {
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2.5, 0), 6, 14.0);
                }
            }
            // Pillars crumble (tick 440)
            else if (pillarsRisen && ticksAlive == 440) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PillarFormation(plugin); }
    }

    // ================================================================
    // 16. RESONANT SHOCKWAVE — Massive 20-block arena-wide shockwave
    // ================================================================
    public static class ResonantShockwave extends BossAttack {
        private final List<BlockDisplayHandle> shockHandles = new ArrayList<>();
        private boolean shockFired = false;

        public ResonantShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_resonant_shockwave", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(900);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sentinel vibrates — model shakes
            BlockDisplayHandle core = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            core.scale(2.0f, 3.5f, 2.0f).glow(255, 200, 0).interpolation(2, 0);
            shockHandles.add(core);
            spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vibration buildup (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !shockFired) {
                // Shake effect
                float jitter = (float) Math.sin(ticksAlive * 1.2) * 0.2f;
                shockHandles.get(0).entity().teleport(
                    center.clone().add(jitter, 2, jitter));
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 10, 3.0);
                }
            }
            // Shockwave fires (tick 40) — expands to 20-block radius in 20 ticks
            else if (ticksAlive == 40 && !shockFired) {
                shockFired = true;
                // Ring of golden blocks expands
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 0.2, Math.sin(angle) * 3),
                        Material.GOLD_BLOCK);
                    wave.scale(1.0f, 0.3f, 1.0f).glow(255, 200, 0).interpolation(1, 0);
                    shockHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.25f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.7f);
            }
            // Wave expands (40-60 ticks = 1 second to full radius)
            else if (shockFired && ticksAlive < 60) {
                float radius = 3 + ((ticksAlive - 40) / 20.0f) * 17;
                for (int i = 1; i < shockHandles.size(); i++) {
                    int idx = i - 1;
                    double angle = (Math.PI * 2 / 20) * idx;
                    shockHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * radius, 0.2, Math.sin(angle) * radius));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, radius);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ResonantShockwave(plugin); }
    }

    // ================================================================
    // 17. GOLDEN FUNERAL — 6 gold block projectiles (post-50% HP only)
    // ================================================================
    public static class GoldenFuneral extends BossAttack {
        private final List<BlockDisplayHandle> funeralHandles = new ArrayList<>();
        private boolean fired = false;

        public GoldenFuneral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_golden_funeral", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Arms hurl outward — TOTEM particles spray from each surface
            BlockDisplayHandle leftArm = displayBuilder.spawnBlock(
                center.clone().add(-2, 3, 0), Material.GOLD_BLOCK);
            leftArm.scale(1.2f, 0.8f, 0.8f).glow(255, 200, 0).interpolation(2, 0);
            funeralHandles.add(leftArm);
            spawnedEntities.add(leftArm.entity());

            BlockDisplayHandle rightArm = displayBuilder.spawnBlock(
                center.clone().add(2, 3, 0), Material.GOLD_BLOCK);
            rightArm.scale(1.2f, 0.8f, 0.8f).glow(255, 200, 0).interpolation(2, 0);
            funeralHandles.add(rightArm);
            spawnedEntities.add(rightArm.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fast windup (0-16 ticks = 0.8 seconds)
            if (ticksAlive < 16 && !fired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 8, 2.0);
                }
            }
            // Fire 6 gold blocks (tick 16)
            else if (ticksAlive == 16 && !fired) {
                fired = true;
                // 3 from each arm in fan pattern
                for (int arm = 0; arm < 2; arm++) {
                    float armX = arm == 0 ? -2 : 2;
                    for (int proj = 0; proj < 3; proj++) {
                        double angle = (Math.PI / 3) * (proj - 1) + (arm == 0 ? Math.PI : 0);
                        BlockDisplayHandle block = displayBuilder.spawnBlock(
                            center.clone().add(armX + Math.cos(angle) * 3, 3, Math.sin(angle) * 3),
                            Material.GOLD_BLOCK);
                        block.scale(0.8f, 0.8f, 0.8f).glow(255, 200, 0).interpolation(1, 0);
                        funeralHandles.add(block);
                        spawnedEntities.add(block.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2.0f, 1.0f);
            }
            // Blocks travel outward (16-36 ticks)
            else if (fired && ticksAlive < 36) {
                float dist = (ticksAlive - 16) * 0.7f;
                for (int i = 2; i < funeralHandles.size(); i++) {
                    int projIdx = i - 2;
                    int arm = projIdx / 3;
                    int proj = projIdx % 3;
                    float armX = arm == 0 ? -2 : 2;
                    double angle = (Math.PI / 3) * (proj - 1) + (arm == 0 ? Math.PI : 0);
                    funeralHandles.get(i).entity().teleport(
                        center.clone().add(
                            armX + Math.cos(angle) * (3 + dist),
                            3 - dist * 0.1,
                            Math.sin(angle) * (3 + dist)));
                }
            }
            // Blocks detonate (tick 36)
            else if (fired && ticksAlive == 36) {
                for (int i = 2; i < funeralHandles.size(); i++) {
                    Location loc = funeralHandles.get(i).entity().getLocation();
                    DisplayBuilder.cyanDust(loc, 8, 3.0);
                    // Embed as obstacle
                    BlockDisplayHandle embed = displayBuilder.spawnBlock(
                        loc.clone().add(0, -loc.getY() + center.getY() + 0.1, 0),
                        Material.GOLD_BLOCK);
                    embed.scale(0.8f, 0.8f, 0.8f).glow(200, 160, 0).interpolation(2, 0);
                    spawnedEntities.add(embed.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GoldenFuneral(plugin); }
    }

    // ================================================================
    // 18. IRON WILL — 2-second full damage immunity (post-50% HP only)
    // ================================================================
    public static class IronWill extends BossAttack {
        private final List<BlockDisplayHandle> willHandles = new ArrayList<>();
        private boolean willActive = false;
        private int willStartTick = 0;

        public IronWill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_iron_will", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(8.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // TOTEM particles intensify from every joint
            BlockDisplayHandle glow = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            glow.scale(2.5f, 4.0f, 2.5f).glow(255, 255, 100).interpolation(2, 0);
            willHandles.add(glow);
            spawnedEntities.add(glow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.5f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Intensity buildup (0-20 ticks = 1 second)
            if (ticksAlive < 20 && !willActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 10, 3.0);
                }
            }
            // Will activates (tick 20) — 40 ticks (2 seconds) immunity
            else if (ticksAlive == 20 && !willActive) {
                willActive = true;
                willStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
            }
            // Immunity window (20-60 ticks)
            else if (willActive && ticksAlive - willStartTick < 40) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 12, 3.0);
                }
            }
            // Will ends (tick 60)
            else if (willActive && ticksAlive - willStartTick == 40) {
                willActive = false;
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_REPAIR, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IronWill(plugin); }
    }

    // ================================================================
    // 19. COLLAPSE PRELUDE — Warning burst at 5% HP threshold
    // ================================================================
    public static class CollapsePrelude extends BossAttack {
        private final List<BlockDisplayHandle> preludeHandles = new ArrayList<>();
        private boolean preludeFired = false;

        public CollapsePrelude(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_collapse_prelude", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sentinel shakes violently — golden light from above
            BlockDisplayHandle skyLight = displayBuilder.spawnBlock(
                center.clone().add(0, 15, 0), Material.GOLD_BLOCK);
            skyLight.scale(3.0f, 0.5f, 3.0f).glow(255, 255, 0).interpolation(4, 0);
            preludeHandles.add(skyLight);
            spawnedEntities.add(skyLight.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning period — TOTEM rain from above (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !preludeFired) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(
                        (Math.random() - 0.5) * 20, 12, (Math.random() - 0.5) * 20), 8, 3.0);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.8f, 0.7f);
                }
            }
            // Desperate radial burst (tick 60)
            else if (ticksAlive == 60 && !preludeFired) {
                preludeFired = true;
                // Golden light sphere expansion
                for (int ring = 0; ring < 3; ring++) {
                    float radius = 4 + ring * 4;
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        BlockDisplayHandle burst = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius),
                            Material.GOLD_BLOCK);
                        burst.scale(0.8f, 0.3f, 0.8f).glow(255, 200, 0).interpolation(1, 0);
                        preludeHandles.add(burst);
                        spawnedEntities.add(burst.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 40, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CollapsePrelude(plugin); }
    }

    // ================================================================
    // 20. THE COLLAPSE — Death sequence: body flies apart, 4 corner detonations
    // ================================================================
    public static class TheCollapse extends BossAttack {
        private final List<BlockDisplayHandle> collapseHandles = new ArrayList<>();
        private boolean collapsed = false;
        private boolean detonated = false;

        public TheCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sanctum_sentinel_the_collapse", AttackType.BOSS, 4), "sanctum_sentinel");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(9999);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sentinel stops and sways
            BlockDisplayHandle body = displayBuilder.spawnBlock(
                center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
            body.scale(2.0f, 3.5f, 2.0f).glow(255, 200, 0).interpolation(3, 0);
            collapseHandles.add(body);
            spawnedEntities.add(body.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_DEATH, 2.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sway and title: "...it remembers." (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !collapsed) {
                float sway = (float) Math.sin(ticksAlive * 0.15) * 0.3f;
                collapseHandles.get(0).entity().teleport(
                    center.clone().add(sway, 2, 0));
            }
            // Body flies apart (tick 40) — 4 blocks to 4 corners
            else if (ticksAlive == 40 && !collapsed) {
                collapsed = true;
                double[][] corners = {{12, 12}, {12, -12}, {-12, 12}, {-12, -12}};
                for (double[] corner : corners) {
                    BlockDisplayHandle block = displayBuilder.spawnBlock(
                        center.clone().add(0, 2, 0), Material.GOLD_BLOCK);
                    block.scale(1.5f, 1.5f, 1.5f).glow(255, 200, 0).interpolation(1, 0);
                    collapseHandles.add(block);
                    spawnedEntities.add(block.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 30, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
            }
            // Blocks fly to corners (40-64 ticks = 1.2 seconds)
            else if (collapsed && !detonated && ticksAlive < 64) {
                float progress = (ticksAlive - 40) / 24.0f;
                double[][] corners = {{12, 12}, {12, -12}, {-12, 12}, {-12, -12}};
                for (int i = 0; i < 4; i++) {
                    if (i + 1 < collapseHandles.size()) {
                        collapseHandles.get(i + 1).entity().teleport(
                            center.clone().add(
                                corners[i][0] * progress,
                                2 + progress * 3,
                                corners[i][1] * progress));
                    }
                }
            }
            // Pause at corners (tick 64-70 = 0.3 seconds)
            else if (collapsed && !detonated && ticksAlive == 64) {
                // Silence except deep resonant tone
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 1.5f, 0.3f);
            }
            // ALL FOUR DETONATE (tick 70)
            else if (collapsed && !detonated && ticksAlive == 70) {
                detonated = true;
                double[][] corners = {{12, 12}, {12, -12}, {-12, 12}, {-12, -12}};
                for (double[] corner : corners) {
                    Location cornerLoc = center.clone().add(corner[0], 5, corner[1]);
                    DisplayBuilder.cyanDust(cornerLoc, 25, 6.0);
                    // Explosion display at each corner
                    for (int i = 0; i < 6; i++) {
                        double angle = (Math.PI * 2 / 6) * i;
                        BlockDisplayHandle debris = displayBuilder.spawnBlock(
                            cornerLoc.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3),
                            Material.GOLD_BLOCK);
                        debris.scale(0.6f, 0.6f, 0.6f).glow(255, 200, 0).interpolation(1, 0);
                        spawnedEntities.add(debris.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.4f);
            }
            // Blackstone core sinks (tick 80-140 = 3 seconds)
            else if (detonated && ticksAlive == 80) {
                // Core remains at center
                BlockDisplayHandle core = displayBuilder.spawnBlock(
                    center.clone().add(0, 2, 0), Material.POLISHED_BLACKSTONE);
                core.scale(0.8f, 0.8f, 0.8f).glow(80, 0, 160).interpolation(5, 0);
                collapseHandles.add(core);
                spawnedEntities.add(core.entity());
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 10, 1.0);
            }
            else if (detonated && ticksAlive > 80 && ticksAlive < 140) {
                // Core slowly sinks
                int coreIdx = collapseHandles.size() - 1;
                if (coreIdx >= 0) {
                    float sinkY = 2 - ((ticksAlive - 80) / 60.0f) * 2.5f;
                    collapseHandles.get(coreIdx).entity().teleport(
                        center.clone().add(0, sinkY, 0));
                }
            }
            // Final note — Dragon immunity drops (tick 140)
            else if (detonated && ticksAlive == 140) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheCollapse(plugin); }
    }
}
