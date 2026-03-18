package com.blockforge.chaoscraft.modes.calamity.attacks.phase2.environmental;

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
 * Phase 2 Environmental — GROUP 1: CRYSTAL SCAR TRIGGERS
 * 10 attacks where DoG's carved gouges become active hazards.
 * Attacks 1-10 from boss2-dog.md.
 *
 * Design notes:
 * - No status effects (all debuffs translated to damage/knockback)
 * - DoG crystalline plague palette: cyan (0,200,255), violet (128,0,255), white (240,240,255)
 * - Materials: AMETHYST_BLOCK, AMETHYST_CLUSTER, BUDDING_AMETHYST, CYAN_STAINED_GLASS, SEA_LANTERN
 */
public final class CrystalScarTriggers {

    private CrystalScarTriggers() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ScarIgnition(plugin));
        registry.register(new CrystalScarGeyser(plugin));
        registry.register(new ScarSpread(plugin));
        registry.register(new ScarMirror(plugin));
        registry.register(new CrystalSpineEruption(plugin));
        registry.register(new ScarResonanceWave(plugin));
        registry.register(new TrappedScar(plugin));
        registry.register(new ScarAnchor(plugin));
        registry.register(new ScarLink(plugin));
        registry.register(new ScarBloom(plugin));
    }

    // =========================================================================
    // 1. SCAR IGNITION — dormant scar floods with teal particles, contact damage
    // =========================================================================
    public static class ScarIgnition extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> scarHandles = new ArrayList<>();

        public ScarIgnition(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_ignition", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(6.0); // 3 hearts per tick cycle
            config.setDamageRadius(2.5);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(440); // 22 seconds
            config.setTicksBetweenDamage(20); // once per second
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: dim pulse + resonant tone 2 seconds before full ignition
            DisplayBuilder.cyanDust(center, 8, 2.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);

            // Scar gouge line: 12 blocks long, 2 blocks wide of amethyst ground tiles
            for (int i = -6; i <= 5; i++) {
                for (int z = -1; z <= 0; z++) {
                    Location loc = center.clone().add(i, 0.02, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.9f, 0.06f, 0.9f).glow(0, 200, 255).interpolation(5, 0);
                    scarHandles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Warning phase: first 40 ticks (2 seconds) are dim pulsing
            if (ticksAlive <= 40) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center, 6, 3.0);
                }
                return;
            }

            // Full ignition: dense teal particles + electric sparks
            if (ticksAlive == 41) {
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.8f);
            }

            if (ticksAlive % 4 == 0) {
                for (int i = -6; i <= 5; i += 2) {
                    Location pLoc = center.clone().add(i, 0.3, 0);
                    DisplayBuilder.cyanDust(pLoc, 4, 0.5);
                }
            }

            // Scar glow intensification over time
            if (ticksAlive % 20 == 0) {
                float pulse = 0.06f + (float)(Math.sin(ticksAlive * 0.15) * 0.02f);
                for (BlockDisplayHandle h : scarHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.45f, -0.03f, -0.45f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.9f, pulse, 0.9f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarIgnition(plugin); }
    }

    // =========================================================================
    // 2. CRYSTAL SCAR GEYSER — vertical teal column erupts from scar, launches players
    // =========================================================================
    public static class CrystalScarGeyser extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> geyserColumn = new ArrayList<>();
        private BlockDisplayHandle apexGlass;

        public CrystalScarGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_scar_geyser", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(120); // 6 seconds
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 1.5 seconds — vibrating amethyst particles + crackling sound
            DisplayBuilder.cyanDust(center, 12, 1.5);
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.7f, 1.2f);

            // Vertical column of amethyst cluster blocks, 10 blocks tall
            for (int y = 0; y < 10; y++) {
                Location loc = center.clone().add(0, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.4f, 0.8f, 0.4f).glow(0, 200, 255).interpolation(4, 0);
                geyserColumn.add(h);
                spawnedEntities.add(h.entity());
            }

            // Apex: cyan stained glass rotating slowly
            Location apexLoc = center.clone().add(0, 10, 0);
            apexGlass = displayBuilder.spawnBlock(apexLoc, Material.CYAN_STAINED_GLASS);
            apexGlass.scale(0.6f, 0.6f, 0.6f).glow(0, 200, 255).interpolation(6, 0);
            spawnedEntities.add(apexGlass.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rotate apex glass
            if (ticksAlive % 4 == 0) {
                float angle = ticksAlive * 0.1f;
                apexGlass.entity().setTransformation(new Transformation(
                        new Vector3f(-0.3f, -0.3f, -0.3f),
                        new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(0.6f, 0.6f, 0.6f),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                apexGlass.entity().setInterpolationDelay(0);
                apexGlass.entity().setInterpolationDuration(4);
            }

            // Continuous column particles
            if (ticksAlive % 3 == 0) {
                Location pLoc = center.clone().add(0, Math.random() * 10, 0);
                DisplayBuilder.cyanDust(pLoc, 5, 0.3);
            }

            // Launch players who touch the column upward
            if (ticksAlive % 10 == 0 && ticksAlive > 30) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    double dx = Math.abs(pLoc.getX() - center.getX());
                    double dz = Math.abs(pLoc.getZ() - center.getZ());
                    if (dx <= 1.0 && dz <= 1.0 && pLoc.getY() >= center.getY() - 1
                            && pLoc.getY() <= center.getY() + 10) {
                        p.setVelocity(p.getVelocity().setY(1.2));
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalScarGeyser(plugin); }
    }

    // =========================================================================
    // 3. SCAR SPREAD — existing scar widens visually, permanent radius increase
    // =========================================================================
    public static class ScarSpread extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> spreadTiles = new ArrayList<>();

        public ScarSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_spread", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage from spread itself
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds visual
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Chime warning
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.6f);

            // Spawn widening slab extensions on both sides of the scar
            for (int i = -4; i <= 3; i++) {
                for (int side = -1; side <= 1; side += 2) {
                    Location loc = center.clone().add(i, 0.01, side * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                    h.scale(0.0f, 0.04f, 0.8f).glow(0, 200, 255).interpolation(8, 0);
                    spreadTiles.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Gradually scale up the spread tiles over 3 seconds (60 ticks)
            if (ticksAlive <= 60) {
                float progress = ticksAlive / 60.0f;
                float scaleX = progress * 0.9f;
                for (BlockDisplayHandle h : spreadTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-scaleX / 2, -0.02f, -0.4f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(scaleX, 0.04f, 0.8f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }

            // Particle bursts as each extension piece appears
            if (ticksAlive % 8 == 0 && ticksAlive <= 60) {
                int idx = ticksAlive / 8;
                if (idx < spreadTiles.size()) {
                    Location pLoc = spreadTiles.get(idx).entity().getLocation();
                    DisplayBuilder.cyanDust(pLoc, 8, 0.5);
                }
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center, 6, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarSpread(plugin); }
    }

    // =========================================================================
    // 4. SCAR MIRROR — two opposite scars ignite, linked damage across both
    // =========================================================================
    public static class ScarMirror extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> scarAHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> scarBHandles = new ArrayList<>();
        private final List<BlockDisplayHandle> linkBeam = new ArrayList<>();

        public ScarMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_mirror", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(6.0); // 3 hearts direct contact
            config.setDamageRadius(2.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: both scars flash white simultaneously
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.7f);

            // Scar A — offset -12 on X axis
            for (int i = -3; i <= 3; i++) {
                Location loc = center.clone().add(-12 + i, 0.02, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.9f, 0.05f, 1.2f).glow(0, 200, 255).interpolation(4, 0);
                scarAHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Scar B — offset +12 on X axis (mirror)
            for (int i = -3; i <= 3; i++) {
                Location loc = center.clone().add(12 + i, 0.02, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.9f, 0.05f, 1.2f).glow(0, 200, 255).interpolation(4, 0);
                scarBHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // Electric spark connection beam between scars
            for (int i = -10; i <= 10; i += 2) {
                Location loc = center.clone().add(i, 0.15, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.15f, 0.08f, 0.15f).glow(0, 200, 255).interpolation(3, 0);
                linkBeam.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning flash at tick 0-50 (2.5 seconds)
            if (ticksAlive <= 50) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.dustParticles(center.clone().add(-12, 0.3, 0), 10, 1.5, 240, 240, 255, 1.0f);
                    DisplayBuilder.dustParticles(center.clone().add(12, 0.3, 0), 10, 1.5, 240, 240, 255, 1.0f);
                }
                return;
            }

            // Active phase: pulse link beam
            if (ticksAlive % 6 == 0) {
                float flicker = 0.08f + (float)(Math.random() * 0.06f);
                for (BlockDisplayHandle h : linkBeam) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.075f, -0.04f, -0.075f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.15f, flicker, 0.15f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            // Mirror residual damage: players near scar B get 3.0 (1.5 hearts) residual
            if (ticksAlive % 20 == 0 && ticksAlive > 50) {
                Location scarA = center.clone().add(-12, 0, 0);
                Location scarB = center.clone().add(12, 0, 0);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double distA = p.getLocation().distanceSquared(scarA);
                    double distB = p.getLocation().distanceSquared(scarB);
                    if (distA <= 9.0) { // within 3 blocks of scar A
                        p.damage(3.0); // 1.5 hearts residual from mirror
                    }
                    if (distB <= 9.0) { // within 3 blocks of scar B
                        p.damage(3.0); // 1.5 hearts residual from mirror
                    }
                }
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(-12, 0.3, 0), 6, 1.5);
                DisplayBuilder.cyanDust(center.clone().add(12, 0.3, 0), 6, 1.5);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarMirror(plugin); }
    }

    // =========================================================================
    // 5. CRYSTAL SPINE ERUPTION — row of amethyst spires erupt along scar in ripple
    // =========================================================================
    public static class CrystalSpineEruption extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> spineHandles = new ArrayList<>();
        private static final int SCAR_LENGTH = 12;

        public CrystalSpineEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_spine_eruption", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts per spine contact
            config.setDamageRadius(1.5);
            config.setDurationTicks(400); // 20 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn spines along scar at scale 0 — will ripple upward
            for (int i = 0; i < SCAR_LENGTH / 2; i++) {
                Location loc = center.clone().add(i * 2 - SCAR_LENGTH / 2, 0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_CLUSTER);
                h.scale(0.0f, 0.0f, 0.0f).glow(0, 200, 255).interpolation(10, 0);
                spineHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ripple: each spine emerges 10 ticks after the previous
            for (int i = 0; i < spineHandles.size(); i++) {
                int spineStartTick = i * 10;
                if (ticksAlive == spineStartTick) {
                    // Emerge this spine
                    BlockDisplay bd = (BlockDisplay) spineHandles.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.3f, 0, -0.3f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.6f, 1.5f, 0.6f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);

                    Location pLoc = bd.getLocation();
                    DisplayBuilder.cyanDust(pLoc, 10, 0.5);
                    DisplayBuilder.playSound(pLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.6f + i * 0.05f);
                }
            }

            // Fade out over last 100 ticks (5 seconds)
            if (ticksAlive > 300) {
                float fadeProgress = (ticksAlive - 300) / 100.0f;
                float scale = Math.max(0f, 1.0f - fadeProgress);
                for (BlockDisplayHandle h : spineHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.3f * scale, 0, -0.3f * scale),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.6f * scale, 1.5f * scale, 0.6f * scale),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }

            // Continuous ambient dust
            if (ticksAlive % 10 == 0 && ticksAlive > 0 && ticksAlive <= 300) {
                DisplayBuilder.cyanDust(center, 8, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CrystalSpineEruption(plugin); }
    }

    // =========================================================================
    // 6. SCAR RESONANCE WAVE — horizontal shockwave of teal dust from scar
    // =========================================================================
    public static class ScarResonanceWave extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private double waveRadius = 0;

        public ScarResonanceWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_resonance_wave", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // handled manually
            config.setDamageRadius(0.0);
            config.setDurationTicks(260); // ~13 seconds (1s warning + 12s wave)
            config.setCooldownTicks(700); // 35 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: resonant tone
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 0.8f);

            // Wave front: ring of sea lantern blocks at ground level
            for (int i = 0; i < 24; i++) {
                double angle = (2 * Math.PI * i) / 24;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 0.1, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.3f, 0.15f, 0.3f).glow(0, 200, 255).interpolation(3, 0);
                waveBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 1 second warning, then wave fires
            if (ticksAlive <= 20) {
                if (ticksAlive % 5 == 0) DisplayBuilder.cyanDust(center, 8, 1.0);
                return;
            }

            // Wave expands outward at 3 blocks per second (60 ticks/sec -> 0.15 blocks/tick)
            waveRadius += 0.15;
            float alpha = Math.max(0f, 1f - (float)(waveRadius / 40.0));

            for (int i = 0; i < waveBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / waveBlocks.size();
                Location newLoc = center.clone().add(Math.cos(angle) * waveRadius, 0.1, Math.sin(angle) * waveRadius);
                BlockDisplay bd = (BlockDisplay) waveBlocks.get(i).entity();
                bd.teleport(newLoc);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.075f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f * alpha, 0.15f * alpha, 0.3f * alpha),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // Damage players hit by the wave front (within 1.5 blocks of radius ring)
            if (ticksAlive % 5 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    double dist = Math.sqrt(p.getLocation().distanceSquared(center));
                    if (Math.abs(dist - waveRadius) <= 1.5 && p.getLocation().getY() <= center.getY() + 1.5) {
                        p.damage(4.0); // 2 hearts on contact, jumpable
                    }
                }
                // Wave front particles
                DisplayBuilder.cyanDust(center.clone().add(waveRadius * 0.7, 0.5, 0), 6, 1.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarResonanceWave(plugin); }
    }

    // =========================================================================
    // 7. TRAPPED SCAR — goes dark, then detonates without warning, 5-block AoE
    // =========================================================================
    public static class TrappedScar extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> darkScarHandles = new ArrayList<>();
        private boolean detonated = false;

        public TrappedScar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("trapped_scar", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(200); // ~10 seconds (8s dark + detonation + aftermath)
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Scar appears dormant — dark, no glow, barely visible
            for (int i = -3; i <= 3; i++) {
                Location loc = center.clone().add(i, 0.01, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.POLISHED_BLACKSTONE);
                h.scale(0.8f, 0.03f, 0.8f).glow(20, 20, 30).interpolation(3, 0);
                darkScarHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            // No sound warning — the silence IS the tell
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Dark phase: 0-160 ticks (8 seconds) — scar sits dormant
            if (ticksAlive < 160) {
                // Subtle: ambient crystal hum ceases at tick 100 (3 sec before detonation)
                // No particles at all — the silence is intentional
                return;
            }

            // DETONATION at tick 160
            if (ticksAlive == 160 && !detonated) {
                detonated = true;

                // Massive burst
                DisplayBuilder.cyanDust(center, 80, 5.0);
                DisplayBuilder.dustParticles(center, 40, 5.0, 240, 240, 255, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.6f);

                // Flash the scar blocks bright
                for (BlockDisplayHandle h : darkScarHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setGlowColorOverride(Color.fromRGB(240, 240, 255));
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.4f, -0.015f, -0.4f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1.2f, 0.06f, 1.2f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }

                // 5-block spherical radius damage — 10.0 HP (5 hearts)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 25.0) { // 5 block radius
                        p.damage(10.0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TrappedScar(plugin); }
    }

    // =========================================================================
    // 8. SCAR ANCHOR — upward reverse portal particles, slows movement in radius
    // =========================================================================
    public static class ScarAnchor extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> anchorHandles = new ArrayList<>();

        public ScarAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_anchor", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // no damage, pure movement hazard
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Scar anchor line — brightens over 2 seconds
            for (int i = -5; i <= 5; i++) {
                Location loc = center.clone().add(i, 0.02, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BUDDING_AMETHYST);
                h.scale(0.8f, 0.05f, 0.8f).glow(0, 100, 140).interpolation(8, 0);
                anchorHandles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Brighten over first 40 ticks (2 seconds)
            if (ticksAlive <= 40) {
                float glow = ticksAlive / 40.0f;
                int g = (int)(200 * glow);
                int b = (int)(255 * glow);
                for (BlockDisplayHandle h : anchorHandles) {
                    h.entity().setGlowColorOverride(Color.fromRGB(0, g, b));
                }
                if (ticksAlive % 10 == 0) DisplayBuilder.cyanDust(center, 6, 2.0);
                return;
            }

            // Active phase: upward reverse portal particles + slow players
            if (ticksAlive % 4 == 0) {
                for (int i = -5; i <= 5; i += 2) {
                    Location pLoc = center.clone().add(i, 0.5 + Math.random() * 3, 0);
                    DisplayBuilder.cyanDust(pLoc, 3, 0.3);
                }
            }

            // Dampen horizontal velocity of nearby players (60% reduction)
            if (ticksAlive % 2 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 6.25) { // 2.5 block radius
                        org.bukkit.util.Vector vel = p.getVelocity();
                        p.setVelocity(vel.setX(vel.getX() * 0.4).setZ(vel.getZ() * 0.4));
                    }
                }
            }

            // Deep hum every 2 seconds
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarAnchor(plugin); }
    }

    // =========================================================================
    // 9. SCAR LINK — two scars connected by head-height laser beam trip-wire
    // =========================================================================
    public static class ScarLink extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();

        public ScarLink(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_link", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(0.0); // damage handled manually per intersection
            config.setDamageRadius(0.0);
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(760); // 38 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Flash warning at both endpoints
            Location endA = center.clone().add(-10, 0, 0);
            Location endB = center.clone().add(10, 0, 0);
            DisplayBuilder.cyanDust(endA, 10, 1.0);
            DisplayBuilder.cyanDust(endB, 10, 1.0);
            DisplayBuilder.playSound(endA, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.9f);
            DisplayBuilder.playSound(endB, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.9f);

            // Beam at head height (1 block above floor) connecting the two scar endpoints
            for (int i = -10; i <= 10; i++) {
                Location loc = center.clone().add(i, 1.0, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.12f, 0.12f, 0.12f).glow(0, 200, 255).interpolation(3, 0);
                beamHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Slow rotation visual — beam pulses slightly
            if (ticksAlive % 8 == 0) {
                float pulse = 0.12f + (float)(Math.sin(ticksAlive * 0.2) * 0.03f);
                for (BlockDisplayHandle h : beamHandles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -pulse / 2, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, pulse, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }
            }

            // Damage: 8 HP (4 hearts) per intersection — check players crossing the beam line
            if (ticksAlive % 10 == 0) {
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    Location pLoc = p.getLocation();
                    // Beam runs along X axis at center Z, Y=center+1
                    double dz = Math.abs(pLoc.getZ() - center.getZ());
                    double dy = Math.abs(pLoc.getY() - (center.getY() + 1.0));
                    double dx = pLoc.getX() - center.getX();
                    if (dz <= 0.6 && dy <= 0.6 && dx >= -10.5 && dx <= 10.5) {
                        p.damage(8.0); // 4 hearts
                        DisplayBuilder.cyanDust(pLoc, 12, 0.5);
                    }
                }
            }

            // Ambient particles along the beam
            if (ticksAlive % 6 == 0) {
                double x = (Math.random() * 20) - 10;
                Location pLoc = center.clone().add(x, 1.0, 0);
                DisplayBuilder.cyanDust(pLoc, 3, 0.2);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarLink(plugin); }
    }

    // =========================================================================
    // 10. SCAR BLOOM — short scar expands into circular damage zone
    // =========================================================================
    public static class ScarBloom extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> bloomHandles = new ArrayList<>();
        private boolean bloomTriggered = false;

        public ScarBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("scar_bloom", AttackType.ENVIRONMENTAL, 2));
            config.setDamage(4.0); // 2 hearts per tick after bloom
            config.setDamageRadius(5.0);
            config.setDurationTicks(200); // 10 seconds
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(40); // every 2 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Warning: 2 seconds before bloom
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.4f);
            DisplayBuilder.cyanDust(center, 10, 1.0);

            // Initial short scar (4-6 blocks)
            for (int i = -2; i <= 2; i++) {
                Location loc = center.clone().add(i, 0.02, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.9f, 0.05f, 0.9f).glow(0, 200, 255).interpolation(3, 0);
                bloomHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Warning phase: 0-40 ticks
            if (ticksAlive < 40) {
                if (ticksAlive % 10 == 0) DisplayBuilder.cyanDust(center, 6, 1.5);
                return;
            }

            // BLOOM at tick 40
            if (ticksAlive == 40 && !bloomTriggered) {
                bloomTriggered = true;

                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);
                DisplayBuilder.cyanDust(center, 60, 5.0);

                // Spawn bloom ring — 4-block radius of cyan tiles
                for (int angle = 0; angle < 16; angle++) {
                    double a = (2 * Math.PI * angle) / 16;
                    for (double r = 2.0; r <= 4.0; r += 1.0) {
                        Location loc = center.clone().add(Math.cos(a) * r, 0.02, Math.sin(a) * r);
                        BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CYAN_STAINED_GLASS);
                        h.scale(0.7f, 0.04f, 0.7f).glow(0, 200, 255).interpolation(5, 0);
                        bloomHandles.add(h);
                        spawnedEntities.add(h.entity());
                    }
                }

                // Initial burst damage: 6.0 HP (3 hearts) in bloom zone
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 25.0) { // 5 block radius
                        p.damage(6.0);
                    }
                }
            }

            // Ongoing glow pulse
            if (ticksAlive > 40 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center, 8, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ScarBloom(plugin); }
    }
}
