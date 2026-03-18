package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 3: "The Unmaking" (49%-25% HP)
 * Attacks #131-140
 *
 * Escalating trident storms, aerial combos, and psychological attacks.
 * Damage: 22-30 hearts. Telegraph: 0.5s or NONE.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasUnmakingD {

    private CalamitasUnmakingD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TridentLockdown(plugin));
        registry.register(new BrimstoneGeysers(plugin));
        registry.register(new ArenaContraction(plugin));
        registry.register(new SplitPersona(plugin));
        registry.register(new CrimsonWavefront(plugin));
        registry.register(new HomingTridentSwarm(plugin));
        registry.register(new SkyStreamDescent(plugin));
        registry.register(new ReversalBurst(plugin));
        registry.register(new PhantomTrident(plugin));
        registry.register(new BrimstoneTentacleSweep(plugin));
    }

    // ================================================================
    // #131 — TRIDENT LOCKDOWN
    // 4 trident walls from all sides creating a shrinking box
    // ================================================================
    public static class TridentLockdown extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean activated = false;
        private int activateTick = 0;

        public TridentLockdown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_lockdown", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 20.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.9f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !activated) {
                activated = true;
                activateTick = ticksAlive;
                // 4 walls of tridents, one per cardinal side
                double[][] directions = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
                for (double[] dir : directions) {
                    for (int j = 0; j < 5; j++) {
                        double perpX = -dir[1];
                        double perpZ = dir[0];
                        Location tLoc = center.clone().add(
                            dir[0] * 20 + perpX * (j - 2) * 3, 3, dir[1] * 20 + perpZ * (j - 2) * 3);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        wallHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);
            }

            // Walls close inward (10-80 ticks)
            if (activated && ticksAlive - activateTick < 70) {
                float progress = (ticksAlive - activateTick) / 70.0f;
                double currentDist = 20.0 * (1.0 - progress * 0.85); // Close to ~3 blocks
                int idx = 0;
                double[][] directions = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
                for (double[] dir : directions) {
                    for (int j = 0; j < 5; j++) {
                        double perpX = -dir[1];
                        double perpZ = dir[0];
                        if (idx < wallHandles.size()) {
                            Location newLoc = center.clone().add(
                                dir[0] * currentDist + perpX * (j - 2) * 3,
                                3 - progress * 2,
                                dir[1] * currentDist + perpZ * (j - 2) * 3);
                            wallHandles.get(idx).entity().teleport(newLoc);
                        }
                        idx++;
                    }
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, (float) currentDist);
                }
            }

            // Convergence (tick 80)
            if (activated && ticksAlive - activateTick == 70) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentLockdown(plugin); }
    }

    // ================================================================
    // #132 — BRIMSTONE GEYSERS
    // 6 geyser eruptions at random positions, sequential detonation
    // ================================================================
    public static class BrimstoneGeysers extends BossAttack {
        private int geysersFired = 0;
        private final List<BlockDisplayHandle> geyserHandles = new ArrayList<>();

        public BrimstoneGeysers(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_geysers", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts
            config.setDamageRadius(3.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26.0);
            config.setImpactRadius(3.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 15, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fire geyser every 15 ticks (6 total)
            if (ticksAlive >= 10 && (ticksAlive - 10) % 15 == 0 && geysersFired < 6) {
                geysersFired++;
                double x = (Math.random() - 0.5) * 30;
                double z = (Math.random() - 0.5) * 30;
                Location geyserLoc = center.clone().add(x, 0.1, z);

                // Floor crack telegraph
                DisplayBuilder.crimsonDust(geyserLoc, 15, 2.0);

                // Geyser column
                BlockDisplayHandle pillar = displayBuilder.spawnBlock(geyserLoc, Material.MAGMA_BLOCK);
                pillar.scale(1.5f, 0.1f, 1.5f).glow(255, 100, 0).interpolation(2, 0);
                geyserHandles.add(pillar);
                spawnedEntities.add(pillar.entity());
            }

            // Animate geysers erupting upward
            for (int i = 0; i < geyserHandles.size(); i++) {
                BlockDisplayHandle geyser = geyserHandles.get(i);
                if (geyser.entity() == null || !geyser.entity().isValid()) continue;
                int geyserAge = ticksAlive - (10 + i * 15);
                if (geyserAge >= 5 && geyserAge < 15) {
                    // Eruption phase
                    float height = (geyserAge - 5) * 0.6f;
                    Location loc = geyser.entity().getLocation();
                    loc.add(0, 0.6, 0);
                    geyser.entity().teleport(loc);
                    DisplayBuilder.crimsonDust(loc, 8, 1.0);
                    DisplayBuilder.crimsonDust(loc, 5, 0.5);
                    if (geyserAge == 5) {
                        triggerImpactDamage(loc);
                        DisplayBuilder.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.7f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneGeysers(plugin); }
    }

    // ================================================================
    // #133 — ARENA CONTRACTION
    // Damaging walls close in from all sides temporarily
    // ================================================================
    public static class ArenaContraction extends BossAttack {
        private final List<BlockDisplayHandle> contractionWalls = new ArrayList<>();
        private boolean contracting = false;

        public ArenaContraction(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_arena_contraction", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts wall contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 30, 20.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.7f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !contracting) {
                contracting = true;
                // 4 wall panels
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location wallLoc = center.clone().add(Math.cos(angle) * 25, 0, Math.sin(angle) * 25);
                    boolean isNS = i % 2 == 0;
                    BlockDisplayHandle wall = displayBuilder.spawnBlock(wallLoc, Material.RED_STAINED_GLASS);
                    wall.scale(isNS ? 0.3f : 50.0f, 8.0f, isNS ? 50.0f : 0.3f)
                        .glow(200, 0, 50).interpolation(5, 0);
                    contractionWalls.add(wall);
                    spawnedEntities.add(wall.entity());
                }
            }

            // Contract (10-110 ticks = 5 seconds)
            if (contracting && ticksAlive > 10 && ticksAlive < 110) {
                float progress = (ticksAlive - 10) / 100.0f;
                double currentDist = 25.0 - (progress * 17.0); // 25 -> 8
                for (int i = 0; i < contractionWalls.size(); i++) {
                    double angle = (Math.PI / 2) * i;
                    Location newLoc = center.clone().add(Math.cos(angle) * currentDist, 0, Math.sin(angle) * currentDist);
                    contractionWalls.get(i).entity().teleport(newLoc);
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, (float) currentDist);
                }
            }

            // Hold at minimum (110-140 ticks)
            // Expand back (140-160 ticks)
            if (contracting && ticksAlive >= 140 && ticksAlive < 160) {
                float progress = (ticksAlive - 140) / 20.0f;
                double currentDist = 8.0 + (progress * 17.0);
                for (int i = 0; i < contractionWalls.size(); i++) {
                    double angle = (Math.PI / 2) * i;
                    Location newLoc = center.clone().add(Math.cos(angle) * currentDist, 0, Math.sin(angle) * currentDist);
                    contractionWalls.get(i).entity().teleport(newLoc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ArenaContraction(plugin); }
    }

    // ================================================================
    // #134 — SPLIT PERSONA
    // Creates 3 afterimage copies that each fire trident volleys
    // ================================================================
    public static class SplitPersona extends BossAttack {
        private final List<BlockDisplayHandle> cloneHandles = new ArrayList<>();
        private boolean split = false;

        public SplitPersona(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_split_persona", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !split) {
                split = true;
                // 3 afterimage positions
                double[] offsets = { -10, 0, 10 };
                for (double offset : offsets) {
                    Location cloneLoc = center.clone().add(offset, 8, 0);
                    BlockDisplayHandle clone = displayBuilder.spawnBlock(cloneLoc, Material.CRIMSON_HYPHAE);
                    clone.scale(1.0f, 2.0f, 0.5f).glow(200, 0, 50).interpolation(3, 0);
                    cloneHandles.add(clone);
                    spawnedEntities.add(clone.entity());
                    DisplayBuilder.crimsonDust(cloneLoc, 3, 1.0);
                }
            }

            // Each clone fires 5 tridents in 60-degree arc (tick 30, 50, 70)
            if (split) {
                int[] fireTicks = { 30, 50, 70 };
                for (int f = 0; f < fireTicks.length; f++) {
                    if (ticksAlive == fireTicks[f] && f < cloneHandles.size()) {
                        Location cloneLoc = cloneHandles.get(f).entity().getLocation();
                        for (int j = 0; j < 5; j++) {
                            double angle = -Math.PI / 6 + (Math.PI / 3) * (j / 4.0);
                            Location tridentLoc = cloneLoc.clone().add(
                                Math.cos(angle) * 2, -1, Math.sin(angle) * 2);
                            DisplayBuilder.crimsonDust(tridentLoc, 8, 0.5);
                            DisplayBuilder.purpleDust(tridentLoc, 4, 0.3);
                        }
                        DisplayBuilder.playSound(cloneLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
                    }
                }

                // Afterimage particles
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle clone : cloneHandles) {
                        if (clone.entity() != null && clone.entity().isValid()) {
                            DisplayBuilder.crimsonDust(clone.entity().getLocation(), 10, 1.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SplitPersona(plugin); }
    }

    // ================================================================
    // #135 — CRIMSON WAVEFRONT
    // Expanding wave of crimson energy on the floor
    // ================================================================
    public static class CrimsonWavefront extends BossAttack {
        private boolean waveActive = false;
        private int waveTick = 0;

        public CrimsonWavefront(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_wavefront", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !waveActive) {
                waveActive = true;
                waveTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.6f);
            }

            // Expanding wave (10-80 ticks)
            if (waveActive && ticksAlive - waveTick < 70) {
                float radius = (ticksAlive - waveTick) * 0.5f;
                if (ticksAlive % 2 == 0) {
                    int segments = Math.max(8, (int)(radius * 4));
                    for (int i = 0; i < segments; i++) {
                        double angle = (2 * Math.PI / segments) * i;
                        Location waveLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.3, Math.sin(angle) * radius);
                        DisplayBuilder.crimsonDust(waveLoc, 3, 0.5);
                        DisplayBuilder.crimsonDust(waveLoc, 1, 0.2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonWavefront(plugin); }
    }

    // ================================================================
    // #136 — HOMING TRIDENT SWARM
    // 10 tridents that loosely track player positions
    // ================================================================
    public static class HomingTridentSwarm extends BossAttack {
        private final List<BlockDisplayHandle> swarmHandles = new ArrayList<>();
        private boolean launched = false;

        public HomingTridentSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_homing_swarm", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(5);
            config.setTracksPlayer(true);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 15, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !launched) {
                launched = true;
                Location elevated = center.clone().add(0, 15, 0);
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI / 10) * i;
                    Location tLoc = elevated.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    swarmHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.9f);
            }

            // Homing movement toward target
            if (launched && ticksAlive > 10 && ticksAlive < 120) {
                for (BlockDisplayHandle handle : swarmHandles) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    // Loose homing — move toward target with randomness
                    Vector dir = center.toVector().subtract(loc.toVector()).normalize();
                    dir.add(new Vector(
                        (Math.random() - 0.5) * 0.5,
                        (Math.random() - 0.5) * 0.3,
                        (Math.random() - 0.5) * 0.5));
                    dir.normalize().multiply(1.2);
                    loc.add(dir);
                    handle.entity().teleport(loc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 2, 0.2);
                    }
                    // Close enough to target — impact
                    if (loc.distanceSquared(center) < 4) {
                        triggerImpactDamage(loc);
                        DisplayBuilder.crimsonDust(loc, 10, 0.5);
                        DisplayBuilder.playSound(loc, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HomingTridentSwarm(plugin); }
    }

    // ================================================================
    // #137 — SKY STREAM DESCENT
    // Sky streams drop to low altitude, damaging players at head height
    // ================================================================
    public static class SkyStreamDescent extends BossAttack {
        private final List<BlockDisplayHandle> streamHandles = new ArrayList<>();
        private boolean descended = false;

        public SkyStreamDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_sky_stream_descent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts per second
            config.setDamageRadius(2.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !descended) {
                descended = true;
                // 5 streams running east-west at different Z positions
                for (int i = 0; i < 5; i++) {
                    double zOffset = (i - 2) * 6.0;
                    for (int seg = -12; seg <= 12; seg += 3) {
                        Location streamLoc = center.clone().add(seg, 20, zOffset);
                        BlockDisplayHandle stream = displayBuilder.spawnBlock(streamLoc, Material.RED_STAINED_GLASS);
                        stream.scale(3.0f, 0.3f, 1.5f).glow(200, 0, 50).interpolation(5, 0);
                        streamHandles.add(stream);
                        spawnedEntities.add(stream.entity());
                    }
                }
            }

            // Descend streams from Y+20 to Y+6 (10-70 ticks = 3 seconds)
            if (descended && ticksAlive > 10 && ticksAlive < 70) {
                float progress = (ticksAlive - 10) / 60.0f;
                double currentY = 20.0 - (progress * 14.0); // 20 -> 6
                for (BlockDisplayHandle stream : streamHandles) {
                    if (stream.entity() == null || !stream.entity().isValid()) continue;
                    Location loc = stream.entity().getLocation();
                    loc.setY(center.getY() + currentY);
                    stream.entity().teleport(loc);
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f,
                        (float)(1.0 - progress * 0.6)); // Pitch drops as streams descend
                }
            }

            // Hold at Y+6 (70-230 ticks = 8 seconds)
            if (descended && ticksAlive >= 70 && ticksAlive < 230) {
                if (ticksAlive % 8 == 0) {
                    for (int i = 0; i < 5; i++) {
                        double zOffset = (i - 2) * 6.0;
                        double xRand = (Math.random() - 0.5) * 24;
                        Location particleLoc = center.clone().add(xRand, 6.5, zOffset);
                        DisplayBuilder.crimsonDust(particleLoc, 3, 1.0);
                        DisplayBuilder.crimsonDust(particleLoc, 2, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyStreamDescent(plugin); }
    }

    // ================================================================
    // #138 — REVERSAL BURST
    // All embedded tridents in arena relaunch toward nearest players
    // ================================================================
    public static class ReversalBurst extends BossAttack {
        private final List<BlockDisplayHandle> reversalHandles = new ArrayList<>();
        private boolean reversed = false;

        public ReversalBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_reversal_burst", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 30, 15.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pull-up animation (tick 10) — simulated embedded tridents rise
            if (ticksAlive == 10 && !reversed) {
                reversed = true;
                // Spawn 12 "reanimated" tridents at floor positions
                for (int i = 0; i < 12; i++) {
                    double x = (Math.random() - 0.5) * 30;
                    double z = (Math.random() - 0.5) * 30;
                    Location tridentLoc = center.clone().add(x, 0.3, z);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tridentLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(255, 100, 0).interpolation(2, 0);
                    reversalHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.6f);
            }

            // Tridents rise (10-25 ticks)
            if (reversed && ticksAlive > 10 && ticksAlive < 25) {
                for (BlockDisplayHandle handle : reversalHandles) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    loc.add(0, 0.5, 0);
                    handle.entity().teleport(loc);
                    DisplayBuilder.purpleDust(loc, 2, 0.2);
                }
            }

            // Tridents fire at players (25-60 ticks)
            if (reversed && ticksAlive >= 25 && ticksAlive < 60) {
                for (BlockDisplayHandle handle : reversalHandles) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    Vector dir = center.toVector().subtract(loc.toVector()).normalize();
                    loc.add(dir.multiply(2.0));
                    handle.entity().teleport(loc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 2, 0.2);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ReversalBurst(plugin); }
    }

    // ================================================================
    // #139 — PHANTOM TRIDENT
    // 4 tridents thrown simultaneously — 3 phantom (fake), 1 real
    // ================================================================
    public static class PhantomTrident extends BossAttack {
        private final List<BlockDisplayHandle> phantomHandles = new ArrayList<>();
        private int realTridentIndex = 0;
        private boolean launched = false;

        public PhantomTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_phantom_trident", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(120);
            config.setCooldownTicks(100);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // No telegraph
            realTridentIndex = (int)(Math.random() * 4);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !launched) {
                launched = true;
                Location elevated = center.clone().add(0, 10, 0);
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI / 2) * i;
                    Location tLoc = elevated.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(tLoc, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    phantomHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    DisplayBuilder.playSound(tLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
                }
            }

            // All 4 fly toward target area (0-40 ticks)
            if (launched && ticksAlive < 40) {
                for (int i = 0; i < phantomHandles.size(); i++) {
                    BlockDisplayHandle handle = phantomHandles.get(i);
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    Vector dir = center.toVector().subtract(loc.toVector()).normalize();
                    loc.add(dir.multiply(1.5));
                    handle.entity().teleport(loc);
                    // All tridents have identical particles — no visual tell
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 3, 0.1);
                        // Real trident has barely perceptible extra dust
                        if (i == realTridentIndex && ticksAlive % 6 == 0) {
                            DisplayBuilder.crimsonDust(loc, 1, 0.1);
                        }
                    }
                }
            }

            // Impact (tick 40) — only real trident deals damage
            if (launched && ticksAlive == 40) {
                for (int i = 0; i < phantomHandles.size(); i++) {
                    Location impactLoc = phantomHandles.get(i).entity().getLocation();
                    if (i == realTridentIndex) {
                        triggerImpactDamage(impactLoc);
                        DisplayBuilder.crimsonDust(impactLoc, 15, 0.5);
                        DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
                    } else {
                        // Phantom dissolves
                        DisplayBuilder.crimsonDust(impactLoc, 5, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomTrident(plugin); }
    }

    // ================================================================
    // #140 — BRIMSTONE TENTACLE SWEEP
    // 3 brimstone tentacles rise and sweep across the arena
    // ================================================================
    public static class BrimstoneTentacleSweep extends BossAttack {
        private final List<BlockDisplayHandle> tentacleHandles = new ArrayList<>();
        private boolean tentaclesActive = false;

        public BrimstoneTentacleSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_tentacle_sweep", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); // 11 hearts per tentacle
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(220);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — 3 columns of purple from floor
            for (int i = 0; i < 3; i++) {
                double x = (Math.random() - 0.5) * 20;
                double z = (Math.random() - 0.5) * 20;
                Location colLoc = center.clone().add(x, 0, z);
                DisplayBuilder.purpleDust(colLoc, 15, 1.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !tentaclesActive) {
                tentaclesActive = true;
                // 3 tentacles at random positions
                for (int i = 0; i < 3; i++) {
                    double x = (Math.random() - 0.5) * 20;
                    double z = (Math.random() - 0.5) * 20;
                    Location tentacleLoc = center.clone().add(x, 0.1, z);
                    // Tentacle column
                    for (int y = 0; y < 5; y++) {
                        BlockDisplayHandle seg = displayBuilder.spawnBlock(
                            tentacleLoc.clone().add(0, y, 0), Material.CRIMSON_HYPHAE);
                        seg.scale(1.0f, 1.0f, 1.0f).glow(128, 0, 255).interpolation(3, 0);
                        tentacleHandles.add(seg);
                        spawnedEntities.add(seg.entity());
                    }
                    DisplayBuilder.playSound(tentacleLoc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.6f);
                    DisplayBuilder.crimsonDust(tentacleLoc, 20, 1.0);
                }
            }

            // Tentacles sweep across arena (10-130 ticks = 6 seconds)
            if (tentaclesActive && ticksAlive > 10 && ticksAlive < 130) {
                int elapsed = ticksAlive - 10;
                double sweepSpeed = 0.3; // blocks per tick
                double[] sweepDirs = { 1.0, -0.5, 0.7 }; // Different directions
                for (int i = 0; i < Math.min(3, tentacleHandles.size() / 5); i++) {
                    double sweepX = sweepDirs[i % 3] * sweepSpeed;
                    double sweepZ = sweepDirs[(i + 1) % 3] * sweepSpeed * 0.8;
                    for (int y = 0; y < 5; y++) {
                        int idx = i * 5 + y;
                        if (idx < tentacleHandles.size()) {
                            BlockDisplayHandle seg = tentacleHandles.get(idx);
                            if (seg.entity() == null || !seg.entity().isValid()) continue;
                            Location loc = seg.entity().getLocation();
                            loc.add(sweepX, 0, sweepZ);
                            seg.entity().teleport(loc);
                        }
                    }
                }

                // Tentacle particles
                if (elapsed % 5 == 0) {
                    for (int i = 0; i < Math.min(3, tentacleHandles.size() / 5); i++) {
                        int baseIdx = i * 5;
                        if (baseIdx < tentacleHandles.size() && tentacleHandles.get(baseIdx).entity() != null) {
                            Location tentLoc = tentacleHandles.get(baseIdx).entity().getLocation();
                            DisplayBuilder.purpleDust(tentLoc, 10, 0.5);
                            DisplayBuilder.crimsonDust(tentLoc, 5, 0.3);
                        }
                    }
                }
            }

            // Aerial trident targeting during tentacle phase
            if (tentaclesActive && ticksAlive % 25 == 0 && ticksAlive > 20 && ticksAlive < 130) {
                DisplayBuilder.playSound(center.clone().add(0, 12, 0), Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneTentacleSweep(plugin); }
    }
}
