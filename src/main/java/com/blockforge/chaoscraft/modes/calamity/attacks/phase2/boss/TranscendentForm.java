package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.boss;

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
 * Phase 2 Boss Attacks - GROUP 9: TRANSCENDENT FORM (#81-90)
 * Attacks from DoG's ethereal Phase 2 form - phase-through strikes,
 * ghostly mechanics, attacks from outside normal space.
 * NO status effects - damage only.
 */
public final class TranscendentForm {

    private TranscendentForm() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PhaseThrough(plugin));
        registry.register(new SpectralEchoCascade(plugin));
        registry.register(new VoidBodyStrike(plugin));
        registry.register(new CosmicManifest(plugin));
        registry.register(new DimensionalReach(plugin));
        registry.register(new TranscendentPhase(plugin));
        registry.register(new AfterimageStorm(plugin));
        registry.register(new SoulDrainAura(plugin));
        registry.register(new GodsContempt(plugin));
        registry.register(new TranscendenceOverload(plugin));
    }

    // ================================================================
    // 81. PHASE THROUGH - DoG becomes translucent, passes through terrain
    // 4-sec ghost state then immediate re-solidification charge
    // ================================================================
    public static class PhaseThrough extends BossAttack {
        private final List<BlockDisplayHandle> ghostHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> solidHandles = new ArrayList<>();
        private boolean phased = false;
        private boolean resolidified = false;

        public PhaseThrough(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_through", AttackType.BOSS, 2), "dog");
            config.setDamage(20.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG segments fading to translucent
            for (int seg = 0; seg < 8; seg++) {
                Location segLoc = center.clone().add(seg * 2 - 7, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.CYAN_STAINED_GLASS);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(4, 0);
                ghostHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Fade transition (0-20 ticks = 1 sec telegraph)
            if (ticksAlive < 20 && !phased) {
                if (ticksAlive % 5 == 0) {
                    for (BlockDisplayHandle h : ghostHandles) {
                        float scale = 1.0f - (ticksAlive / 20.0f) * 0.4f;
                        BlockDisplay bd = (BlockDisplay) h.entity();
                        bd.setTransformation(new Transformation(
                            new Vector3f(-scale / 2, -scale / 2, -scale / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scale, scale, scale),
                            new AxisAngle4f(0, 0, 1, 0)));
                        bd.setInterpolationDelay(0);
                        bd.setInterpolationDuration(3);
                    }
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 6, 4.0);
                }
            }
            // Ghost phase active (20-100 ticks = 4 sec)
            else if (ticksAlive == 20 && !phased) {
                phased = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);
            }
            // Ghost orbits arena
            else if (phased && !resolidified && ticksAlive < 100) {
                float angle = (ticksAlive - 20) * 0.08f;
                float radius = 10.0f;
                for (int seg = 0; seg < ghostHandles.size(); seg++) {
                    double segAngle = angle - seg * 0.3;
                    Location orbitLoc = center.clone().add(
                        Math.cos(segAngle) * radius, 2, Math.sin(segAngle) * radius);
                    ghostHandles.get(seg).entity().teleport(orbitLoc);
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(
                        Math.cos(angle) * radius, 2, Math.sin(angle) * radius), 4, 1.0);
                }
            }
            // Re-solidification charge (tick 100)
            else if (ticksAlive == 100 && !resolidified) {
                resolidified = true;
                // Remove ghost
                for (BlockDisplayHandle h : ghostHandles) h.entity().remove();
                ghostHandles.clear();

                // Solid body charge
                double chargeAngle = Math.random() * Math.PI * 2;
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(
                        Math.cos(chargeAngle) * (10 - seg), 2, Math.sin(chargeAngle) * (10 - seg));
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.2f, 1.2f, 1.2f).glow(0, 200, 255).interpolation(1, 0);
                    solidHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 6.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 1.0f);
            }
            // Charge sweeps through (100-120 ticks)
            else if (resolidified && ticksAlive < 120) {
                float progress = (ticksAlive - 100) / 20.0f;
                for (int seg = 0; seg < solidHandles.size(); seg++) {
                    Location segLoc = center.clone().add((seg - 4) * 2, 2, progress * 15);
                    solidHandles.get(seg).entity().teleport(segLoc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhaseThrough(plugin); }
    }

    // ================================================================
    // 82. SPECTRAL ECHO CASCADE - Translucent ghost-copies persist
    // where DoG was. Arena fills with damaging afterimages for 6 sec.
    // ================================================================
    public static class SpectralEchoCascade extends BossAttack {
        private final List<BlockDisplayHandle> echoHandles = new ArrayList<>();
        private int echoWavesSpawned = 0;

        public SpectralEchoCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_echo_cascade", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(320);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Echoes form every 10 ticks for 12 waves (0-120 ticks = 6 sec)
            int waveIdx = ticksAlive / 10;
            if (waveIdx >= 0 && waveIdx < 12 && echoWavesSpawned <= waveIdx && ticksAlive % 10 == 0) {
                echoWavesSpawned = waveIdx + 1;

                // Current DoG position along orbit path
                double angle = waveIdx * 0.5;
                double radius = 8 + Math.sin(waveIdx * 0.7) * 4;
                Location dogPos = center.clone().add(Math.cos(angle) * radius, 2, Math.sin(angle) * radius);

                // Spawn echo (4 segments of ghostly glass)
                for (int seg = 0; seg < 4; seg++) {
                    Location segLoc = dogPos.clone().add(
                        Math.cos(angle + Math.PI) * seg * 1.5, 0, Math.sin(angle + Math.PI) * seg * 1.5);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.CYAN_STAINED_GLASS);
                    // Newer echoes are brighter; older fade
                    int brightness = 255 - (11 - waveIdx) * 15;
                    if (brightness < 80) brightness = 80;
                    h.scale(0.8f, 0.8f, 0.8f).glow(0, brightness, 255).interpolation(2, 0);
                    echoHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(dogPos, 6, 1.5);
            }
            // Oldest echoes fade out (after first 6 waves)
            if (echoWavesSpawned > 6 && ticksAlive % 10 == 5 && echoHandles.size() > 24) {
                for (int i = 0; i < 4 && !echoHandles.isEmpty(); i++) {
                    echoHandles.get(0).entity().remove();
                    echoHandles.remove(0);
                }
            }
            // Lingering particles
            if (ticksAlive % 15 == 0 && !echoHandles.isEmpty()) {
                for (int i = 0; i < echoHandles.size(); i += 4) {
                    DisplayBuilder.cyanDust(echoHandles.get(i).entity().getLocation(), 2, 0.5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpectralEchoCascade(plugin); }
    }

    // ================================================================
    // 83. VOID BODY STRIKE - DoG fully invisible for 3 sec
    // Delayed ELECTRIC_SPARK flashes show where it was, not where it is
    // ================================================================
    public static class VoidBodyStrike extends BossAttack {
        private final List<BlockDisplayHandle> trailHandles = new ArrayList<>();
        private boolean invisible = false;
        private boolean reappeared = false;

        public VoidBodyStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_body_strike", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(480);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Sound cue only (0.8 sec deep distortion)
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Sound cue telegraph (0-16 ticks = 0.8 sec)
            if (ticksAlive < 16 && !invisible) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 2, 0), 4, 3.0);
                }
            }
            // DoG vanishes (tick 16)
            else if (ticksAlive == 16 && !invisible) {
                invisible = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.3f);
            }
            // Invisible phase (16-76 ticks = 3 sec): after-contact flashes
            else if (invisible && !reappeared && ticksAlive < 76) {
                // Delayed trail flashes showing where DoG WAS (0.5 sec behind)
                if (ticksAlive % 5 == 0) {
                    double angle = (ticksAlive - 16) * 0.1;
                    double radius = 8;
                    // DoG's actual position (invisible)
                    Location actualPos = center.clone().add(
                        Math.cos(angle) * radius, 2, Math.sin(angle) * radius);
                    // Flash at delayed position (half second behind)
                    double delayedAngle = angle - 0.5;
                    Location flashPos = center.clone().add(
                        Math.cos(delayedAngle) * radius, 2, Math.sin(delayedAngle) * radius);

                    BlockDisplayHandle h = displayBuilder.spawnBlock(flashPos, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.3f, 0.3f).glow(240, 240, 255).interpolation(1, 0);
                    trailHandles.add(h);
                    spawnedEntities.add(h.entity());

                    DisplayBuilder.cyanDust(flashPos, 5, 0.8);
                }
                // Fade old flashes
                if (trailHandles.size() > 6) {
                    trailHandles.get(0).entity().remove();
                    trailHandles.remove(0);
                }
            }
            // DoG reappears (tick 76)
            else if (ticksAlive == 76 && !reappeared) {
                reappeared = true;
                // Reappearance flash
                for (BlockDisplayHandle h : trailHandles) h.entity().remove();
                trailHandles.clear();

                for (int seg = 0; seg < 6; seg++) {
                    Location segLoc = center.clone().add(seg * 2 - 5, 2, 3);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    trailHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 3), 25, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBodyStrike(plugin); }
    }

    // ================================================================
    // 84. COSMIC MANIFEST - DoG visually expands 300% but hitbox unchanged
    // Massive illusion causes over-dodging; real hitbox is normal size
    // ================================================================
    public static class CosmicManifest extends BossAttack {
        private final List<BlockDisplayHandle> expandedHandles = new ArrayList<>();
        private boolean expanded = false;

        public CosmicManifest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cosmic_manifest", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Bass pulse before expansion
            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.2f);
            // Brief flare across all segments
            for (int seg = 0; seg < 6; seg++) {
                Location segLoc = center.clone().add(seg * 2 - 5, 2, 0);
                DisplayBuilder.cyanDust(segLoc, 8, 1.0);
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Bass pulse telegraph (0-20 ticks = 1 sec)
            if (ticksAlive < 20 && !expanded) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 10, 5.0);
                }
            }
            // Expansion (tick 20): visual scale triples
            else if (ticksAlive == 20 && !expanded) {
                expanded = true;
                // Massive 300% visual body
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(seg * 4 - 14, 2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(3.0f, 3.0f, 3.0f).glow(0, 200, 255).interpolation(3, 0);
                    expandedHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 40, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.3f);
            }
            // Expanded charge (20-60 ticks = 2 sec)
            else if (expanded && ticksAlive > 20 && ticksAlive < 60) {
                float progress = (ticksAlive - 20) / 40.0f;
                for (int seg = 0; seg < expandedHandles.size(); seg++) {
                    double x = (seg * 4 - 14) + progress * 20;
                    expandedHandles.get(seg).entity().teleport(center.clone().add(x, 2, 0));
                }
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(progress * 20 - 5, 2, 0), 12, 4.0);
                }
            }
            // Return to normal size (tick 60)
            else if (ticksAlive == 60) {
                for (BlockDisplayHandle h : expandedHandles) h.entity().remove();
                expandedHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 15, 5.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CosmicManifest(plugin); }
    }

    // ================================================================
    // 85. DIMENSIONAL REACH - Rifts at player positions, spectral jaws snap
    // 3 sequential rifts, 1 sec apart, jaws close on fixed position
    // ================================================================
    public static class DimensionalReach extends BossAttack {
        private final List<BlockDisplayHandle> riftHandles = new ArrayList<>();
        private int riftsSpawned = 0;

        public DimensionalReach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_reach", AttackType.BOSS, 2), "dog");
            config.setDamage(22.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // 3 rifts, each 20 ticks apart
            int riftIdx = ticksAlive / 20;
            if (riftIdx >= 0 && riftIdx < 3 && riftsSpawned <= riftIdx && ticksAlive % 20 == 0) {
                riftsSpawned = riftIdx + 1;

                // Rift at random offset near center
                double angle = Math.random() * Math.PI * 2;
                double dist = 3 + Math.random() * 8;
                Location riftLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Portal buildup at feet
                for (int r = 0; r < 4; r++) {
                    double a = (Math.PI * 2 / 4) * r;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                        riftLoc.clone().add(Math.cos(a) * 0.8, 0.05, Math.sin(a) * 0.8), Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 0.1f, 0.4f).glow(0, 200, 255).interpolation(2, 0);
                    riftHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(riftLoc.clone().add(0, 0.5, 0), 10, 1.0);
                DisplayBuilder.playSound(riftLoc, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.0f);
            }

            // Jaw snap events at each rift (20 ticks after rift forms)
            for (int r = 0; r < riftsSpawned; r++) {
                int snapTick = (r + 1) * 20 + 20;
                if (ticksAlive == snapTick) {
                    // Spawn jaw blocks clamping shut
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 3 + r * 3.5;
                    Location jawLoc = center.clone().add(Math.cos(angle) * dist, 1, Math.sin(angle) * dist);

                    // Upper jaw
                    BlockDisplayHandle upper = displayBuilder.spawnBlock(
                        jawLoc.clone().add(0, 1.5, 0), Material.AMETHYST_BLOCK);
                    upper.scale(1.5f, 0.6f, 1.5f).glow(0, 200, 255).interpolation(1, 0);
                    riftHandles.add(upper);
                    spawnedEntities.add(upper.entity());

                    // Lower jaw
                    BlockDisplayHandle lower = displayBuilder.spawnBlock(
                        jawLoc.clone().add(0, 0.5, 0), Material.AMETHYST_BLOCK);
                    lower.scale(1.5f, 0.6f, 1.5f).glow(0, 200, 255).interpolation(1, 0);
                    riftHandles.add(lower);
                    spawnedEntities.add(lower.entity());

                    DisplayBuilder.cyanDust(jawLoc, 15, 2.0);
                    DisplayBuilder.playSound(jawLoc, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalReach(plugin); }
    }

    // ================================================================
    // 86. TRANSCENDENT PHASE - DoG fully exits dimension
    // 6 portal vortexes appear randomly, then DoG re-emerges with charge
    // ================================================================
    public static class TranscendentPhase extends BossAttack {
        private final List<BlockDisplayHandle> vortexHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> chargeHandles = new ArrayList<>();
        private int vortexesSpawned = 0;
        private boolean reEmerged = false;

        public TranscendentPhase(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("transcendent_phase", AttackType.BOSS, 2), "dog");
            config.setDamage(22.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Audio shift: silence then hum
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.5f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Vortexes appear at 1-sec intervals (20 ticks apart), 6 total
            int vortexIdx = ticksAlive / 20;
            if (vortexIdx >= 0 && vortexIdx < 6 && vortexesSpawned <= vortexIdx && ticksAlive % 20 == 0) {
                vortexesSpawned = vortexIdx + 1;

                double angle = Math.random() * Math.PI * 2;
                double dist = 4 + Math.random() * 10;
                Location vortexLoc = center.clone().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

                // Rotating vortex ring
                for (int i = 0; i < 8; i++) {
                    double a = (Math.PI * 2 / 8) * i;
                    Location ringLoc = vortexLoc.clone().add(Math.cos(a) * 2, 1.5, Math.sin(a) * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(ringLoc, Material.CRYING_OBSIDIAN);
                    h.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                    vortexHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.purpleDust(vortexLoc.clone().add(0, 1.5, 0), 12, 2.5);
                DisplayBuilder.playSound(vortexLoc, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.8f);
            }

            // Rotate vortex rings
            if (vortexesSpawned > 0 && ticksAlive % 3 == 0) {
                float rot = ticksAlive * 0.1f;
                for (int v = 0; v < vortexesSpawned && v * 8 < vortexHandles.size(); v++) {
                    for (int i = 0; i < 8; i++) {
                        int idx = v * 8 + i;
                        if (idx >= vortexHandles.size()) break;
                        double a = rot + (Math.PI * 2 / 8) * i;
                        Location baseLoc = vortexHandles.get(v * 8).entity().getLocation();
                        double cx = baseLoc.getX();
                        double cz = baseLoc.getZ();
                        // Approximate center from first element
                        vortexHandles.get(idx).entity().teleport(
                            baseLoc.clone().add(Math.cos(a) * 2 - Math.cos(rot) * 2, 0,
                                Math.sin(a) * 2 - Math.sin(rot) * 2));
                    }
                }
            }

            // Active vortex damage particles
            if (vortexesSpawned > 0 && ticksAlive % 12 == 0 && !reEmerged) {
                for (int i = 0; i < vortexHandles.size(); i += 8) {
                    DisplayBuilder.purpleDust(vortexHandles.get(i).entity().getLocation(), 5, 2.0);
                }
            }

            // DoG re-emerges (tick 100 = 5 sec)
            if (ticksAlive == 100 && !reEmerged) {
                reEmerged = true;
                // Remove vortexes
                for (BlockDisplayHandle h : vortexHandles) h.entity().remove();
                vortexHandles.clear();

                // Large central rift burst
                for (int seg = 0; seg < 10; seg++) {
                    Location segLoc = center.clone().add(seg * 2 - 9, 2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.3f, 1.3f, 1.3f).glow(0, 200, 255).interpolation(1, 0);
                    chargeHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 40, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.4f);
            }
            // Re-emergence charge (100-120 ticks)
            else if (reEmerged && ticksAlive > 100 && ticksAlive < 120) {
                float progress = (ticksAlive - 100) / 20.0f;
                for (int seg = 0; seg < chargeHandles.size(); seg++) {
                    chargeHandles.get(seg).entity().teleport(
                        center.clone().add((seg * 2 - 9) + progress * 16, 2, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TranscendentPhase(plugin); }
    }

    // ================================================================
    // 87. AFTERIMAGE STORM - Speed triples, dozens of overlapping copies
    // Only one has real hitbox; afterimages deal reduced damage
    // ================================================================
    public static class AfterimageStorm extends BossAttack {
        private final List<BlockDisplayHandle> afterimageHandles = new ArrayList<>();
        private boolean stormActive = false;

        public AfterimageStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("afterimage_storm", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(520);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Full body flare
            for (int seg = 0; seg < 8; seg++) {
                Location segLoc = center.clone().add(seg * 2 - 7, 2, 0);
                DisplayBuilder.cyanDust(segLoc, 10, 1.0);
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Acceleration telegraph (0-20 ticks = 1 sec)
            if (ticksAlive < 20 && !stormActive) {
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 12, 5.0);
                }
            }
            // Storm begins (tick 20)
            else if (ticksAlive == 20 && !stormActive) {
                stormActive = true;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            }
            // Storm active (20-100 ticks = 4 sec): spawn rapid afterimages
            else if (stormActive && ticksAlive < 100) {
                if (ticksAlive % 4 == 0) {
                    // New afterimage at random position
                    double angle = Math.random() * Math.PI * 2;
                    double dist = 3 + Math.random() * 12;
                    Location imgLoc = center.clone().add(Math.cos(angle) * dist, 2, Math.sin(angle) * dist);

                    for (int seg = 0; seg < 3; seg++) {
                        double segAngle = angle + Math.PI;
                        Location segLoc = imgLoc.clone().add(
                            Math.cos(segAngle) * seg * 1.2, 0, Math.sin(segAngle) * seg * 1.2);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.CYAN_STAINED_GLASS);
                        h.scale(0.7f, 0.7f, 0.7f).glow(0, 200, 255).interpolation(1, 0);
                        afterimageHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }

                    // Real DoG (brighter trailing edge, slightly larger)
                    if (ticksAlive % 12 == 0) {
                        BlockDisplayHandle real = displayBuilder.spawnBlock(imgLoc, Material.AMETHYST_BLOCK);
                        real.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                        afterimageHandles.add(real);
                        spawnedEntities.add(real.entity());
                        DisplayBuilder.cyanDust(imgLoc, 6, 1.0);
                    }
                }
                // Fade oldest afterimages
                while (afterimageHandles.size() > 30) {
                    afterimageHandles.get(0).entity().remove();
                    afterimageHandles.remove(0);
                }
            }
            // Storm ends (tick 100)
            else if (ticksAlive == 100) {
                for (BlockDisplayHandle h : afterimageHandles) h.entity().remove();
                afterimageHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AfterimageStorm(plugin); }
    }

    // ================================================================
    // 88. SOUL DRAIN AURA - 10-block-radius field from all body segments
    // Continuous drain, particles flow toward DoG's body
    // ================================================================
    public static class SoulDrainAura extends BossAttack {
        private final List<BlockDisplayHandle> auraHandles = new ArrayList<>();
        private boolean auraActive = false;

        public SoulDrainAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_drain_aura", AttackType.BOSS, 2), "dog");
            config.setDamage(12.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(560);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Aura buildup (0-40 ticks = 2 sec)
            if (ticksAlive < 40 && !auraActive) {
                float intensity = ticksAlive / 40.0f;
                if (ticksAlive % 5 == 0) {
                    // Sparse particles building in density
                    int count = (int)(4 * intensity + 2);
                    for (int i = 0; i < count; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 10 * intensity;
                        DisplayBuilder.cyanDust(center.clone().add(
                            Math.cos(angle) * dist, 2, Math.sin(angle) * dist), 3, 1.0);
                    }
                }
            }
            // Aura activates (tick 40)
            else if (ticksAlive == 40 && !auraActive) {
                auraActive = true;
                // Aura boundary ring
                for (int i = 0; i < 24; i++) {
                    double a = (Math.PI * 2 / 24) * i;
                    Location loc = center.clone().add(Math.cos(a) * 10, 0.1, Math.sin(a) * 10);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                    h.scale(0.5f, 0.1f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
                    auraHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Central body segments
                for (int seg = 0; seg < 6; seg++) {
                    Location segLoc = center.clone().add(seg * 2 - 5, 2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(0.8f, 0.8f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
                    auraHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
            }
            // Aura active (40-200 ticks = 8 sec): particles flow inward
            else if (auraActive && ticksAlive < 200) {
                if (ticksAlive % 6 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double angle = Math.random() * Math.PI * 2;
                        double dist = 8 + Math.random() * 2;
                        Location outerLoc = center.clone().add(
                            Math.cos(angle) * dist, 1 + Math.random() * 2, Math.sin(angle) * dist);
                        DisplayBuilder.cyanDust(outerLoc, 3, 0.5);
                    }
                    // Inner draw particles
                    DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 4, 2.0);
                }
                // Rotate boundary ring slowly
                if (ticksAlive % 4 == 0) {
                    float rot = ticksAlive * 0.02f;
                    for (int i = 0; i < 24 && i < auraHandles.size(); i++) {
                        double a = rot + (Math.PI * 2 / 24) * i;
                        auraHandles.get(i).entity().teleport(
                            center.clone().add(Math.cos(a) * 10, 0.1, Math.sin(a) * 10));
                    }
                }
            }
            // Aura dissipates (tick 200)
            else if (ticksAlive == 200) {
                for (BlockDisplayHandle h : auraHandles) h.entity().remove();
                auraHandles.clear();
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 15, 8.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SoulDrainAura(plugin); }
    }

    // ================================================================
    // 89. GOD'S CONTEMPT - DoG hunts lowest-DPS player for 6 sec
    // Singular pursuit at max speed; other players can interrupt
    // ================================================================
    public static class GodsContempt extends BossAttack {
        private final List<BlockDisplayHandle> pursuitHandles = new ArrayList<>();
        private boolean huntActive = false;

        public GodsContempt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gods_contempt", AttackType.BOSS, 2), "dog");
            config.setDamage(18.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(600);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Targeting indicator: white crosshair
            for (int arm = 0; arm < 4; arm++) {
                double a = (Math.PI / 2) * arm;
                for (int r = 1; r <= 3; r++) {
                    Location loc = center.clone().add(Math.cos(a) * r, 0.05, Math.sin(a) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                    h.scale(0.3f, 0.05f, 0.3f).glow(240, 240, 255).interpolation(2, 0);
                    pursuitHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Targeting telegraph (0-20 ticks = 1 sec)
            if (ticksAlive < 20 && !huntActive) {
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 8, 3.0);
                }
            }
            // Hunt begins (tick 20)
            else if (ticksAlive == 20 && !huntActive) {
                huntActive = true;
                // Remove crosshair
                for (BlockDisplayHandle h : pursuitHandles) h.entity().remove();
                pursuitHandles.clear();

                // DoG body with blazing eyes
                for (int seg = 0; seg < 8; seg++) {
                    Location segLoc = center.clone().add(-10, 2, seg - 4);
                    Material mat = seg == 0 ? Material.SEA_LANTERN : Material.AMETHYST_BLOCK;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, mat);
                    h.scale(1.0f, 1.0f, 1.0f).glow(seg == 0 ? 240 : 0, seg == 0 ? 240 : 200, 255).interpolation(1, 0);
                    pursuitHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.4f);
            }
            // Active pursuit (20-140 ticks = 6 sec): DoG sweeps toward center
            else if (huntActive && ticksAlive > 20 && ticksAlive < 140) {
                float progress = (ticksAlive - 20) / 120.0f;
                double sweepAngle = progress * Math.PI * 3;
                double sweepRadius = 10 - progress * 7;
                for (int seg = 0; seg < pursuitHandles.size(); seg++) {
                    double segAngle = sweepAngle - seg * 0.3;
                    pursuitHandles.get(seg).entity().teleport(
                        center.clone().add(
                            Math.cos(segAngle) * sweepRadius, 2, Math.sin(segAngle) * sweepRadius));
                }
                if (ticksAlive % 8 == 0) {
                    double headAngle = sweepAngle;
                    DisplayBuilder.cyanDust(
                        center.clone().add(Math.cos(headAngle) * sweepRadius, 2,
                            Math.sin(headAngle) * sweepRadius), 6, 1.0);
                }
            }
            // Hunt ends (tick 140)
            else if (ticksAlive == 140) {
                for (BlockDisplayHandle h : pursuitHandles) h.entity().remove();
                pursuitHandles.clear();
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GodsContempt(plugin); }
    }

    // ================================================================
    // 90. TRANSCENDENCE OVERLOAD - Full-body particle explosion
    // Every particle type fires outward, 1 sec darkness, then combo attack
    // ================================================================
    public static class TranscendenceOverload extends BossAttack {
        private final List<BlockDisplayHandle> overloadHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> comboHandles = new ArrayList<>();
        private boolean overloaded = false;
        private boolean comboFired = false;

        public TranscendenceOverload(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("transcendence_overload", AttackType.BOSS, 2), "dog");
            config.setDamage(14.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // DoG segments with multi-particle burst beginning
            for (int seg = 0; seg < 8; seg++) {
                Location segLoc = center.clone().add(seg * 2 - 7, 3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(2, 0);
                overloadHandles.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Multi-particle burst telegraph (0-30 ticks = 1.5 sec)
            if (ticksAlive < 30 && !overloaded) {
                float intensity = ticksAlive / 30.0f;
                if (ticksAlive % 3 == 0) {
                    for (BlockDisplayHandle h : overloadHandles) {
                        Location loc = h.entity().getLocation();
                        DisplayBuilder.cyanDust(loc, (int)(4 * intensity + 1), 1.5 * intensity);
                        DisplayBuilder.purpleDust(loc, (int)(3 * intensity + 1), 1.2 * intensity);
                        DisplayBuilder.crimsonDust(loc, (int)(2 * intensity + 1), 1.0 * intensity);
                    }
                }
            }
            // Overload burst (tick 30): everything fires outward, then darkness
            else if (ticksAlive == 30 && !overloaded) {
                overloaded = true;
                // Remove body segments
                for (BlockDisplayHandle h : overloadHandles) h.entity().remove();
                overloadHandles.clear();

                // Massive outward burst of all particle types
                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 50, 12.0);
                DisplayBuilder.purpleDust(center.clone().add(0, 3, 0), 50, 12.0);
                DisplayBuilder.crimsonDust(center.clone().add(0, 3, 0), 40, 10.0);

                // Darkness overlay (blackstone blocks)
                for (int x = -12; x <= 12; x += 4) {
                    for (int z = -12; z <= 12; z += 4) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 8, z), Material.POLISHED_BLACKSTONE);
                        h.scale(4.0f, 0.5f, 4.0f).glow(0, 200, 255).interpolation(2, 0);
                        overloadHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 2.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 0.3f);
            }
            // Darkness snap (30-50 ticks = 1 sec)
            else if (overloaded && ticksAlive < 50 && !comboFired) {
                // Dark atmosphere
                if (ticksAlive % 5 == 0) {
                    DisplayBuilder.darkPurpleDust(center.clone().add(0, 4, 0), 8, 10.0);
                }
            }
            // Combo attack fires from darkness (tick 50)
            else if (ticksAlive == 50 && !comboFired) {
                comboFired = true;
                // Remove darkness overlay
                for (BlockDisplayHandle h : overloadHandles) h.entity().remove();
                overloadHandles.clear();

                // Rift charge body
                for (int seg = 0; seg < 6; seg++) {
                    Location segLoc = center.clone().add(-12 + seg * 2, 2, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(segLoc, Material.AMETHYST_BLOCK);
                    h.scale(1.0f, 1.0f, 1.0f).glow(0, 200, 255).interpolation(1, 0);
                    comboHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Beam segments
                for (int seg = 0; seg < 12; seg++) {
                    double angle = Math.random() * Math.PI * 2;
                    Location beamLoc = center.clone().add(
                        Math.cos(angle) * seg, 3, Math.sin(angle) * seg);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(beamLoc, Material.END_ROD);
                    h.scale(0.2f, 0.2f, 1.5f).glow(240, 240, 255).interpolation(1, 0);
                    comboHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Projectile volley (4 amethyst clusters)
                for (int p = 0; p < 4; p++) {
                    double pAngle = (Math.PI * 2 / 4) * p;
                    Location projLoc = center.clone().add(Math.cos(pAngle) * 4, 4, Math.sin(pAngle) * 4);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(projLoc, Material.AMETHYST_CLUSTER);
                    h.scale(0.5f, 0.5f, 0.5f).glow(128, 0, 255).interpolation(1, 0);
                    comboHandles.add(h);
                    spawnedEntities.add(h.entity());
                }

                DisplayBuilder.cyanDust(center.clone().add(0, 3, 0), 30, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);
            }
            // Combo sweeps (50-170 ticks = 6 sec window)
            else if (comboFired && ticksAlive > 50 && ticksAlive < 170) {
                float progress = (ticksAlive - 50) / 120.0f;
                // Move charge body across
                for (int i = 0; i < 6 && i < comboHandles.size(); i++) {
                    comboHandles.get(i).entity().teleport(
                        center.clone().add(-12 + i * 2 + progress * 24, 2, 0));
                }
                // Move projectiles outward
                for (int p = 0; p < 4; p++) {
                    int pIdx = 18 + p;
                    if (pIdx < comboHandles.size()) {
                        double pAngle = (Math.PI * 2 / 4) * p;
                        double dist = 4 + progress * 15;
                        comboHandles.get(pIdx).entity().teleport(
                            center.clone().add(Math.cos(pAngle) * dist, 4 - progress * 3, Math.sin(pAngle) * dist));
                    }
                }
                if (ticksAlive % 8 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(progress * 12, 2, 0), 8, 3.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TranscendenceOverload(plugin); }
    }
}
