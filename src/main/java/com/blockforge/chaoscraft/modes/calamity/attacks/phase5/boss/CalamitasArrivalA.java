package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

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
 * Supreme Calamitas — Phase 4E Boss (Boss 5, Final Boss)
 * Phase 1: "The Arrival" — HP 100% to 75%
 * Attacks #1-10
 *
 * She descends. Every strike is surgical. Phase 1 teaches patterns that
 * return far worse in later phases. Crimson robes trailing DUST particles.
 * NO status effects — damage only.
 */
public final class CalamitasArrivalA {

    private CalamitasArrivalA() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TheDescent(plugin));
        registry.register(new SingularIntent(plugin));
        registry.register(new TridentFan(plugin));
        registry.register(new BrimstoneSkull(plugin));
        registry.register(new AltitudePunishment(plugin));
        registry.register(new BrimstoneLaser(plugin));
        registry.register(new TheEightPointCrown(plugin));
        registry.register(new GroundEruption(plugin));
        registry.register(new SeekerMark(plugin));
        registry.register(new ProximityWarning(plugin));
    }

    // ================================================================
    // 1. THE DESCENT — Cinematic opener, radial trident burst at Y+20
    // ================================================================
    public static class TheDescent extends BossAttack {
        private final List<BlockDisplayHandle> descentHandles = new ArrayList<>();
        private boolean burstFired = false;
        private int burstTick = 0;

        public TheDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_the_descent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(999999);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Silhouette begins at Y+100 — crimson particle outline
            BlockDisplayHandle silhouette = displayBuilder.spawnBlock(
                center.clone().add(0, 100, 0), Material.RED_STAINED_GLASS);
            silhouette.scale(1.5f, 3.0f, 1.5f).glow(180, 0, 0).interpolation(3, 0);
            descentHandles.add(silhouette);
            spawnedEntities.add(silhouette.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Descent phase (0-100 ticks = 5 seconds, Y+100 to Y+20)
            if (ticksAlive < 100 && !burstFired) {
                float progress = ticksAlive / 100.0f;
                double currentY = 100.0 - (progress * 80.0);
                if (!descentHandles.isEmpty()) {
                    descentHandles.get(0).entity().teleport(
                        center.clone().add(0, currentY, 0));
                }
                // Sky streams activate one by one
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, currentY, 0), 15, 2.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.5f, 0.6f);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, currentY, 0), 8, 1.5);
                }
            }
            // Pause at Y+20 (tick 100-103)
            else if (ticksAlive >= 100 && ticksAlive < 103 && !burstFired) {
                if (!descentHandles.isEmpty()) {
                    descentHandles.get(0).entity().teleport(center.clone().add(0, 20, 0));
                }
            }
            // Radial burst — 8 trident-like projectile displays (tick 103)
            else if (ticksAlive == 103 && !burstFired) {
                burstFired = true;
                burstTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.8f, 1.2f);
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 2, 20, Math.sin(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.IRON_BLOCK);
                    trident.scale(0.15f, 0.15f, 1.0f).glow(180, 20, 0).interpolation(2, 0);
                    descentHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 25, 3.0);
            }
            // Tridents fly outward radially (103-183 ticks = 4 seconds)
            else if (burstFired && ticksAlive - burstTick > 0 && ticksAlive - burstTick < 80) {
                float dist = (ticksAlive - burstTick) * 1.4f;
                for (int i = 1; i < descentHandles.size() && i <= 8; i++) {
                    double angle = (Math.PI * 2 / 8) * (i - 1);
                    Location travelLoc = center.clone().add(
                        Math.cos(angle) * (2 + dist), 20, Math.sin(angle) * (2 + dist));
                    descentHandles.get(i).entity().teleport(travelLoc);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, dist * 0.3);
                }
            }
            // Ground impact — soul fire vents at landing positions (tick 183+)
            else if (burstFired && ticksAlive - burstTick == 80) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location impactLoc = center.clone().add(
                        Math.cos(angle) * 114, 0.1, Math.sin(angle) * 114);
                    DisplayBuilder.crimsonDust(impactLoc, 12, 1.5);
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheDescent(plugin); }
    }

    // ================================================================
    // 2. SINGULAR INTENT — Aimed single trident, pure read-and-dodge
    // ================================================================
    public static class SingularIntent extends BossAttack {
        private final List<BlockDisplayHandle> tridentHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public SingularIntent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_singular_intent", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(160);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Electric spark spiral forming on right hand
            BlockDisplayHandle handGlow = displayBuilder.spawnBlock(
                center.clone().add(1, 20, 0), Material.IRON_BLOCK);
            handGlow.scale(0.3f, 0.3f, 0.3f).glow(200, 200, 255).interpolation(3, 0);
            tridentHandles.add(handGlow);
            spawnedEntities.add(handGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 1.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph spiral (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !fired) {
                double angle = ticksAlive * 0.4;
                float radius = 1.0f - (ticksAlive / 36.0f) * 0.7f;
                Location spiralLoc = center.clone().add(
                    1 + Math.cos(angle) * radius, 20 + Math.sin(angle) * radius, 0);
                if (!tridentHandles.isEmpty()) {
                    tridentHandles.get(0).entity().teleport(spiralLoc);
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(spiralLoc, 6, 0.5);
                }
            }
            // Fire single trident (tick 36)
            else if (ticksAlive == 36 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                BlockDisplayHandle projectile = displayBuilder.spawnBlock(
                    center.clone().add(1, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                projectile.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                tridentHandles.add(projectile);
                spawnedEntities.add(projectile.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.3f);
            }
            // Trident travels (36-76 ticks = 2 seconds flight)
            else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 2.2f;
                if (tridentHandles.size() > 1) {
                    tridentHandles.get(1).entity().teleport(
                        center.clone().add(1, 20 - dist * 0.05, dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(1, 20 - dist * 0.05, dist), 4, 0.4);
                }
            }
            // Impact — residual electric sparks (tick 76)
            else if (fired && ticksAlive - fireTick == 40) {
                Location impactLoc = center.clone().add(1, 0.1, 88);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.crimsonDust(impactLoc, 15, 1.5);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_ARROW_HIT, 0.8f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SingularIntent(plugin); }
    }

    // ================================================================
    // 3. TRIDENT FAN — 3-spread fan aimed at target player
    // ================================================================
    public static class TridentFan extends BossAttack {
        private final List<BlockDisplayHandle> fanHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public TridentFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_trident_fan", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Three electric spark lines tracing outward in 30-degree fan
            for (int i = -1; i <= 1; i++) {
                double angle = Math.toRadians(15 * i);
                Location linePt = center.clone().add(Math.sin(angle) * 3, 20, Math.cos(angle) * 3);
                BlockDisplayHandle line = displayBuilder.spawnBlock(linePt, Material.IRON_BLOCK);
                line.scale(0.1f, 0.1f, 2.0f).glow(200, 200, 255).interpolation(3, 0);
                fanHandles.add(line);
                spawnedEntities.add(line.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph phase (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !fired) {
                if (ticksAlive % 8 == 0) {
                    for (int i = -1; i <= 1; i++) {
                        double angle = Math.toRadians(15 * i);
                        DisplayBuilder.crimsonDust(
                            center.clone().add(Math.sin(angle) * 4, 20, Math.cos(angle) * 4), 5, 0.8);
                    }
                }
            }
            // Fire 3 tridents in fan (tick 40)
            else if (ticksAlive == 40 && !fired) {
                fired = true;
                fireTick = ticksAlive;
                for (int i = -1; i <= 1; i++) {
                    double angle = Math.toRadians(15 * i);
                    Location spawnLoc = center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    fanHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Fan tridents travel (40-80 ticks = 2 seconds)
            else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 1.8f;
                int idx = 3;
                for (int i = -1; i <= 1; i++) {
                    if (idx < fanHandles.size()) {
                        double angle = Math.toRadians(15 * i);
                        double vertOffset = (i == 0) ? 0 : (i == -1 ? 0.5 : -0.3);
                        fanHandles.get(idx).entity().teleport(
                            center.clone().add(Math.sin(angle) * (2 + dist),
                                20 + vertOffset, Math.cos(angle) * (2 + dist)));
                    }
                    idx++;
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 20, 2 + dist), 6, 2.0);
                }
            }
            // Impact (tick 80)
            else if (fired && ticksAlive - fireTick == 40) {
                for (int i = -1; i <= 1; i++) {
                    double angle = Math.toRadians(15 * i);
                    Location impactLoc = center.clone().add(
                        Math.sin(angle) * 74, 0.1, Math.cos(angle) * 74);
                    DisplayBuilder.crimsonDust(impactLoc, 8, 1.0);
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 72));
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_HIT, 0.7f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TridentFan(plugin); }
    }

    // ================================================================
    // 4. BRIMSTONE SKULL — Slow skull projectile with splash detonation
    // ================================================================
    public static class BrimstoneSkull extends BossAttack {
        private final List<BlockDisplayHandle> skullHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;

        public BrimstoneSkull(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_skull", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Skull materializes in palm — growing over telegraph
            BlockDisplayHandle skull = displayBuilder.spawnBlock(
                center.clone().add(0.5, 20, 0), Material.SOUL_SAND);
            skull.scale(0.3f, 0.3f, 0.3f).glow(180, 80, 0).interpolation(3, 0);
            skullHandles.add(skull);
            spawnedEntities.add(skull.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — skull grows in palm (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !launched) {
                float scale = 0.3f + (ticksAlive / 30.0f) * 0.7f;
                if (!skullHandles.isEmpty()) {
                    skullHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 20 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0.5, 20, 0), 8, 0.8);
                }
            }
            // Launch skull (tick 30)
            else if (ticksAlive == 30 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.4f, 0.5f);
            }
            // Skull travels slowly (30-130 ticks = 5 seconds at velocity 0.8)
            else if (launched && ticksAlive - launchTick < 100) {
                float dist = (ticksAlive - launchTick) * 0.8f;
                if (!skullHandles.isEmpty()) {
                    skullHandles.get(0).entity().teleport(
                        center.clone().add(0.5, 20 - dist * 0.02, dist));
                }
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(
                        center.clone().add(0.5, 20 - dist * 0.02, dist), 6, 0.6);
                }
            }
            // Detonation — splash radius 3 blocks (tick 130)
            else if (launched && ticksAlive - launchTick == 100) {
                float dist = 100 * 0.8f;
                Location detonationLoc = center.clone().add(0.5, 0.1, dist);
                // 4 lava columns in plus pattern
                for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    BlockDisplayHandle column = displayBuilder.spawnBlock(
                        detonationLoc.clone().add(offset[0], 0, offset[1]), Material.MAGMA_BLOCK);
                    column.scale(1.0f, 2.0f, 1.0f).glow(255, 100, 0).interpolation(2, 0);
                    spawnedEntities.add(column.entity());
                }
                triggerImpactDamage(detonationLoc);
                DisplayBuilder.crimsonDust(detonationLoc, 30, 3.0);
                DisplayBuilder.playSound(detonationLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                DisplayBuilder.playSound(detonationLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneSkull(plugin); }
    }

    // ================================================================
    // 5. ALTITUDE PUNISHMENT — Vertical cylindrical damage field below her
    // ================================================================
    public static class AltitudePunishment extends BossAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private boolean pulseActive = false;
        private int pulseStartTick = 0;

        public AltitudePunishment(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_altitude_punishment", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // She descends to Y+17 — pulsing crimson aura
            BlockDisplayHandle aura = displayBuilder.spawnBlock(
                center.clone().add(0, 17, 0), Material.RED_STAINED_GLASS);
            aura.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(3, 0);
            columnHandles.add(aura);
            spawnedEntities.add(aura.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — descending aura pulses (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !pulseActive) {
                float pulseSize = 2.0f + (float) Math.sin(ticksAlive * 0.3) * 1.5f;
                if (!columnHandles.isEmpty()) {
                    columnHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-pulseSize / 2, 17 - pulseSize / 2, -pulseSize / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulseSize, pulseSize, pulseSize),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 12, 0), 10, 3.0);
                }
            }
            // Damage pulses begin (tick 40) — 3 pulses over 60 ticks
            else if (ticksAlive == 40 && !pulseActive) {
                pulseActive = true;
                pulseStartTick = ticksAlive;
            }
            // Active pulse phase (40-100 ticks = 3 seconds, 3 pulses at 20-tick intervals)
            else if (pulseActive && ticksAlive - pulseStartTick < 60) {
                if ((ticksAlive - pulseStartTick) % 20 == 0) {
                    // Damage cylinder below her Y+17 down to Y+0
                    for (int y = 0; y <= 17; y += 3) {
                        DisplayBuilder.crimsonDust(center.clone().add(0, y, 0), 8, 6.0);
                    }
                    triggerImpactDamage(center.clone().add(0, 8, 0));
                    DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.5f, 0.6f);
                }
            }
            // Return to cruise altitude (tick 100+)
            else if (pulseActive && ticksAlive - pulseStartTick >= 60) {
                if (ticksAlive - pulseStartTick == 60) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.0f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AltitudePunishment(plugin); }
    }

    // ================================================================
    // 6. BRIMSTONE LASER — Sustained tracking beam, 4 seconds
    // ================================================================
    public static class BrimstoneLaser extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean beamActive = false;
        private int beamStartTick = 0;

        public BrimstoneLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_laser", AttackType.BOSS, 5), "calamitas");
            config.setDamage(12.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hand extends toward player — soul fire flame particle line traces
            BlockDisplayHandle handGlow = displayBuilder.spawnBlock(
                center.clone().add(1, 20, 0), Material.MAGMA_BLOCK);
            handGlow.scale(0.4f, 0.4f, 0.4f).glow(255, 80, 0).interpolation(3, 0);
            beamHandles.add(handGlow);
            spawnedEntities.add(handGlow.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — beam charge builds (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !beamActive) {
                float intensity = ticksAlive / 40.0f;
                if (!beamHandles.isEmpty()) {
                    float scale = 0.4f + intensity * 0.6f;
                    beamHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 20 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1, 20, 0), 6 + (int)(intensity * 8), 1.0);
                }
            }
            // Beam activates (tick 40) — sustained for 80 ticks = 4 seconds
            else if (ticksAlive == 40 && !beamActive) {
                beamActive = true;
                beamStartTick = ticksAlive;
                // Beam ray segments
                for (int seg = 0; seg < 10; seg++) {
                    Location segLoc = center.clone().add(1, 20, 2 + seg * 3);
                    BlockDisplayHandle beamSeg = displayBuilder.spawnBlock(segLoc, Material.ORANGE_STAINED_GLASS);
                    beamSeg.scale(0.5f, 0.5f, 3.0f).glow(255, 100, 0).interpolation(1, 0);
                    beamHandles.add(beamSeg);
                    spawnedEntities.add(beamSeg.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.8f, 0.7f);
            }
            // Beam sustained — tracking first 40 ticks, locked last 40 (40-120)
            else if (beamActive && ticksAlive - beamStartTick < 80) {
                int beamTick = ticksAlive - beamStartTick;
                // Slow rotation tracking for first 2 seconds
                double rotAngle = (beamTick < 40)
                    ? Math.toRadians(beamTick * 5.0 / 40.0)
                    : Math.toRadians(5.0);
                for (int seg = 1; seg < beamHandles.size() && seg <= 10; seg++) {
                    double dist = 2 + (seg - 1) * 3;
                    Location segLoc = center.clone().add(
                        1 + Math.sin(rotAngle) * dist, 20, Math.cos(rotAngle) * dist);
                    beamHandles.get(seg).entity().teleport(segLoc);
                }
                if (beamTick % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(1, 20, 15), 10, 2.0);
                }
            }
            // Beam ends (tick 120)
            else if (beamActive && ticksAlive - beamStartTick == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 0.6f, 1.1f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneLaser(plugin); }
    }

    // ================================================================
    // 7. THE EIGHT-POINT CROWN — 8 tridents in horizontal ring at Y+22
    // ================================================================
    public static class TheEightPointCrown extends BossAttack {
        private final List<BlockDisplayHandle> crownHandles = new ArrayList<>();
        private boolean released = false;
        private int releaseTick = 0;

        public TheEightPointCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_eight_point_crown", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 spark points materialize in a ring at Y+22
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 / 8) * i;
                Location pointLoc = center.clone().add(Math.cos(angle) * 4, 22, Math.sin(angle) * 4);
                BlockDisplayHandle point = displayBuilder.spawnBlock(pointLoc, Material.IRON_BLOCK);
                point.scale(0.2f, 0.2f, 0.2f).glow(200, 200, 255).interpolation(3, 0);
                crownHandles.add(point);
                spawnedEntities.add(point.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.5f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — points pulse in ring (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !released) {
                float pulse = 0.2f + (float) Math.sin(ticksAlive * 0.4) * 0.1f;
                for (int i = 0; i < 8 && i < crownHandles.size(); i++) {
                    crownHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, pulse, pulse),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 12, 4.0);
                }
            }
            // Release all 8 outward (tick 40)
            else if (ticksAlive == 40 && !released) {
                released = true;
                releaseTick = ticksAlive;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 1.1f);
                // Replace points with trident projectiles
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location spawnLoc = center.clone().add(Math.cos(angle) * 4, 22, Math.sin(angle) * 4);
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    crownHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
            }
            // Tridents fly outward at Y+22 (40-120 ticks = 4 seconds)
            else if (released && ticksAlive - releaseTick < 80) {
                float dist = (ticksAlive - releaseTick) * 1.6f;
                for (int i = 0; i < 8; i++) {
                    int handleIdx = 8 + i;
                    if (handleIdx < crownHandles.size()) {
                        double angle = (Math.PI * 2 / 8) * i;
                        crownHandles.get(handleIdx).entity().teleport(
                            center.clone().add(Math.cos(angle) * (4 + dist), 22, Math.sin(angle) * (4 + dist)));
                    }
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 22, 0), 8, 4 + dist * 0.2);
                }
            }
            // Wall ricochet (tick 120) — reflected once
            else if (released && ticksAlive - releaseTick == 80) {
                triggerImpactDamage(center.clone().add(0, 22, 0));
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location wallPt = center.clone().add(Math.cos(angle) * 132, 22, Math.sin(angle) * 132);
                    DisplayBuilder.crimsonDust(wallPt, 10, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 1.0f, 1.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheEightPointCrown(plugin); }
    }

    // ================================================================
    // 8. GROUND ERUPTION — 3 marked ground positions erupt simultaneously
    // ================================================================
    public static class GroundEruption extends BossAttack {
        private final List<BlockDisplayHandle> eruptionHandles = new ArrayList<>();
        private boolean erupted = false;
        private int eruptTick = 0;

        public GroundEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_ground_eruption", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 lava particle circles on ground
            double[][] positions = {{0, 0, 5}, {-4, 0, 8}, {3, 0, 3}};
            for (double[] pos : positions) {
                Location markLoc = center.clone().add(pos[0], 0.05, pos[2]);
                BlockDisplayHandle mark = displayBuilder.spawnBlock(markLoc, Material.MAGMA_BLOCK);
                mark.scale(3.0f, 0.06f, 3.0f).glow(255, 80, 0).interpolation(3, 0);
                eruptionHandles.add(mark);
                spawnedEntities.add(mark.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_POP, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — circles pulse (0-36 ticks = 1.8 seconds)
            if (ticksAlive < 36 && !erupted) {
                float pulse = 3.0f + (float) Math.sin(ticksAlive * 0.3) * 0.5f;
                for (int i = 0; i < 3 && i < eruptionHandles.size(); i++) {
                    eruptionHandles.get(i).entity().setTransformation(new Transformation(
                        new Vector3f(-pulse / 2, -0.03f, -pulse / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(pulse, 0.06f, pulse),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 8 == 0) {
                    double[][] positions = {{0, 0, 5}, {-4, 0, 8}, {3, 0, 3}};
                    for (double[] pos : positions) {
                        DisplayBuilder.crimsonDust(center.clone().add(pos[0], 0.2, pos[2]), 6, 1.5);
                    }
                }
            }
            // Eruption (tick 36) — columns of brimstone fire
            else if (ticksAlive == 36 && !erupted) {
                erupted = true;
                eruptTick = ticksAlive;
                double[][] positions = {{0, 0, 5}, {-4, 0, 8}, {3, 0, 3}};
                for (double[] pos : positions) {
                    Location eruptLoc = center.clone().add(pos[0], 0.1, pos[2]);
                    // Column 4 blocks tall
                    for (int y = 0; y < 4; y++) {
                        BlockDisplayHandle col = displayBuilder.spawnBlock(
                            eruptLoc.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                        col.scale(1.5f, 1.0f, 1.5f).glow(255, 60, 0).interpolation(1, 0);
                        spawnedEntities.add(col.entity());
                    }
                    triggerImpactDamage(eruptLoc);
                    DisplayBuilder.crimsonDust(eruptLoc, 20, 1.5);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.5f, 0.8f);
            }
            // Puddle persistence (36-116 ticks = 4 seconds)
            else if (erupted && ticksAlive - eruptTick < 80) {
                if (ticksAlive % 10 == 0) {
                    double[][] positions = {{0, 0, 5}, {-4, 0, 8}, {3, 0, 3}};
                    for (double[] pos : positions) {
                        DisplayBuilder.crimsonDust(center.clone().add(pos[0], 0.2, pos[2]), 5, 1.5);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundEruption(plugin); }
    }

    // ================================================================
    // 9. SEEKER MARK — Single trident with trajectory adjustment
    // ================================================================
    public static class SeekerMark extends BossAttack {
        private final List<BlockDisplayHandle> seekerHandles = new ArrayList<>();
        private boolean launched = false;
        private int launchTick = 0;
        private double seekAngle = 0;

        public SeekerMark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_seeker_mark", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // CRIT halo around target player — mark visible at shoulder height
            BlockDisplayHandle halo = displayBuilder.spawnBlock(
                center.clone().add(0, 1.5, 8), Material.GLOWSTONE);
            halo.scale(2.0f, 0.1f, 2.0f).glow(255, 255, 200).interpolation(3, 0);
            seekerHandles.add(halo);
            spawnedEntities.add(halo.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.2f, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — halo orbits target (0-40 ticks = 2 seconds)
            if (ticksAlive < 40 && !launched) {
                double orbit = ticksAlive * 0.3;
                if (!seekerHandles.isEmpty()) {
                    seekerHandles.get(0).entity().teleport(
                        center.clone().add(Math.cos(orbit) * 1.0, 1.5, 8 + Math.sin(orbit) * 1.0));
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 1.5, 8), 8, 2.0);
                }
            }
            // Launch seeker trident (tick 40)
            else if (ticksAlive == 40 && !launched) {
                launched = true;
                launchTick = ticksAlive;
                seekAngle = 0;
                BlockDisplayHandle trident = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                trident.scale(0.12f, 0.12f, 1.2f).glow(220, 220, 255).interpolation(1, 0);
                seekerHandles.add(trident);
                spawnedEntities.add(trident.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            }
            // Seeker travels with trajectory adjustments (40-140 ticks = 5 seconds tracking)
            else if (launched && ticksAlive - launchTick < 100) {
                float dist = (ticksAlive - launchTick) * 1.6f;
                // Adjust heading by up to 12 degrees every 10 ticks, max 30 total
                if ((ticksAlive - launchTick) % 10 == 0 && Math.abs(seekAngle) < Math.toRadians(30)) {
                    seekAngle += Math.toRadians(6);
                    DisplayBuilder.playSound(center, Sound.BLOCK_METAL_HIT, 0.4f, 1.6f);
                }
                if (seekerHandles.size() > 1) {
                    seekerHandles.get(1).entity().teleport(
                        center.clone().add(
                            Math.sin(seekAngle) * dist,
                            20 - dist * 0.1,
                            Math.cos(seekAngle) * dist));
                }
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(
                        Math.sin(seekAngle) * dist, 20 - dist * 0.1, Math.cos(seekAngle) * dist), 5, 0.5);
                }
            }
            // Impact or despawn (tick 140)
            else if (launched && ticksAlive - launchTick == 100) {
                Location impactLoc = center.clone().add(0, 0.1, 160);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.crimsonDust(impactLoc, 15, 2.0);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeekerMark(plugin); }
    }

    // ================================================================
    // 10. PROXIMITY WARNING — Punishes bunched players with cluster tridents
    // ================================================================
    public static class ProximityWarning extends BossAttack {
        private final List<BlockDisplayHandle> warningHandles = new ArrayList<>();
        private boolean scanned = false;
        private int scanTick = 0;

        public ProximityWarning(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_proximity_warning", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Eyes flare white — twin END_ROD beams downward
            BlockDisplayHandle leftEye = displayBuilder.spawnBlock(
                center.clone().add(-0.3, 21, 0), Material.SEA_LANTERN);
            leftEye.scale(0.15f, 0.15f, 0.15f).glow(255, 255, 255).interpolation(2, 0);
            warningHandles.add(leftEye);
            spawnedEntities.add(leftEye.entity());

            BlockDisplayHandle rightEye = displayBuilder.spawnBlock(
                center.clone().add(0.3, 21, 0), Material.SEA_LANTERN);
            rightEye.scale(0.15f, 0.15f, 0.15f).glow(255, 255, 255).interpolation(2, 0);
            warningHandles.add(rightEye);
            spawnedEntities.add(rightEye.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph — eye beams sweep downward (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !scanned) {
                float sweepAngle = (ticksAlive / 30.0f) * (float) Math.PI;
                Location beamEnd = center.clone().add(Math.sin(sweepAngle) * 10, 0.5, Math.cos(sweepAngle) * 10);
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.crimsonDust(beamEnd, 6, 1.5);
                }
            }
            // Scan and fire at clusters (tick 30)
            else if (ticksAlive == 30 && !scanned) {
                scanned = true;
                scanTick = ticksAlive;
                // Spawn tridents aimed at cluster center
                for (int i = 0; i < 3; i++) {
                    double angle = Math.toRadians(5 * (i - 1));
                    Location spawnLoc = center.clone().add(Math.sin(angle), 20, Math.cos(angle));
                    BlockDisplayHandle trident = displayBuilder.spawnBlock(spawnLoc, Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    trident.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    warningHandles.add(trident);
                    spawnedEntities.add(trident.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.1f);
            }
            // Tridents fly toward cluster (30-60 ticks)
            else if (scanned && ticksAlive - scanTick < 30) {
                float dist = (ticksAlive - scanTick) * 2.0f;
                for (int i = 0; i < 3; i++) {
                    int idx = 2 + i;
                    if (idx < warningHandles.size()) {
                        double angle = Math.toRadians(5 * (i - 1));
                        warningHandles.get(idx).entity().teleport(
                            center.clone().add(Math.sin(angle) * dist, 20 - dist * 0.3, Math.cos(angle) * dist));
                    }
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.crimsonDust(center.clone().add(0, 15, dist), 6, 1.5);
                }
            }
            // Impact (tick 60)
            else if (scanned && ticksAlive - scanTick == 30) {
                Location impactLoc = center.clone().add(0, 0.5, 60);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.crimsonDust(impactLoc, 15, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ProximityWarning(plugin); }
    }
}
