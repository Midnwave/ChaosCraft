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
 * Attacks #141-150
 *
 * Full arena coverage, beam overdrive, and Phase 3 terminus.
 * Damage: 24-36 hearts (escalating toward phase end).
 * Calamitas colors: crimson(200,0,50), orange(255,100,0), soul blue(0,150,255), purple(128,0,255)
 *
 * NO status effects — damage only.
 */
public final class CalamitasUnmakingE {

    private CalamitasUnmakingE() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Stormcall(plugin));
        registry.register(new BrimstoneSigil(plugin));
        registry.register(new TriDirectionTridentSweep(plugin));
        registry.register(new Shatter(plugin));
        registry.register(new ScreamingPursuit(plugin));
        registry.register(new HorizonBeam(plugin));
        registry.register(new BrimstoneVortex(plugin));
        registry.register(new CrimsonCrucible(plugin));
        registry.register(new AbsoluteZeroWindows(plugin));
        registry.register(new PhaseTerminusRuin(plugin));
    }

    // ================================================================
    // #141 — STORMCALL
    // 30 tridents in a uniform 6x5 grid saturate the entire arena
    // ================================================================
    public static class Stormcall extends BossAttack {
        private final List<BlockDisplayHandle> stormHandles = new ArrayList<>();
        private boolean dropped = false;

        public Stormcall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_stormcall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts
            config.setDamageRadius(2.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(24.0);
            config.setImpactRadius(2.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — all sky streams surge
            DisplayBuilder.crimsonDust(center.clone().add(0, 30, 0), 30, 10.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph flash (tick 10)
            if (ticksAlive == 10 && !dropped) {
                dropped = true;
                // 6x5 grid = 30 tridents
                for (int gx = 0; gx < 6; gx++) {
                    for (int gz = 0; gz < 5; gz++) {
                        double x = (gx - 2.5) * 6.0;
                        double z = (gz - 2.0) * 6.0;
                        Location dropLoc = center.clone().add(x, 40, z);
                        BlockDisplayHandle trident = displayBuilder.spawnBlock(dropLoc, Material.PRISMARINE_BRICKS);
                        trident.scale(0.12f, 0.12f, 0.9f).glow(200, 0, 50).interpolation(2, 0);
                        stormHandles.add(trident);
                        spawnedEntities.add(trident.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.7f);
            }

            // All 30 fall simultaneously (10-34 ticks = 1.2 seconds at 1.5x speed)
            if (dropped && ticksAlive > 10 && ticksAlive < 35) {
                float fallSpeed = 2.5f; // 1.5x freefall
                for (BlockDisplayHandle handle : stormHandles) {
                    if (handle.entity() == null || !handle.entity().isValid()) continue;
                    Location loc = handle.entity().getLocation();
                    loc.subtract(0, fallSpeed, 0);
                    handle.entity().teleport(loc);
                    if (ticksAlive % 3 == 0) {
                        DisplayBuilder.crimsonDust(loc, 4, 0.3);
                    }
                }
            }

            // Impact (tick 35)
            if (dropped && ticksAlive == 35) {
                for (BlockDisplayHandle handle : stormHandles) {
                    if (handle.entity() != null && handle.entity().isValid()) {
                        Location loc = handle.entity().getLocation();
                        triggerImpactDamage(loc);
                        DisplayBuilder.purpleDust(loc, 15, 0.5);
                    }
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Stormcall(plugin); }
    }

    // ================================================================
    // #142 — BRIMSTONE SIGIL
    // 3 simultaneous floor sigils that charge and detonate
    // ================================================================
    public static class BrimstoneSigil extends BossAttack {
        private final List<BlockDisplayHandle> sigilHandles = new ArrayList<>();
        private final List<Location> sigilLocations = new ArrayList<>();
        private int sigilsPlaced = 0;

        public BrimstoneSigil(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_sigil", AttackType.BOSS, 5), "calamitas");
            config.setDamage(40.0); // 20 hearts detonation
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(40.0);
            config.setImpactRadius(5.0);
        }

        @Override
        protected void onSpawn(Location center) {
            // First sigil telegraph
            DisplayBuilder.crimsonDust(center, 20, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Place 3 sigils sequentially (tick 10, 30, 50)
            int[] placeTicks = { 10, 30, 50 };
            for (int i = 0; i < 3; i++) {
                if (ticksAlive == placeTicks[i] && sigilsPlaced <= i) {
                    sigilsPlaced++;
                    double x = (Math.random() - 0.5) * 25;
                    double z = (Math.random() - 0.5) * 25;
                    Location sigilLoc = center.clone().add(x, 0.02, z);
                    sigilLocations.add(sigilLoc);
                    // Sigil glyph block display
                    BlockDisplayHandle sigil = displayBuilder.spawnBlock(sigilLoc, Material.RED_GLAZED_TERRACOTTA);
                    sigil.scale(5.0f, 0.03f, 5.0f).glow(200, 0, 50).interpolation(3, 0);
                    sigilHandles.add(sigil);
                    spawnedEntities.add(sigil.entity());
                    DisplayBuilder.playSound(sigilLoc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 0.7f);
                }
            }

            // Sigil charge pulsing
            for (int i = 0; i < sigilLocations.size(); i++) {
                Location sigilLoc = sigilLocations.get(i);
                int sigAge = ticksAlive - placeTicks[i];
                if (sigAge > 0 && sigAge < 60) {
                    // Pulse rate increases as charge progresses
                    int pulseInterval = Math.max(2, 8 - (sigAge / 10));
                    if (sigAge % pulseInterval == 0) {
                        DisplayBuilder.crimsonDust(sigilLoc, 8, 5.0);
                        DisplayBuilder.purpleDust(sigilLoc, 5, 3.0);
                        DisplayBuilder.playSound(sigilLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.4f, 0.8f + (sigAge / 60.0f) * 0.4f);
                    }
                }
                // Detonation at charge completion
                if (sigAge == 60) {
                    triggerImpactDamage(sigilLoc);
                    DisplayBuilder.crimsonDust(sigilLoc, 1, 3.0);
                    DisplayBuilder.crimsonDust(sigilLoc, 30, 5.0);
                    DisplayBuilder.playSound(sigilLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSigil(plugin); }
    }

    // ================================================================
    // #143 — TRI-DIRECTION TRIDENT SWEEP
    // 3 afterimage positions each fire 5-trident arc = 15 total
    // ================================================================
    public static class TriDirectionTridentSweep extends BossAttack {
        private final List<BlockDisplayHandle> afterimageHandles = new ArrayList<>();
        private boolean fired = false;

        public TriDirectionTridentSweep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_tri_direction_sweep", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts
            config.setDamageRadius(1.5);
            config.setDurationTicks(160);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — 3 afterimages split
            DisplayBuilder.purpleDust(center, 20, 3.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks)
            if (ticksAlive < 10) {
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 2.0);
                }
                return;
            }

            // Create 3 afterimages (tick 10)
            if (ticksAlive == 10 && !fired) {
                fired = true;
                double[] xOffsets = { -10, 0, 10 };
                for (double xOff : xOffsets) {
                    Location imageLoc = center.clone().add(xOff, 12, 0);
                    BlockDisplayHandle image = displayBuilder.spawnBlock(imageLoc, Material.CRIMSON_HYPHAE);
                    image.scale(0.8f, 1.8f, 0.4f).glow(200, 0, 50).interpolation(3, 0);
                    afterimageHandles.add(image);
                    spawnedEntities.add(image.entity());
                    DisplayBuilder.crimsonDust(imageLoc, 3, 1.0);
                }
            }

            // Fire volleys (tick 20)
            if (fired && ticksAlive == 20) {
                double[] xOffsets = { -10, 0, 10 };
                for (int i = 0; i < 3; i++) {
                    Location imageLoc = center.clone().add(xOffsets[i], 12, 0);
                    for (int j = 0; j < 5; j++) {
                        double spreadAngle = -Math.PI / 3 + (2 * Math.PI / 3) * (j / 4.0);
                        Location tridentLoc = imageLoc.clone().add(
                            Math.cos(spreadAngle) * 2, -1, Math.sin(spreadAngle) * 2);
                        DisplayBuilder.crimsonDust(tridentLoc, 8, 0.3);
                        DisplayBuilder.purpleDust(tridentLoc, 4, 0.2);
                    }
                    DisplayBuilder.playSound(imageLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 1.0f);
                }
            }

            // Afterimage particles
            if (fired && ticksAlive > 10 && ticksAlive < 60) {
                if (ticksAlive % 4 == 0) {
                    for (BlockDisplayHandle image : afterimageHandles) {
                        if (image.entity() != null && image.entity().isValid()) {
                            DisplayBuilder.crimsonDust(image.entity().getLocation(), 10, 1.0);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TriDirectionTridentSweep(plugin); }
    }

    // ================================================================
    // #144 — SHATTER
    // 1 overcharged trident detonates into 24 shard-tridents
    // ================================================================
    public static class Shatter extends BossAttack {
        private BlockDisplayHandle overcharged;
        private final List<BlockDisplayHandle> shardHandles = new ArrayList<>();
        private boolean launchedUp = false;
        private boolean detonated = false;

        public Shatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_shatter", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts per shard
            config.setDamageRadius(1.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(5);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(32.0);
            config.setImpactRadius(1.5);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — catches a trident, it glows crimson
            DisplayBuilder.crimsonDust(center.clone().add(0, 6, 0), 20, 1.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Overcharged trident launches upward (tick 10)
            if (ticksAlive == 10 && !launchedUp) {
                launchedUp = true;
                Location handLoc = center.clone().add(0, 6, 0);
                overcharged = displayBuilder.spawnBlock(handLoc, Material.PRISMARINE_BRICKS);
                overcharged.scale(0.2f, 0.2f, 1.2f).glow(200, 0, 50).interpolation(2, 0);
                spawnedEntities.add(overcharged.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
            }

            // Trident ascends (10-30 ticks)
            if (launchedUp && !detonated && ticksAlive > 10 && ticksAlive < 30) {
                if (overcharged.entity() != null) {
                    Location loc = overcharged.entity().getLocation();
                    loc.add(0, 2.5, 0);
                    overcharged.entity().teleport(loc);
                    DisplayBuilder.crimsonDust(loc, 20, 0.5);
                }
            }

            // Apex hang (tick 30-50 = 1 second)
            if (launchedUp && !detonated && ticksAlive >= 30 && ticksAlive < 50) {
                if (overcharged.entity() != null) {
                    DisplayBuilder.crimsonDust(overcharged.entity().getLocation(), 15, 1.0);
                }
                if (ticksAlive == 49) {
                    // Apex glow
                    DisplayBuilder.crimsonDust(overcharged.entity().getLocation(), 1, 1.0);
                }
            }

            // Detonation — shatter into 24 shards (tick 50)
            if (launchedUp && !detonated && ticksAlive == 50) {
                detonated = true;
                Location apexLoc = overcharged.entity() != null ?
                    overcharged.entity().getLocation() : center.clone().add(0, 50, 0);
                DisplayBuilder.playSound(apexLoc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f, 0.8f);

                for (int i = 0; i < 24; i++) {
                    double angle = (2 * Math.PI / 24) * i;
                    double elevAngle = -Math.PI / 4 + (Math.random() * Math.PI / 4);
                    BlockDisplayHandle shard = displayBuilder.spawnBlock(apexLoc.clone(), Material.PRISMARINE_BRICKS);
                    shard.scale(0.1f, 0.1f, 0.6f).glow(200, 0, 50).interpolation(2, 0);
                    shardHandles.add(shard);
                    spawnedEntities.add(shard.entity());
                }
            }

            // Shards rain down (50-90 ticks)
            if (detonated && ticksAlive > 50 && ticksAlive < 90) {
                int elapsed = ticksAlive - 50;
                for (int i = 0; i < shardHandles.size(); i++) {
                    BlockDisplayHandle shard = shardHandles.get(i);
                    if (shard.entity() == null || !shard.entity().isValid()) continue;
                    double angle = (2 * Math.PI / 24) * i;
                    double horizDist = elapsed * 0.8;
                    double yDrop = elapsed * 2.0;
                    Location apexLoc = center.clone().add(0, 50, 0);
                    Location shardLoc = apexLoc.clone().add(
                        Math.cos(angle) * horizDist, -yDrop, Math.sin(angle) * horizDist);
                    shard.entity().teleport(shardLoc);
                    if (elapsed % 3 == 0) {
                        DisplayBuilder.crimsonDust(shardLoc, 3, 0.2);
                    }
                    // Ground impact
                    if (shardLoc.getY() <= center.getY() + 0.5) {
                        triggerImpactDamage(shardLoc);
                        DisplayBuilder.purpleDust(shardLoc, 10, 0.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Shatter(plugin); }
    }

    // ================================================================
    // #145 — SCREAMING PURSUIT
    // 200% speed sprint with 4-block damage aura for 8 seconds
    // ================================================================
    public static class ScreamingPursuit extends BossAttack {
        private boolean sprinting = false;
        private int sprintStartTick = 0;

        public ScreamingPursuit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_screaming_pursuit", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); // 14 hearts per second of contact
            config.setDamageRadius(4.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            // No telegraph — begins screaming and sprinting
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 0 && !sprinting) {
                sprinting = true;
                sprintStartTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.8f);
            }

            // Sprint phase (0-160 ticks = 8 seconds)
            if (sprinting && ticksAlive - sprintStartTick < 160) {
                // Wide damage aura particles
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.crimsonDust(center, 10, 4.0);
                    DisplayBuilder.crimsonDust(center, 5, 2.0);
                    DisplayBuilder.purpleDust(center, 8, 3.0);
                }
                // Scream sound loop
                if (ticksAlive % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.9f);
                }
                // Footfall sounds
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_STEP, 0.6f, 1.0f);
                }
                // Trident at other players every 40 ticks
                if (ticksAlive % 40 == 0) {
                    Location tridentLoc = center.clone().add(0, 5, 0);
                    DisplayBuilder.crimsonDust(tridentLoc, 8, 0.5);
                    DisplayBuilder.playSound(tridentLoc, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                }
            }

            // Rage exhaustion (tick 160 — 0.5s pause)
            if (sprinting && ticksAlive - sprintStartTick == 160) {
                DisplayBuilder.crimsonDust(center, 10, 1.5);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScreamingPursuit(plugin); }
    }

    // ================================================================
    // #146 — HORIZON BEAM
    // Horizontal beam at Y+2, forces crouch, then Y+4 follow-up
    // ================================================================
    public static class HorizonBeam extends BossAttack {
        private boolean beam1Active = false;
        private boolean beam2Active = false;

        public HorizonBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_horizon_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(30.0); // 15 hearts per second
            config.setDamageRadius(1.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — END_ROD line at horizon height
            for (int x = -15; x <= 15; x += 2) {
                DisplayBuilder.purpleDust(center.clone().add(x, 2, 0), 3, 0.3);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Beam 1 at Y+2 (tick 10-110 = 5 seconds)
            if (ticksAlive == 10 && !beam1Active) {
                beam1Active = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);
            }
            if (beam1Active && ticksAlive >= 10 && ticksAlive < 110) {
                if (ticksAlive % 2 == 0) {
                    for (int x = -15; x <= 15; x += 2) {
                        for (int z = -15; z <= 15; z += 3) {
                            Location beamLoc = center.clone().add(x, 2, z);
                            DisplayBuilder.purpleDust(beamLoc, 2, 0.2);
                            DisplayBuilder.crimsonDust(beamLoc, 1, 0.3);
                        }
                    }
                }
                // Tridents at crouching targets
                if (ticksAlive % 25 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.0f);
                }
            }

            // Beam 2 at Y+4 (tick 115-215 = 5 seconds)
            if (ticksAlive == 115 && !beam2Active) {
                beam2Active = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.8f);
            }
            if (beam2Active && ticksAlive >= 115 && ticksAlive < 215) {
                if (ticksAlive % 2 == 0) {
                    for (int x = -15; x <= 15; x += 2) {
                        for (int z = -15; z <= 15; z += 3) {
                            Location beamLoc = center.clone().add(x, 4, z);
                            DisplayBuilder.purpleDust(beamLoc, 2, 0.2);
                            DisplayBuilder.crimsonDust(beamLoc, 1, 0.3);
                        }
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HorizonBeam(plugin); }
    }

    // ================================================================
    // #147 — BRIMSTONE VORTEX
    // 8-block pull zone + tridents from rim
    // ================================================================
    public static class BrimstoneVortex extends BossAttack {
        private boolean vortexActive = false;
        private int vortexTick = 0;

        public BrimstoneVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_vortex", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); // 8 hearts per second
            config.setDamageRadius(4.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.purpleDust(center, 30, 4.0);
            DisplayBuilder.crimsonDust(center, 20, 8.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null || center.getWorld() == null) return;

            if (ticksAlive == 10 && !vortexActive) {
                vortexActive = true;
                vortexTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }

            // Vortex pull (10-170 ticks = 8 seconds)
            if (vortexActive && ticksAlive - vortexTick < 160) {
                // Pull players toward center
                World w = center.getWorld();
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist <= 20.0 && dist > 1.0) {
                        Vector pullDir = center.toVector().subtract(player.getLocation().toVector()).normalize();
                        player.setVelocity(player.getVelocity().add(pullDir.multiply(0.12)));
                    }
                }

                // Vortex particles
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.purpleDust(center, 40, 4.0);
                    DisplayBuilder.crimsonDust(center, 15, 8.0);
                    DisplayBuilder.crimsonDust(center, 5, 2.0);
                    DisplayBuilder.purpleDust(center, 8, 3.0);
                }

                // Rim trident throws every 30 ticks
                if ((ticksAlive - vortexTick) % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.7f, 1.0f);
                }
            }

            // Vortex collapse (tick 170) — outward burst
            if (vortexActive && ticksAlive - vortexTick == 160) {
                World w = center.getWorld();
                for (Player player : w.getPlayers()) {
                    if (isExempt(player)) continue;
                    double dist = player.getLocation().distance(center);
                    if (dist <= 10.0) {
                        Vector pushDir = player.getLocation().toVector().subtract(center.toVector()).normalize();
                        player.setVelocity(player.getVelocity().add(pushDir.multiply(1.0)));
                    }
                }
                DisplayBuilder.crimsonDust(center, 2, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneVortex(plugin); }
    }

    // ================================================================
    // #148 — CRIMSON CRUCIBLE
    // 12-block radius brimstone sphere, inside vs outside dynamic
    // ================================================================
    public static class CrimsonCrucible extends BossAttack {
        private boolean crucibleActive = false;
        private int crucibleTick = 0;

        public CrimsonCrucible(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_crucible", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); // 12 hearts per second
            config.setDamageRadius(12.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Telegraph — all 5 streams converge downward
            DisplayBuilder.crimsonDust(center.clone().add(0, 10, 0), 30, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            Location crucibleCenter = center.clone().add(0, 10, 0);

            if (ticksAlive == 10 && !crucibleActive) {
                crucibleActive = true;
                crucibleTick = ticksAlive;
            }

            // Crucible active (10-210 ticks = 10 seconds)
            if (crucibleActive && ticksAlive - crucibleTick < 200) {
                // Sphere particles
                if (ticksAlive % 3 == 0) {
                    for (int i = 0; i < 12; i++) {
                        double theta = Math.random() * Math.PI;
                        double phi = Math.random() * 2 * Math.PI;
                        double x = 12 * Math.sin(theta) * Math.cos(phi);
                        double y = 12 * Math.sin(theta) * Math.sin(phi);
                        double z = 12 * Math.cos(theta);
                        Location sphereLoc = crucibleCenter.clone().add(x, y, z);
                        DisplayBuilder.crimsonDust(sphereLoc, 3, 0.5);
                        DisplayBuilder.purpleDust(sphereLoc, 2, 0.2);
                    }
                }

                // Stream convergence particles
                if (ticksAlive % 5 == 0) {
                    for (int s = 0; s < 5; s++) {
                        double angle = (2 * Math.PI / 5) * s;
                        Location streamLoc = crucibleCenter.clone().add(
                            Math.cos(angle) * 6, 5, Math.sin(angle) * 6);
                        DisplayBuilder.crimsonDust(streamLoc, 5, 1.0);
                    }
                }
            }

            // Collapse — 16 tridents burst outward (tick 210)
            if (crucibleActive && ticksAlive - crucibleTick == 200) {
                for (int i = 0; i < 16; i++) {
                    double angle = (2 * Math.PI / 16) * i;
                    Location burstLoc = crucibleCenter.clone().add(Math.cos(angle) * 3, -5, Math.sin(angle) * 3);
                    DisplayBuilder.purpleDust(burstLoc, 5, 0.5);
                }
                DisplayBuilder.crimsonDust(crucibleCenter, 2, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 0.8f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrimsonCrucible(plugin); }
    }

    // ================================================================
    // #149 — ABSOLUTE ZERO WINDOWS
    // Entire arena lethal except 3 safe zones that shift 4 times
    // ================================================================
    public static class AbsoluteZeroWindows extends BossAttack {
        private boolean floodActive = false;
        private int floodTick = 0;
        private int windowShift = 0;
        private final List<Location> currentWindows = new ArrayList<>();

        public AbsoluteZeroWindows(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_absolute_zero_windows", AttackType.BOSS, 5), "calamitas");
            config.setDamage(32.0); // 16 hearts per second outside window
            config.setDamageRadius(50.0); // Full arena
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            // Global pulse telegraph
            DisplayBuilder.crimsonDust(center, 100, 25.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !floodActive) {
                floodActive = true;
                floodTick = ticksAlive;
                generateWindows(center);
            }

            // Flood active (10-330 ticks = 16 seconds)
            if (floodActive && ticksAlive - floodTick < 320) {
                int elapsed = ticksAlive - floodTick;

                // Window shifts every 80 ticks (4 seconds)
                int currentShift = elapsed / 80;
                if (currentShift > windowShift && currentShift < 4) {
                    windowShift = currentShift;
                    // Old windows despawn particles
                    for (Location oldWin : currentWindows) {
                        DisplayBuilder.crimsonDust(oldWin, 5, 1.5);
                    }
                    generateWindows(center);
                    // New windows spawn particles
                    for (Location newWin : currentWindows) {
                        DisplayBuilder.purpleDust(newWin, 10, 1.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
                }

                // Arena flood particles
                if (elapsed % 4 == 0) {
                    for (int i = 0; i < 15; i++) {
                        double x = (Math.random() - 0.5) * 40;
                        double z = (Math.random() - 0.5) * 40;
                        Location floodLoc = center.clone().add(x, 0.5, z);
                        DisplayBuilder.crimsonDust(floodLoc, 3, 0.5);
                    }
                }

                // Window safe zone particles
                if (elapsed % 3 == 0) {
                    for (Location win : currentWindows) {
                        DisplayBuilder.purpleDust(win, 20, 1.5);
                    }
                }

                // Tridents into windows every 80 ticks
                if (elapsed % 80 == 40) {
                    for (Location win : currentWindows) {
                        DisplayBuilder.playSound(win, Sound.ENTITY_GUARDIAN_ATTACK, 0.6f, 1.0f);
                        DisplayBuilder.crimsonDust(win.clone().add(0, 8, 0), 8, 0.5);
                    }
                }
            }

            // Flood ends (tick 330) — recovery pause
            if (floodActive && ticksAlive - floodTick == 320) {
                // Particle dissipation
                DisplayBuilder.crimsonDust(center, 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.3f);
            }
        }

        private void generateWindows(Location center) {
            currentWindows.clear();
            for (int i = 0; i < 3; i++) {
                double x = (Math.random() - 0.5) * 30;
                double z = (Math.random() - 0.5) * 30;
                currentWindows.add(center.clone().add(x, 0, z));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbsoluteZeroWindows(plugin); }
    }

    // ================================================================
    // #150 — PHASE TERMINUS — RUIN
    // Phase 3->4 transition: 5-layer combo over 20 seconds
    // ================================================================
    public static class PhaseTerminusRuin extends BossAttack {
        private boolean ruinActive = false;
        private int ruinTick = 0;

        public PhaseTerminusRuin(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_phase_terminus_ruin", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); // 13 hearts trident base
            config.setDamageRadius(3.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(9999); // One-time
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            // 1.5 second telegraph — ascend, arms spread, streams descend
            Location elevated = center.clone().add(0, 40, 0);
            DisplayBuilder.crimsonDust(elevated, 30, 5.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-30 ticks = 1.5s)
            if (ticksAlive < 30) {
                if (ticksAlive % 3 == 0) {
                    Location elevated = center.clone().add(0, 40, 0);
                    DisplayBuilder.crimsonDust(elevated, 15, 3.0);
                    // Streams descend
                    for (int s = 0; s < 5; s++) {
                        double angle = (2 * Math.PI / 5) * s;
                        Location streamLoc = elevated.clone().add(
                            Math.cos(angle) * 4, -5 + ticksAlive * 0.5, Math.sin(angle) * 4);
                        DisplayBuilder.crimsonDust(streamLoc, 5, 1.0);
                    }
                }
                return;
            }

            if (ticksAlive == 30 && !ruinActive) {
                ruinActive = true;
                ruinTick = ticksAlive;
            }

            if (!ruinActive) return;
            int elapsed = ticksAlive - ruinTick;

            // Layer 1 (0-100 ticks = 0-5s): 20 sphere tridents
            if (elapsed >= 0 && elapsed < 100) {
                if (elapsed == 0) {
                    Location elevated = center.clone().add(0, 40, 0);
                    for (int i = 0; i < 20; i++) {
                        double theta = Math.random() * Math.PI;
                        double phi = Math.random() * 2 * Math.PI;
                        Location tLoc = elevated.clone().add(
                            Math.sin(theta) * Math.cos(phi) * 2,
                            Math.sin(theta) * Math.sin(phi) * 2,
                            Math.cos(theta) * 2);
                        DisplayBuilder.crimsonDust(tLoc, 5, 0.3);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.2f, 0.8f);
                }
                if (elapsed % 5 == 0) {
                    double x = (Math.random() - 0.5) * 30;
                    double z = (Math.random() - 0.5) * 30;
                    DisplayBuilder.crimsonDust(center.clone().add(x, 0.5, z), 5, 0.5);
                }
            }

            // Layer 2 (60-200 ticks = 3-10s): dual sweeping beams
            if (elapsed >= 60 && elapsed < 200) {
                if (elapsed % 3 == 0) {
                    double beamAngle1 = (elapsed - 60) * 0.03;
                    double beamAngle2 = beamAngle1 + Math.PI;
                    for (int d = 2; d <= 20; d += 3) {
                        Location b1 = center.clone().add(Math.cos(beamAngle1) * d, 3, Math.sin(beamAngle1) * d);
                        Location b2 = center.clone().add(Math.cos(beamAngle2) * d, 3, Math.sin(beamAngle2) * d);
                        DisplayBuilder.purpleDust(b1, 3, 0.2);
                        DisplayBuilder.purpleDust(b2, 3, 0.2);
                    }
                }
            }

            // Layer 3 (160-280 ticks = 8-14s): ground ring + trident ring
            if (elapsed >= 160 && elapsed < 280) {
                if (elapsed == 160) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
                }
                if (elapsed % 5 == 0) {
                    double ringAngle = elapsed * 0.1;
                    for (int i = 0; i < 16; i++) {
                        double angle = ringAngle + (2 * Math.PI / 16) * i;
                        Location ringLoc = center.clone().add(Math.cos(angle) * 8, 1, Math.sin(angle) * 8);
                        DisplayBuilder.crimsonDust(ringLoc, 3, 0.3);
                        DisplayBuilder.purpleDust(ringLoc, 2, 0.2);
                    }
                }
            }

            // Layer 4 (240-360 ticks = 12-18s): skull bursts + stream drops
            if (elapsed >= 240 && elapsed < 360) {
                if (elapsed % 40 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI / 2) * i;
                        Location skullLoc = center.clone().add(Math.cos(angle) * 12, 5, Math.sin(angle) * 12);
                        DisplayBuilder.crimsonDust(skullLoc, 1, 2.0);
                        DisplayBuilder.purpleDust(skullLoc, 30, 3.0);
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.6f);
                }
            }

            // Layer 5 (340-400 ticks = 17-20s): EVERYTHING
            if (elapsed >= 340 && elapsed < 400) {
                // Maximum density chaos
                if (elapsed % 2 == 0) {
                    for (int i = 0; i < 10; i++) {
                        double x = (Math.random() - 0.5) * 30;
                        double z = (Math.random() - 0.5) * 30;
                        Location chaosLoc = center.clone().add(x, Math.random() * 5, z);
                        Particle[] particles = { Particle.LAVA, Particle.FLAME, Particle.DRAGON_BREATH,
                            Particle.TOTEM_OF_UNDYING, Particle.WITCH, Particle.END_ROD };
                        DisplayBuilder.crimsonDust(chaosLoc, 3, 1.0);
                        DisplayBuilder.crimsonDust(chaosLoc, 2, 0.5);
                    }
                }
                if (elapsed % 10 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.8f);
                }
            }

            // Phase 4 trigger (tick 400)
            if (elapsed == 400) {
                // White flash — all particles clear
                DisplayBuilder.purpleDust(center, 200, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseTerminusRuin(plugin); }
    }
}
