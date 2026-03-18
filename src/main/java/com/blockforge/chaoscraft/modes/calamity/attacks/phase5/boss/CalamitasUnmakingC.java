package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Supreme Calamitas — Phase 3: "The Unmaking" (49%-25% HP)
 * Attacks #121-130
 *
 * Beam overdrive + radial patterns. Escalating damage 20-28 hearts.
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasUnmakingC {

    private CalamitasUnmakingC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new DualSweepingBeams(plugin));
        registry.register(new TridentMinefield(plugin));
        registry.register(new BrimstoneNovaChain(plugin));
        registry.register(new HelixDescentStrike(plugin));
        registry.register(new VoidPrison(plugin));
        registry.register(new TridentRicochet(plugin));
        registry.register(new BrimstoneFountain(plugin));
        registry.register(new CrossfireBarrage(plugin));
        registry.register(new AerialDiveSequence(plugin));
        registry.register(new RadialBurnGrid(plugin));
    }

    // ================================================================
    // #121 — DUAL SWEEPING BEAMS
    // Two brimstone beams sweep in opposing arcs from aerial position
    // ================================================================
    public static class DualSweepingBeams extends BossAttack {
        private boolean beamsActive = false;
        private int beamStartTick = 0;

        public DualSweepingBeams(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_dual_sweeping_beams", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts per second
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location elevated = center.clone().add(0, 20, 0);
            DisplayBuilder.crimsonDust(elevated, 15, 2.0);
            DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location elevated = center.clone().add(0, 20, 0);

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(elevated, 10, 2.0);
                }
                return;
            }

            // Beams active (10-130 ticks = 6 seconds)
            if (ticksAlive == 10 && !beamsActive) {
                beamsActive = true;
                beamStartTick = ticksAlive;
            }

            if (beamsActive && ticksAlive - beamStartTick < 120) {
                int elapsed = ticksAlive - beamStartTick;
                double sweepAngle1 = (elapsed / 120.0) * Math.PI; // 0 to PI
                double sweepAngle2 = Math.PI + (elapsed / 120.0) * Math.PI; // PI to 2PI

                // Beam 1 — sweeps clockwise
                if (elapsed % 2 == 0) {
                    for (int d = 1; d <= 25; d += 2) {
                        Location beam1Loc = elevated.clone().add(
                            Math.cos(sweepAngle1) * d, -d * 0.6, Math.sin(sweepAngle1) * d);
                        DisplayBuilder.purpleDust(beam1Loc, 3, 0.3);
                        DisplayBuilder.purpleDust(beam1Loc, 2, 0.1);
                    }
                    // Beam 2 — sweeps counter-clockwise
                    for (int d = 1; d <= 25; d += 2) {
                        Location beam2Loc = elevated.clone().add(
                            Math.cos(sweepAngle2) * d, -d * 0.6, Math.sin(sweepAngle2) * d);
                        DisplayBuilder.crimsonDust(beam2Loc, 3, 0.3);
                        DisplayBuilder.purpleDust(beam2Loc, 2, 0.1);
                    }
                }

                // Beam sounds
                if (elapsed % 20 == 0) {
                    DisplayBuilder.playSound(elevated, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DualSweepingBeams(plugin); }
    }

    // ================================================================
    // #122 — TRIDENT MINEFIELD
    // 16 tridents embed in the floor, proximity-triggered detonation
    // ================================================================
    public static class TridentMinefield extends BossAttack {
        private final List<BlockDisplayHandle> mineHandles = new ArrayList<>();
        private boolean minesPlaced = false;

        public TridentMinefield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_minefield", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts per mine
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 15, 0), 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Place mines (tick 0-20, one every 1.25 ticks roughly)
            if (!minesPlaced && ticksAlive < 20) {
                if (ticksAlive % 1 == 0 && mineHandles.size() < 16) {
                    double x = (Math.random() - 0.5) * 30;
                    double z = (Math.random() - 0.5) * 30;
                    Location mineLoc = center.clone().add(x, 0.05, z);
                    BlockDisplayHandle mine = displayBuilder.spawnBlock(mineLoc, Material.PRISMARINE_BRICKS);
                    mine.scale(0.3f, 0.15f, 0.8f).glow(200, 0, 50).interpolation(2, 0);
                    mineHandles.add(mine);
                    spawnedEntities.add(mine.entity());
                    DisplayBuilder.playSound(mineLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.5f, 1.2f);
                }
                if (ticksAlive == 19) minesPlaced = true;
            }

            // Mines pulse and check proximity
            if (minesPlaced && ticksAlive % 10 == 0) {
                for (BlockDisplayHandle mine : mineHandles) {
                    if (mine.entity() == null || !mine.entity().isValid()) continue;
                    Location mineLoc = mine.entity().getLocation();
                    // Pulse glow
                    DisplayBuilder.crimsonDust(mineLoc, 3, 0.5);
                    // Proximity check — if a player is within 2 blocks, detonate
                    if (center.getWorld() != null) {
                        for (Player player : center.getWorld().getPlayers()) {
                            if (isExempt(player)) continue;
                            if (player.getLocation().distanceSquared(mineLoc) <= 4.0) {
                                // Detonate
                                triggerImpactDamage(mineLoc);
                                DisplayBuilder.crimsonDust(mineLoc, 2, 1.0);
                                DisplayBuilder.crimsonDust(mineLoc, 20, 3.0);
                                DisplayBuilder.playSound(mineLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.0f);
                                mine.entity().remove();
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentMinefield(plugin); }
    }

    // ================================================================
    // #123 — BRIMSTONE NOVA CHAIN
    // 3 sequential brimstone novas, each larger than the last
    // ================================================================
    public static class BrimstoneNovaChain extends BossAttack {
        private int novasFired = 0;

        public BrimstoneNovaChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_nova_chain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts per nova
            config.setDamageRadius(4.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 20, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Nova 1 at tick 10 (radius 4)
            if (ticksAlive == 10 && novasFired == 0) {
                novasFired++;
                fireNova(center, 4.0);
            }
            // Nova 2 at tick 40 (radius 7)
            else if (ticksAlive == 40 && novasFired == 1) {
                novasFired++;
                fireNova(center, 7.0);
            }
            // Nova 3 at tick 70 (radius 11)
            else if (ticksAlive == 70 && novasFired == 2) {
                novasFired++;
                fireNova(center, 11.0);
            }

            // Nova expanding ring animations
            if (ticksAlive > 10 && ticksAlive < 30) {
                animateNovaRing(center, (ticksAlive - 10) / 20.0 * 4.0);
            }
            if (ticksAlive > 40 && ticksAlive < 60) {
                animateNovaRing(center, (ticksAlive - 40) / 20.0 * 7.0);
            }
            if (ticksAlive > 70 && ticksAlive < 90) {
                animateNovaRing(center, (ticksAlive - 70) / 20.0 * 11.0);
            }
        }

        private void fireNova(Location center, double radius) {
            triggerImpactDamage(center);
            DisplayBuilder.crimsonDust(center, 1, radius / 2);
            DisplayBuilder.crimsonDust(center, 40, radius);
            DisplayBuilder.crimsonDust(center, 20, radius);
            DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
        }

        private void animateNovaRing(Location center, double radius) {
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI / 16) * i;
                Location ringLoc = center.clone().add(Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                DisplayBuilder.crimsonDust(ringLoc, 2, 0.3);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneNovaChain(plugin); }
    }

    // ================================================================
    // #124 — HELIX DESCENT STRIKE
    // Descending double helix of projectiles spiraling down from sky
    // ================================================================
    public static class HelixDescentStrike extends BossAttack {
        private final List<BlockDisplayHandle> helixHandles = new ArrayList<>();
        private boolean descending = false;
        private int descentTick = 0;

        public HelixDescentStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_helix_descent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            Location sky = center.clone().add(0, 40, 0);
            DisplayBuilder.purpleDust(sky, 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spawn helix projectiles (tick 10)
            if (ticksAlive == 10 && !descending) {
                descending = true;
                descentTick = ticksAlive;
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 8) * (i % 8);
                    double yOffset = 40 - (i * 2.5);
                    boolean isHelix1 = i < 8;
                    Location helixLoc = center.clone().add(
                        Math.cos(angle) * 3, yOffset, Math.sin(angle) * 3);
                    Material mat = isHelix1 ? Material.MAGMA_BLOCK : Material.PRISMARINE_BRICKS;
                    BlockDisplayHandle node = displayBuilder.spawnBlock(helixLoc, mat);
                    node.scale(0.5f, 0.5f, 0.5f).glow(isHelix1 ? 255 : 0, isHelix1 ? 100 : 150, isHelix1 ? 0 : 255)
                        .interpolation(2, 0);
                    helixHandles.add(node);
                    spawnedEntities.add(node.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);
            }

            // Helix descends (10-80 ticks)
            if (descending && ticksAlive - descentTick < 70) {
                int elapsed = ticksAlive - descentTick;
                float descent = elapsed * 0.6f;
                for (int i = 0; i < helixHandles.size(); i++) {
                    double angle = (2 * Math.PI / 8) * (i % 8) + elapsed * 0.15;
                    double baseY = 40 - (i * 2.5);
                    double currentY = baseY - descent;
                    double radius = 3 + (elapsed * 0.05);
                    Location flyLoc = center.clone().add(
                        Math.cos(angle) * radius, currentY, Math.sin(angle) * radius);
                    helixHandles.get(i).entity().teleport(flyLoc);
                    if (elapsed % 3 == 0) {
                        boolean isHelix1 = i < 8;
                        if (isHelix1) {
                            DisplayBuilder.crimsonDust(flyLoc, 2, 0.2);
                        } else {
                            DisplayBuilder.cyanDust(flyLoc, 2, 0.2);
                        }
                    }
                }
            }

            // Impact cascade (tick 80)
            if (descending && ticksAlive - descentTick == 70) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location impactLoc = center.clone().add(Math.cos(angle) * 5, 0.1, Math.sin(angle) * 5);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.crimsonDust(impactLoc, 2, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HelixDescentStrike(plugin); }
    }

    // ================================================================
    // #125 — VOID PRISON
    // Creates a shrinking box that forces players into a kill zone
    // ================================================================
    public static class VoidPrison extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean prisonActive = false;

        public VoidPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_void_prison", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts per wall contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 30, 12.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Build prison walls (tick 10)
            if (ticksAlive == 10 && !prisonActive) {
                prisonActive = true;
                float startSize = 12.0f;
                // 4 walls at cardinal positions
                Location[] wallPositions = {
                    center.clone().add(startSize, 0, 0),
                    center.clone().add(-startSize, 0, 0),
                    center.clone().add(0, 0, startSize),
                    center.clone().add(0, 0, -startSize)
                };
                for (int i = 0; i < 4; i++) {
                    boolean isNS = i >= 2;
                    BlockDisplayHandle wall = displayBuilder.spawnBlock(wallPositions[i], Material.RED_STAINED_GLASS);
                    wall.scale(isNS ? 24.0f : 0.2f, 5.0f, isNS ? 0.2f : 24.0f)
                        .glow(128, 0, 255).interpolation(5, 0);
                    wallHandles.add(wall);
                    spawnedEntities.add(wall.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.9f, 0.5f);
            }

            // Walls shrink inward (10-140 ticks = 6.5 seconds)
            if (prisonActive && ticksAlive > 10 && ticksAlive < 140) {
                float progress = (ticksAlive - 10) / 130.0f;
                float currentSize = 12.0f - (progress * 9.0f); // Shrinks from 12 to 3

                if (wallHandles.size() >= 4) {
                    wallHandles.get(0).entity().teleport(center.clone().add(currentSize, 0, 0));
                    wallHandles.get(1).entity().teleport(center.clone().add(-currentSize, 0, 0));
                    wallHandles.get(2).entity().teleport(center.clone().add(0, 0, currentSize));
                    wallHandles.get(3).entity().teleport(center.clone().add(0, 0, -currentSize));
                }

                // Wall particles
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.purpleDust(center, 15, currentSize);
                }
            }

            // Prison fully contracted — explosion (tick 140)
            if (prisonActive && ticksAlive == 140) {
                triggerImpactDamage(center);
                DisplayBuilder.crimsonDust(center, 2, 3.0);
                DisplayBuilder.crimsonDust(center, 60, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidPrison(plugin); }
    }

    // ================================================================
    // #126 — TRIDENT RICOCHET
    // Tridents that bounce off walls and change direction
    // ================================================================
    public static class TridentRicochet extends BossAttack {
        private final List<BlockDisplayHandle> ricochetHandles = new ArrayList<>();
        private final List<double[]> velocities = new ArrayList<>();
        private boolean launched = false;

        public TridentRicochet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_ricochet", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(180);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 8, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Launch 8 tridents (tick 10)
            if (ticksAlive == 10 && !launched) {
                launched = true;
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    Location origin = center.clone().add(0, 8, 0);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                    trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                    ricochetHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                    velocities.add(new double[]{ Math.cos(angle) * 1.5, -0.3, Math.sin(angle) * 1.5 });
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.9f);
            }

            // Animate with wall bouncing (10-120 ticks)
            if (launched && ticksAlive > 10 && ticksAlive < 120) {
                double arenaRadius = 20.0;
                for (int i = 0; i < ricochetHandles.size(); i++) {
                    BlockDisplayHandle handle = ricochetHandles.get(i);
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    double[] vel = velocities.get(i);

                    loc.add(vel[0], vel[1], vel[2]);

                    // Wall bounce check
                    double relX = loc.getX() - center.getX();
                    double relZ = loc.getZ() - center.getZ();
                    if (Math.abs(relX) > arenaRadius) {
                        vel[0] = -vel[0];
                        DisplayBuilder.crimsonDust(loc, 8, 0.5);
                        DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);
                    }
                    if (Math.abs(relZ) > arenaRadius) {
                        vel[2] = -vel[2];
                        DisplayBuilder.crimsonDust(loc, 8, 0.5);
                        DisplayBuilder.playSound(loc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);
                    }

                    // Floor bounce
                    if (loc.getY() <= center.getY() + 0.5) {
                        vel[1] = Math.abs(vel[1]) * 0.7;
                    }
                    vel[1] -= 0.05; // Gravity

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
        public AbstractAttack newInstance() { return new TridentRicochet(plugin); }
    }

    // ================================================================
    // #127 — BRIMSTONE FOUNTAIN
    // Central eruption that rains brimstone shards outward
    // ================================================================
    public static class BrimstoneFountain extends BossAttack {
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean erupted = false;

        public BrimstoneFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_fountain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); // 11 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(26.0); // 13 hearts
            config.setImpactRadius(2.5);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 30, 2.0);
            DisplayBuilder.crimsonDust(center, 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 15, 1.5);
                }
                return;
            }

            // Eruption (tick 10) — launch shards upward
            if (ticksAlive == 10 && !erupted) {
                erupted = true;
                // 20 shards launch from center upward
                for (int i = 0; i < 20; i++) {
                    double angle = (2 * Math.PI / 20) * i;
                    double speed = 1.5 + Math.random() * 1.0;
                    Location shardLoc = center.clone().add(0, 0.5, 0);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(shardLoc, Material.MAGMA_BLOCK);
                    shard.scale(0.3f, 0.3f, 0.3f).glow(255, 100, 0).interpolation(2, 0);
                    shardHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
                DisplayBuilder.crimsonDust(center, 1, 2.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }

            // Animate shards — arc upward then rain down (10-80 ticks)
            if (erupted && ticksAlive > 10 && ticksAlive < 80) {
                int elapsed = ticksAlive - 10;
                for (int i = 0; i < shardHandles.size(); i++) {
                    BlockDisplayHandle shard = shardHandles.get(i);
                    if (shard.entity() == null || !shard.entity().isValid()) continue;
                    double angle = (2 * Math.PI / 20) * i;
                    double speed = 1.5 + (i % 5) * 0.2;
                    double horizDist = elapsed * speed * 0.3;
                    double yVel = 2.5 - elapsed * 0.1; // Parabolic arc
                    double y = elapsed * 2.5 - 0.5 * 0.1 * elapsed * elapsed;
                    if (y < 0) y = 0;
                    Location shardLoc = center.clone().add(
                        Math.cos(angle) * horizDist, y, Math.sin(angle) * horizDist);
                    shard.entity().teleport(shardLoc);
                    if (elapsed % 3 == 0) {
                        DisplayBuilder.crimsonDust(shardLoc, 2, 0.2);
                        DisplayBuilder.crimsonDust(shardLoc, 2, 0.2);
                    }
                    // Ground impact
                    if (y <= 0.1 && elapsed > 20) {
                        triggerImpactDamage(shardLoc);
                        DisplayBuilder.crimsonDust(shardLoc, 5, 1.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneFountain(plugin); }
    }

    // ================================================================
    // #128 — CROSSFIRE BARRAGE
    // Tridents from 2 opposing directions creating a crossfire zone
    // ================================================================
    public static class CrossfireBarrage extends BossAttack {
        private final List<BlockDisplayHandle> crossHandles = new ArrayList<>();
        private boolean firing = false;
        private int fireTick = 0;
        private int tridentsFired = 0;

        public CrossfireBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crossfire_barrage", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(28.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — crimson lines from east and west
            DisplayBuilder.crimsonDust(center.clone().add(25, 3, 0), 15, 2.0);
            DisplayBuilder.crimsonDust(center.clone().add(-25, 3, 0), 15, 2.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                return;
            }

            // Begin firing (tick 10)
            if (ticksAlive == 10 && !firing) {
                firing = true;
                fireTick = ticksAlive;
            }

            // Fire alternating tridents from east and west every 4 ticks
            if (firing && (ticksAlive - fireTick) % 4 == 0 && tridentsFired < 20) {
                tridentsFired++;
                boolean fromEast = tridentsFired % 2 == 0;
                double xOrigin = fromEast ? 25 : -25;
                double zOffset = (Math.random() - 0.5) * 20;
                double yOffset = 2 + Math.random() * 4;
                Location origin = center.clone().add(xOrigin, yOffset, zOffset);
                BlockDisplayHandle trident = displayBuilder.spawnBlock(origin, Material.PRISMARINE_BRICKS);
                trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                crossHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(origin, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
            }

            // Animate tridents crossing the arena
            for (BlockDisplayHandle handle : crossHandles) {
                if (handle.entity() == null || !handle.entity().isValid()) continue;
                Location loc = handle.entity().getLocation();
                double relX = loc.getX() - center.getX();
                double direction = relX > 0 ? -2.0 : 2.0;
                loc.add(direction, -0.1, 0);
                handle.entity().teleport(loc);
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(loc, 2, 0.2);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrossfireBarrage(plugin); }
    }

    // ================================================================
    // #129 — AERIAL DIVE SEQUENCE
    // 3 rapid aerial dives with expanding shockwaves
    // ================================================================
    public static class AerialDiveSequence extends BossAttack {
        private int divesCompleted = 0;

        public AerialDiveSequence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_aerial_dive_sequence", AttackType.BOSS, 5), "calamitas");
            config.setDamage(30.0); // 15 hearts per dive
            config.setDamageRadius(4.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(30.0);
            config.setImpactRadius(4.0);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 20, 3.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dive 1 (tick 20)
            if (ticksAlive == 20 && divesCompleted == 0) {
                divesCompleted++;
                executeDive(center, 0, 0);
            }
            // Dive 2 (tick 60)
            else if (ticksAlive == 60 && divesCompleted == 1) {
                divesCompleted++;
                executeDive(center, 8, 5);
            }
            // Dive 3 (tick 100)
            else if (ticksAlive == 100 && divesCompleted == 2) {
                divesCompleted++;
                executeDive(center, -6, -7);
            }

            // Descent trail animation
            if (ticksAlive >= 15 && ticksAlive < 20) {
                float yPos = 30 - (ticksAlive - 15) * 6.0f;
                DisplayBuilder.crimsonDust(center.clone().add(0, yPos, 0), 15, 1.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, yPos, 0), 5, 0.5);
            }
            if (ticksAlive >= 55 && ticksAlive < 60) {
                float yPos = 30 - (ticksAlive - 55) * 6.0f;
                DisplayBuilder.crimsonDust(center.clone().add(8, yPos, 5), 15, 1.0);
            }
            if (ticksAlive >= 95 && ticksAlive < 100) {
                float yPos = 30 - (ticksAlive - 95) * 6.0f;
                DisplayBuilder.crimsonDust(center.clone().add(-6, yPos, -7), 15, 1.0);
            }
        }

        private void executeDive(Location center, double xOffset, double zOffset) {
            Location impactLoc = center.clone().add(xOffset, 0.1, zOffset);
            triggerImpactDamage(impactLoc);
            DisplayBuilder.crimsonDust(impactLoc, 1, 2.0);
            DisplayBuilder.crimsonDust(impactLoc, 40, 4.0);
            DisplayBuilder.crimsonDust(impactLoc, 50, 4.0);
            DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
            DisplayBuilder.playSound(impactLoc, Sound.ENTITY_RAVAGER_ROAR, 0.8f, 0.5f);

            // Shockwave ring
            BlockDisplayHandle crater = displayBuilder.spawnBlock(
                impactLoc.clone().add(0, 0.02, 0), Material.CRACKED_STONE_BRICKS);
            crater.scale(4.0f, 0.04f, 4.0f).glow(200, 0, 50).interpolation(3, 0);
            spawnedEntities.add(crater.entity());
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AerialDiveSequence(plugin); }
    }

    // ================================================================
    // #130 — RADIAL BURN GRID
    // Concentric rings of fire expanding from center
    // ================================================================
    public static class RadialBurnGrid extends BossAttack {
        private final List<BlockDisplayHandle> gridHandles = new ArrayList<>();
        private int ringsPlaced = 0;

        public RadialBurnGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_radial_burn_grid", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); // 10 hearts per second
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.crimsonDust(center, 25, 2.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Place concentric rings every 20 ticks (1 second each)
            if (ticksAlive >= 10 && (ticksAlive - 10) % 20 == 0 && ringsPlaced < 5) {
                ringsPlaced++;
                double radius = ringsPlaced * 4.0;
                // Create ring of fire at this radius
                int segments = (int)(radius * 3);
                for (int i = 0; i < segments; i++) {
                    double angle = (2 * Math.PI / segments) * i;
                    Location ringLoc = center.clone().add(
                        Math.cos(angle) * radius, 0.03, Math.sin(angle) * radius);
                    BlockDisplayHandle fire = displayBuilder.spawnBlock(ringLoc, Material.MAGMA_BLOCK);
                    fire.scale(1.2f, 0.04f, 1.2f).glow(255, 100, 0).interpolation(3, 0);
                    gridHandles.add(fire);
                    spawnedEntities.add(fire.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 0.8f + ringsPlaced * 0.1f);
            }

            // Ring fire particles
            if (ringsPlaced > 0 && ticksAlive % 8 == 0) {
                for (int ring = 1; ring <= ringsPlaced; ring++) {
                    double radius = ring * 4.0;
                    for (int i = 0; i < 4; i++) {
                        double angle = Math.random() * 2 * Math.PI;
                        Location fireLoc = center.clone().add(
                            Math.cos(angle) * radius, 0.5, Math.sin(angle) * radius);
                        DisplayBuilder.crimsonDust(fireLoc, 3, 0.5);
                        DisplayBuilder.crimsonDust(fireLoc, 2, 0.3);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RadialBurnGrid(plugin); }
    }
}
