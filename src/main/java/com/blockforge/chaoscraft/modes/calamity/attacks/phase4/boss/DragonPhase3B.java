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
 * Phase 4D Boss — THE VOID EMPEROR (Ender Dragon)
 * Phase 3B: Attacks #101-115
 * Mid-to-late Phase 3: Deception attacks, essence escalations,
 * sky weapon integration, floor collapse followup, pursuit mechanics.
 * Phase 3 damage range: 18-36 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase3B {

    private DragonPhase3B() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PhantomFeint(plugin));
        registry.register(new BrimstoneColumnArray(plugin));
        registry.register(new VoidmawEchoMassRift(plugin));
        registry.register(new WingShockwave(plugin));
        registry.register(new DoGEchoCrystalPrison(plugin));
        registry.register(new StreamConvergenceStormGrid(plugin));
        registry.register(new VoidSiphon(plugin));
        registry.register(new PhantomBarrage(plugin));
        registry.register(new AllEssencesTrident(plugin));
        registry.register(new CelestialRain(plugin));
        registry.register(new VoidSplit(plugin));
        registry.register(new GroundZeroBreath(plugin));
        registry.register(new SkyCollapse(plugin));
        registry.register(new Pursuit(plugin));
        registry.register(new VoidCollapseSphere(plugin));
    }

    // ================================================================
    // #101 — PHANTOM FEINT — Fake charge direction A, real charge direction B
    // ================================================================
    public static class PhantomFeint extends BossAttack {
        private final List<BlockDisplayHandle> feintHandles = new ArrayList<>();
        private boolean chargedReal = false;

        public PhantomFeint(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_phantom_feint", AttackType.BOSS, 4), "dragon");
            config.setDamage(28.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Fake telegraph pointing direction A (north)
            BlockDisplayHandle fakeCone = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 5), Material.WHITE_STAINED_GLASS);
            fakeCone.scale(2.0f, 2.0f, 6.0f).glow(255, 255, 255).interpolation(2, 0);
            feintHandles.add(fakeCone);
            spawnedEntities.add(fakeCone.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fake telegraph (0-8 ticks = 0.4 seconds) pointing north
            if (ticksAlive < 8 && !chargedReal) {
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 5), 8, 3.0);
            }
            // Real charge fires EAST (90-degree offset) (tick 8)
            else if (ticksAlive == 8 && !chargedReal) {
                chargedReal = true;
                // Charge trail going east
                for (int i = 0; i < 10; i++) {
                    Location trailPt = center.clone().add(i * 3, 10 - i * 0.5, 0);
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(trailPt, Material.WHITE_STAINED_GLASS);
                    trail.scale(2.0f, 3.0f, 2.0f).glow(255, 255, 255).interpolation(1, 0);
                    feintHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                Location chargeLoc = center.clone().add(25, 2, 0);
                triggerImpactDamage(chargeLoc);
                DisplayBuilder.cyanDust(chargeLoc, 20, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 2.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomFeint(plugin); }
    }

    // ================================================================
    // #102 — BRIMSTONE COLUMN ARRAY — 16 simultaneous brimstone columns
    // ================================================================
    public static class BrimstoneColumnArray extends BossAttack {
        private final List<BlockDisplayHandle> columnHandles = new ArrayList<>();
        private boolean columnsErupted = false;

        public BrimstoneColumnArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_brimstone_column_array", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 16 magma markers
            for (int q = 0; q < 4; q++) {
                for (int i = 0; i < 4; i++) {
                    double qAngle = (Math.PI / 2) * q + 0.3;
                    double radius = 5 + i * 5;
                    Location markerLoc = center.clone().add(Math.cos(qAngle + i * 0.2) * radius, 0.05, Math.sin(qAngle + i * 0.2) * radius);
                    BlockDisplayHandle marker = displayBuilder.spawnBlock(markerLoc, Material.MAGMA_BLOCK);
                    marker.scale(0.6f, 0.1f, 0.6f).glow(200, 0, 80).interpolation(2, 0);
                    columnHandles.add(marker);
                    spawnedEntities.add(marker.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !columnsErupted) {
                columnsErupted = true;
                for (BlockDisplayHandle marker : columnHandles) {
                    Location markerLoc = marker.entity().getLocation();
                    for (int y = 0; y < 6; y++) {
                        BlockDisplayHandle col = displayBuilder.spawnBlock(
                            markerLoc.clone().add(0, y, 0), Material.MAGMA_BLOCK);
                        col.scale(1.0f, 1.0f, 1.0f).glow(200, 0, 80).interpolation(1, 0);
                        spawnedEntities.add(col.entity());
                    }
                    triggerImpactDamage(markerLoc);
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 20.0);
                DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 2.5f, 0.8f);
            }
            // Columns collapse (tick 26)
            if (columnsErupted && ticksAlive == 26) {
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_HURT, 1.5f, 0.7f);
            }
            // Scorched patches (26-186 ticks = 8 seconds)
            if (columnsErupted && ticksAlive > 26 && ticksAlive < 186 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.3, 0), 6, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrimstoneColumnArray(plugin); }
    }

    // ================================================================
    // #103 — VOIDMAW ECHO: MASS RIFT — 8 full void rifts, ring pattern
    // ================================================================
    public static class VoidmawEchoMassRift extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private boolean riftsOpen = false;

        public VoidmawEchoMassRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_voidmaw_echo_mass_rift", AttackType.BOSS, 4), "dragon");
            config.setDamage(28.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 3.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !riftsOpen) {
                riftsOpen = true;
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    double radius = 12;
                    Location riftLoc = center.clone().add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                    for (int y = 0; y < 3; y++) {
                        BlockDisplayHandle rift = displayBuilder.spawnBlock(
                            riftLoc.clone().add(0, y, 0), Material.PURPLE_STAINED_GLASS);
                        rift.scale(5.0f, 1.0f, 0.5f).glow(80, 0, 160).interpolation(2, 0);
                        riftHandles.add(rift);
                        spawnedEntities.add(rift.entity());
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 15.0);
            }
            // Rifts active — pulling (10-210 ticks = 10 seconds)
            if (riftsOpen && ticksAlive < 210 && ticksAlive % 10 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (Math.PI * 2 / 8) * i;
                    Location pullLoc = center.clone().add(Math.cos(angle) * 12, 1.5, Math.sin(angle) * 12);
                    DisplayBuilder.cyanDust(pullLoc, 4, 3.0);
                }
            }
            // Rifts close simultaneously (tick 210)
            if (riftsOpen && ticksAlive == 210) {
                for (BlockDisplayHandle rift : riftHandles) {
                    DisplayBuilder.cyanDust(rift.entity().getLocation(), 6, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidmawEchoMassRift(plugin); }
    }

    // ================================================================
    // #104 — WING SHOCKWAVE — Downward hemisphere pressure wave
    // ================================================================
    public static class WingShockwave extends BossAttack {
        private final List<BlockDisplayHandle> waveHandles = new ArrayList<>();
        private boolean waveReleased = false;

        public WingShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_wing_shockwave_p3", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(20.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(240);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 3.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !waveReleased) {
                waveReleased = true;
                // Hemisphere wave
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    Location wavePt = center.clone().add(Math.cos(angle) * 3, 15, Math.sin(angle) * 3);
                    BlockDisplayHandle wave = displayBuilder.spawnBlock(wavePt, Material.WHITE_STAINED_GLASS);
                    wave.scale(3.0f, 3.0f, 1.0f).glow(200, 200, 255).interpolation(1, 0);
                    waveHandles.add(wave);
                    spawnedEntities.add(wave.entity());
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 30, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.6f);
            }
            // Wave expands (6-40 ticks)
            if (waveReleased && ticksAlive < 40) {
                float dist = (ticksAlive - 6) * 0.7f;
                for (int i = 0; i < waveHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    waveHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (3 + dist), 15 - dist * 0.3, Math.sin(angle) * (3 + dist)));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WingShockwave(plugin); }
    }

    // ================================================================
    // #105 — DoG ECHO: CRYSTAL PRISON — 3 simultaneous prisons
    // ================================================================
    public static class DoGEchoCrystalPrison extends BossAttack {
        private final List<BlockDisplayHandle> prisonHandles = new ArrayList<>();
        private boolean prisonsFormed = false;
        private int formTick = 0;

        public DoGEchoCrystalPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_dog_echo_crystal_prison", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !prisonsFormed) {
                prisonsFormed = true;
                formTick = ticksAlive;
                // 3 prisons at random offsets
                for (int p = 0; p < 3; p++) {
                    double ox = (Math.random() - 0.5) * 20;
                    double oz = (Math.random() - 0.5) * 20;
                    Location prisonLoc = center.clone().add(ox, 0.1, oz);
                    // 3x3x3 cage walls
                    for (int face = 0; face < 6; face++) {
                        double[] offsets = {
                            face < 2 ? (face == 0 ? -1.5 : 1.5) : 0,
                            face >= 2 && face < 4 ? (face == 2 ? -0.1 : 3.1) : 1.5,
                            face >= 4 ? (face == 4 ? -1.5 : 1.5) : 0
                        };
                        BlockDisplayHandle wall = displayBuilder.spawnBlock(
                            prisonLoc.clone().add(offsets[0], offsets[1], offsets[2]), Material.AMETHYST_BLOCK);
                        wall.scale(face < 2 ? 0.3f : 3.0f, face >= 2 && face < 4 ? 0.3f : 3.0f, face >= 4 ? 0.3f : 3.0f)
                            .glow(0, 200, 255).interpolation(2, 0);
                        prisonHandles.add(wall);
                        spawnedEntities.add(wall.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_PLACE, 2.0f, 0.6f);
            }
            // Prisons active — resonance damage (10-170 ticks = 8 seconds)
            if (prisonsFormed && ticksAlive - formTick < 160) {
                if (ticksAlive % 20 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 1.5, 0), 6, 10.0);
                }
                if (ticksAlive % 30 == 0) {
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.5f, 0.9f);
                }
            }
            // Prisons dissolve (tick 170)
            if (prisonsFormed && ticksAlive - formTick == 160) {
                for (BlockDisplayHandle wall : prisonHandles) {
                    DisplayBuilder.cyanDust(wall.entity().getLocation(), 4, 1.0);
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 2.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DoGEchoCrystalPrison(plugin); }
    }

    // ================================================================
    // #106 — STREAM CONVERGENCE II: STORM GRID — 25 bolts from merged streams
    // ================================================================
    public static class StreamConvergenceStormGrid extends BossAttack {
        private final List<BlockDisplayHandle> gridHandles = new ArrayList<>();
        private boolean gridFired = false;

        public StreamConvergenceStormGrid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_stream_convergence_storm_grid", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All streams converge overhead
            BlockDisplayHandle mass = displayBuilder.spawnBlock(
                center.clone().add(0, 35, 0), Material.GLOWSTONE);
            mass.scale(6.0f, 6.0f, 6.0f).glow(255, 255, 255).interpolation(3, 0);
            gridHandles.add(mass);
            spawnedEntities.add(mass.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 3.0f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive < 10) {
                DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 12, 5.0);
            }
            // Grid fires — 5x5 bolt pattern (tick 10)
            else if (ticksAlive == 10 && !gridFired) {
                gridFired = true;
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location boltLoc = center.clone().add(x * 4, 0.2, z * 4);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.GLOWSTONE);
                        bolt.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 200).interpolation(1, 0);
                        gridHandles.add(bolt);
                        spawnedEntities.add(bolt.entity());
                        triggerImpactDamage(boltLoc);
                    }
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 5, 0), 30, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 1.0f);
            }
            // Stream regeneration (10-90 ticks = 4 seconds)
            if (gridFired && ticksAlive < 90 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 25, 0), 6, 8.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new StreamConvergenceStormGrid(plugin); }
    }

    // ================================================================
    // #107 — VOID SIPHON — Gravitational upward pull + teleport scatter
    // ================================================================
    public static class VoidSiphon extends BossAttack {
        private final List<BlockDisplayHandle> siphonHandles = new ArrayList<>();
        private boolean siphonActive = false;
        private int siphonTick = 0;

        public VoidSiphon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_siphon", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Large void aperture above center
            BlockDisplayHandle aperture = displayBuilder.spawnBlock(
                center.clone().add(0, 15, 0), Material.OBSIDIAN);
            aperture.scale(6.0f, 6.0f, 6.0f).glow(20, 0, 40).interpolation(3, 0);
            siphonHandles.add(aperture);
            spawnedEntities.add(aperture.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 3.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 8 && !siphonActive) {
                siphonActive = true;
                siphonTick = ticksAlive;
            }
            // Siphon pulls upward (8-88 ticks = 4 seconds)
            if (siphonActive && ticksAlive - siphonTick < 80) {
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, 6.0);
                }
            }
            // Siphon collapses — teleport scatter (tick 88)
            if (siphonActive && ticksAlive - siphonTick == 80) {
                triggerImpactDamage(center.clone().add(0, 15, 0));
                DisplayBuilder.cyanDust(center.clone().add(0, 15, 0), 25, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 0.6f);
            }
            // Pull ambient
            if (siphonActive && ticksAlive - siphonTick < 80 && ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 2.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSiphon(plugin); }
    }

    // ================================================================
    // #108 — PHANTOM BARRAGE — 30 homing phantom bolts, rapid fire
    // ================================================================
    public static class PhantomBarrage extends BossAttack {
        private final List<BlockDisplayHandle> boltHandles = new ArrayList<>();
        private int boltsFired = 0;

        public PhantomBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_phantom_barrage", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(1.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Body pulse telegraph
            DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 20, 5.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Rapid fire: 3 bolts every tick for ~10 ticks (4-14 ticks)
            if (ticksAlive >= 4 && ticksAlive < 24 && boltsFired < 30) {
                if (ticksAlive % 2 == 0) {
                    for (int b = 0; b < 3 && boltsFired < 30; b++) {
                        boltsFired++;
                        double angle = Math.random() * Math.PI * 2;
                        Location boltLoc = center.clone().add(Math.cos(angle) * 3, 10, Math.sin(angle) * 3);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.PURPLE_STAINED_GLASS);
                        bolt.scale(0.3f, 0.3f, 0.6f).glow(80, 0, 120).interpolation(1, 0);
                        boltHandles.add(bolt);
                        spawnedEntities.add(bolt.entity());
                    }
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2.0f, 1.4f);
                }
            }
            // Bolts travel outward and track
            if (boltsFired > 0) {
                for (int i = 0; i < boltHandles.size(); i++) {
                    int boltAge = ticksAlive - (4 + (i / 3) * 2);
                    if (boltAge > 0 && boltAge < 80) {
                        double angle = i * 0.2 + boltAge * 0.05;
                        float dist = boltAge * 0.4f;
                        boltHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(angle) * (3 + dist), 10 - dist * 0.1, Math.sin(angle) * (3 + dist)));
                    }
                }
            }
            // Bolts expire (tick 84)
            if (ticksAlive == 84) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 1.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomBarrage(plugin); }
    }

    // ================================================================
    // #109 — ALL ESSENCES: THE TRIDENT — Triple-component projectile
    // ================================================================
    public static class AllEssencesTrident extends BossAttack {
        private final List<BlockDisplayHandle> tridentHandles = new ArrayList<>();
        private boolean tridentFired = false;

        public AllEssencesTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_all_essences_trident", AttackType.BOSS, 4), "dragon");
            config.setDamage(24.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(360);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3 orbiting essence spheres
            Material[] mats = {Material.PURPLE_STAINED_GLASS, Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK};
            int[][] colors = {{100, 0, 200}, {0, 200, 255}, {200, 0, 100}};
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 / 3) * i;
                Location sphereLoc = center.clone().add(Math.cos(angle) * 2, 12, Math.sin(angle) * 2);
                BlockDisplayHandle sphere = displayBuilder.spawnBlock(sphereLoc, mats[i]);
                sphere.scale(1.0f, 1.0f, 1.0f).glow(colors[i][0], colors[i][1], colors[i][2]).interpolation(2, 0);
                tridentHandles.add(sphere);
                spawnedEntities.add(sphere.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 2.5f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Spheres orbit (0-10 ticks)
            if (ticksAlive < 10 && !tridentFired) {
                for (int i = 0; i < 3; i++) {
                    double angle = (Math.PI * 2 / 3) * i + ticksAlive * 0.3;
                    tridentHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 2, 12, Math.sin(angle) * 2));
                }
            }
            // Trident fires — 3 bolts in tight spread (tick 10)
            else if (ticksAlive == 10 && !tridentFired) {
                tridentFired = true;
                Material[] boltMats = {Material.PURPLE_STAINED_GLASS, Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK};
                for (int i = 0; i < 3; i++) {
                    double angle = (i - 1) * 0.09;
                    Location boltLoc = center.clone().add(Math.cos(angle) * 5, 10, Math.sin(angle) * 5);
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, boltMats[i]);
                    bolt.scale(0.4f, 0.4f, 1.5f).glow(200, 100, 255).interpolation(1, 0);
                    tridentHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 2.5f, 0.9f);
            }
            // Bolts travel (10-50 ticks)
            if (tridentFired && ticksAlive < 50) {
                float dist = (ticksAlive - 10) * 0.8f;
                for (int i = 3; i < tridentHandles.size(); i++) {
                    double angle = (i - 4) * 0.09;
                    tridentHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (5 + dist), 10 - dist * 0.15, Math.sin(angle) * (5 + dist)));
                }
            }
            // Impact — each component hits (tick 50)
            if (tridentFired && ticksAlive == 50) {
                triggerImpactDamage(center.clone().add(20, 2, 0));
                DisplayBuilder.cyanDust(center.clone().add(20, 2, 0), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 1.5f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ITEM_FIRECHARGE_USE, 2.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AllEssencesTrident(plugin); }
    }

    // ================================================================
    // #110 — CELESTIAL RAIN — Nether Star ItemDisplays detach and fall
    // ================================================================
    public static class CelestialRain extends BossAttack {
        private final List<BlockDisplayHandle> starHandles = new ArrayList<>();
        private int wavesComplete = 0;

        public CelestialRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_celestial_rain", AttackType.BOSS, 4), "dragon");
            config.setDamage(28.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 20 stars jitter
            for (int i = 0; i < 20; i++) {
                double ox = (Math.random() - 0.5) * 30;
                double oz = (Math.random() - 0.5) * 30;
                Location starLoc = center.clone().add(ox, 25 + Math.random() * 10, oz);
                BlockDisplayHandle star = displayBuilder.spawnBlock(starLoc, Material.GLOWSTONE);
                star.scale(0.8f, 0.8f, 0.8f).glow(255, 255, 200).interpolation(2, 0);
                starHandles.add(star);
                spawnedEntities.add(star.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.5f, 1.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Wave 1 — all 20 fall (tick 16)
            if (ticksAlive == 16 && wavesComplete == 0) {
                wavesComplete = 1;
            }
            // Stars descend (16-56 ticks = 2 seconds of fall)
            if (wavesComplete >= 1 && ticksAlive >= 16 && ticksAlive < 56) {
                float fallDist = (ticksAlive - 16) * 0.8f;
                for (BlockDisplayHandle star : starHandles) {
                    Location loc = star.entity().getLocation();
                    star.entity().teleport(loc.clone().add(0, -fallDist * 0.05, 0));
                }
            }
            // Wave 1 impacts (tick 56)
            if (ticksAlive == 56 && wavesComplete == 1) {
                for (BlockDisplayHandle star : starHandles) {
                    Location impactLoc = star.entity().getLocation();
                    impactLoc.setY(center.getY() + 0.2);
                    triggerImpactDamage(impactLoc);
                    DisplayBuilder.cyanDust(impactLoc, 6, 2.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 3.0f, 1.0f);
            }
            // Wave 2 — stars respawn and fall again (tick 96)
            if (ticksAlive == 96 && wavesComplete == 1) {
                wavesComplete = 2;
                for (BlockDisplayHandle star : starHandles) {
                    double ox = (Math.random() - 0.5) * 30;
                    double oz = (Math.random() - 0.5) * 30;
                    star.entity().teleport(center.clone().add(ox, 28 + Math.random() * 8, oz));
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.8f);
            }
            // Wave 2 descend (96-136)
            if (wavesComplete == 2 && ticksAlive >= 96 && ticksAlive < 136) {
                float fallDist = (ticksAlive - 96) * 0.8f;
                for (BlockDisplayHandle star : starHandles) {
                    Location loc = star.entity().getLocation();
                    star.entity().teleport(loc.clone().add(0, -fallDist * 0.05, 0));
                }
            }
            // Wave 2 impacts (tick 136)
            if (ticksAlive == 136 && wavesComplete == 2) {
                for (int i = 0; i < starHandles.size(); i++) {
                    Location impactLoc = starHandles.get(i).entity().getLocation();
                    impactLoc.setY(center.getY() + 0.2);
                    triggerImpactDamage(impactLoc);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 3.0f, 1.0f);
            }
            // Light pillars aftermath (56-176 ticks = 6 seconds)
            if (wavesComplete >= 1 && ticksAlive > 56 && ticksAlive < 176 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 4, 0), 6, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CelestialRain(plugin); }
    }

    // ================================================================
    // #111 — VOID SPLIT — Dragon creates 1 visual clone, subtle tell
    // ================================================================
    public static class VoidSplit extends BossAttack {
        private final List<BlockDisplayHandle> splitHandles = new ArrayList<>();
        private boolean splitActive = false;

        public VoidSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_split", AttackType.BOSS, 4), "dragon");
            config.setDamage(26.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 15, 4.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !splitActive) {
                splitActive = true;
                // Clone creation — PORTAL particles (tell: clone has PORTAL wingtips)
                BlockDisplayHandle clone = displayBuilder.spawnBlock(
                    center.clone().add(-10, 10, 0), Material.OBSIDIAN);
                clone.scale(4.0f, 5.0f, 4.0f).glow(80, 0, 120).interpolation(2, 0);
                splitHandles.add(clone);
                spawnedEntities.add(clone.entity());
                // Real dragon marker (DRAGON_BREATH wingtips)
                BlockDisplayHandle real = displayBuilder.spawnBlock(
                    center.clone().add(10, 10, 0), Material.OBSIDIAN);
                real.scale(4.0f, 5.0f, 4.0f).glow(40, 80, 40).interpolation(2, 0);
                splitHandles.add(real);
                spawnedEntities.add(real.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 2.0f, 1.4f);
            }
            // Both charge simultaneously from opposite sides (6-26 ticks)
            if (splitActive && ticksAlive > 6 && ticksAlive < 26) {
                float progress = (ticksAlive - 6) / 20.0f;
                if (splitHandles.size() >= 2) {
                    splitHandles.get(0).entity().teleport(
                        center.clone().add(-10 + progress * 20, 10 - progress * 8, 0));
                    splitHandles.get(1).entity().teleport(
                        center.clone().add(10 - progress * 20, 10 - progress * 8, 0));
                }
            }
            // Real dragon hits (tick 26)
            if (splitActive && ticksAlive == 26) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.5f, 1.0f);
            }
            // Clone shatters (tick 30)
            if (splitActive && ticksAlive == 30) {
                DisplayBuilder.cyanDust(center.clone().add(-2, 2, 0), 12, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_HURT, 1.5f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidSplit(plugin); }
    }

    // ================================================================
    // #112 — GROUND ZERO BREATH — 360-degree rotating ground breath
    // ================================================================
    public static class GroundZeroBreath extends BossAttack {
        private final List<BlockDisplayHandle> breathHandles = new ArrayList<>();
        private boolean breathActive = false;
        private int breathTick = 0;

        public GroundZeroBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_ground_zero_breath", AttackType.BOSS, 4), "dragon");
            config.setDamage(30.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon descends near ground
            BlockDisplayHandle jawCharge = displayBuilder.spawnBlock(
                center.clone().add(0, 3, 0), Material.PURPLE_STAINED_GLASS);
            jawCharge.scale(2.0f, 2.0f, 2.0f).glow(100, 0, 200).interpolation(2, 0);
            breathHandles.add(jawCharge);
            spawnedEntities.add(jawCharge.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 8 && !breathActive) {
                breathActive = true;
                breathTick = ticksAlive;
            }
            // Breath sweeps 360 degrees (8-128 ticks = 6 seconds)
            if (breathActive && ticksAlive - breathTick < 120) {
                float rotAngle = ((ticksAlive - breathTick) / 120.0f) * (float)(Math.PI * 2);
                double bx = Math.cos(rotAngle) * 12;
                double bz = Math.sin(rotAngle) * 12;
                Location breathEnd = center.clone().add(bx, 1.5, bz);
                if (ticksAlive % 3 == 0) {
                    BlockDisplayHandle breathSeg = displayBuilder.spawnBlock(breathEnd, Material.PURPLE_STAINED_GLASS);
                    breathSeg.scale(8.0f, 3.0f, 1.0f).glow(80, 0, 140).interpolation(1, 0);
                    spawnedEntities.add(breathSeg.entity());
                    triggerImpactDamage(breathEnd);
                    DisplayBuilder.cyanDust(breathEnd, 4, 3.0);
                }
            }
            // Breath sound loop
            if (breathActive && ticksAlive % 20 == 0 && ticksAlive - breathTick < 120) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 3.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GroundZeroBreath(plugin); }
    }

    // ================================================================
    // #113 — SKY COLLAPSE — All sky particles fall as meteor shower
    // ================================================================
    public static class SkyCollapse extends BossAttack {
        private final List<BlockDisplayHandle> fragmentHandles = new ArrayList<>();
        private boolean collapseFired = false;

        public SkyCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_sky_collapse", AttackType.BOSS, 4), "dragon");
            config.setDamage(22.0);
            config.setDamageRadius(0.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(500);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All streams halt
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_DEACTIVATE, 3.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Streams freeze (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !collapseFired) {
                DisplayBuilder.cyanDust(center.clone().add(0, 35, 0), 15, 20.0);
            }
            // Fragments released (tick 10)
            else if (ticksAlive == 10 && !collapseFired) {
                collapseFired = true;
                // Simulate 50 falling fragments
                Material[] fragMats = {Material.MAGENTA_STAINED_GLASS, Material.YELLOW_STAINED_GLASS, Material.WHITE_STAINED_GLASS};
                for (int i = 0; i < 50; i++) {
                    double ox = (Math.random() - 0.5) * 30;
                    double oz = (Math.random() - 0.5) * 30;
                    Location fragLoc = center.clone().add(ox, 30 + Math.random() * 10, oz);
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(fragLoc, fragMats[i % 3]);
                    frag.scale(0.3f, 0.3f, 0.3f).glow(255, 255, 200).interpolation(1, 0);
                    fragmentHandles.add(frag);
                    spawnedEntities.add(frag.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.4f);
            }
            // Fragments fall (10-40 ticks = 1.5 seconds)
            if (collapseFired && ticksAlive < 40) {
                float fallDist = (ticksAlive - 10) * 1.0f;
                for (BlockDisplayHandle frag : fragmentHandles) {
                    Location loc = frag.entity().getLocation();
                    frag.entity().teleport(loc.clone().add(0, -fallDist * 0.05, 0));
                }
            }
            // Landing impacts (tick 40)
            if (collapseFired && ticksAlive == 40) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 25, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 0.9f);
            }
            // Streams rebuild (40-140 ticks = 5 seconds)
            if (collapseFired && ticksAlive > 40 && ticksAlive < 140 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 6, 15.0);
            }
            if (collapseFired && ticksAlive == 140) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SkyCollapse(plugin); }
    }

    // ================================================================
    // #114 — PURSUIT — 8-second active tracking at 250% speed
    // ================================================================
    public static class Pursuit extends BossAttack {
        private final List<BlockDisplayHandle> pursuitHandles = new ArrayList<>();
        private boolean pursuing = false;
        private int pursuitTick = 0;

        public Pursuit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_pursuit", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // No telegraph — instant pursuit
            pursuing = true;
            pursuitTick = 0;
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_STARE, 2.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Active pursuit (0-160 ticks = 8 seconds)
            if (pursuing && ticksAlive < 160) {
                // Dragon chases in a tracking spiral
                double chaseAngle = ticksAlive * 0.08;
                double radius = 8 + Math.sin(ticksAlive * 0.05) * 4;
                Location chasePt = center.clone().add(
                    Math.cos(chaseAngle) * radius, 6, Math.sin(chaseAngle) * radius);
                if (ticksAlive % 4 == 0) {
                    BlockDisplayHandle trail = displayBuilder.spawnBlock(chasePt, Material.WHITE_STAINED_GLASS);
                    trail.scale(1.5f, 2.0f, 1.5f).glow(255, 255, 255).interpolation(1, 0);
                    pursuitHandles.add(trail);
                    spawnedEntities.add(trail.entity());
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.5f, 1.3f);
                }
            }
            // Final slam (tick 160)
            if (pursuing && ticksAlive == 160) {
                pursuing = false;
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Pursuit(plugin); }
    }

    // ================================================================
    // #115 — VOID COLLAPSE SPHERE — Charged gravitational explosion
    // ================================================================
    public static class VoidCollapseSphere extends BossAttack {
        private final List<BlockDisplayHandle> sphereHandles = new ArrayList<>();
        private boolean charging = false;
        private boolean detonated = false;
        private int chargeTick = 0;

        public VoidCollapseSphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_collapse_sphere", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(12.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon hovers, sphere begins forming
            BlockDisplayHandle sphereCore = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 0), Material.OBSIDIAN);
            sphereCore.scale(0.5f, 0.5f, 0.5f).glow(20, 0, 40).interpolation(4, 0);
            sphereHandles.add(sphereCore);
            spawnedEntities.add(sphereCore.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 10 && !charging) {
                charging = true;
                chargeTick = ticksAlive;
            }
            // Sphere grows over 3 seconds (10-70 ticks)
            if (charging && !detonated && ticksAlive - chargeTick < 60) {
                float progress = (ticksAlive - chargeTick) / 60.0f;
                float scale = 0.5f + progress * 9.5f;
                sphereHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 10 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, scale);
                }
            }
            // Sphere detonates (tick 70)
            else if (charging && !detonated && ticksAlive - chargeTick == 60) {
                detonated = true;
                // Massive explosion
                for (int i = 0; i < 20; i++) {
                    double angle = (Math.PI * 2 / 20) * i;
                    for (int layer = 0; layer < 2; layer++) {
                        double yAngle = (layer - 0.5) * 0.5;
                        Location burstPt = center.clone().add(
                            Math.cos(angle) * 12, 10 + Math.sin(yAngle) * 8, Math.sin(angle) * 12);
                        BlockDisplayHandle burst = displayBuilder.spawnBlock(burstPt, Material.PURPLE_STAINED_GLASS);
                        burst.scale(3.0f, 3.0f, 3.0f).glow(100, 0, 200).interpolation(1, 0);
                        spawnedEntities.add(burst.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 50, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidCollapseSphere(plugin); }
    }
}
