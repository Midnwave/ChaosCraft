package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.environmental;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Environmental — GROUP 6: ATMOSPHERIC
 * 10 arena-wide effects that hit all players simultaneously.
 * Attacks 51-60 from boss1-voidmaw.md.
 *
 * Design notes:
 * - No status effects (all debuff descriptions translated to damage or knockback-push visuals)
 * - tracksPlayer = false for all (atmospheric = global, not targeted)
 * - Calamity color palette: purple (128,0,255), cyan (0,200,255), crimson (200,0,50)
 */
public final class Atmospheric {

    private Atmospheric() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidBreathes(plugin));
        registry.register(new DimensionalPressure(plugin));
        registry.register(new VoidHeartbeat(plugin));
        registry.register(new MaddeningWhisper(plugin));
        registry.register(new TotalDarkness(plugin));
        registry.register(new RealityBleed(plugin));
        registry.register(new VoidScream(plugin));
        registry.register(new NecroticPulse(plugin));
        registry.register(new VoidGravitySpike(plugin));
        registry.register(new TheLongNote(plugin));
    }

    // =========================================================================
    // 51. THE VOID BREATHES — massive outward ring pulse from arena center
    // =========================================================================
    public static class VoidBreathes extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ringHandles = new ArrayList<>();
        private boolean pulseFired = false;

        public VoidBreathes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_breathes", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(600); // 30 seconds
            config.setTicksBetweenDamage(80);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Single dead-silence moment, then inhalation sound
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.6f, 0.3f);

            // Inner ring of obsidian blocks at ground level — will expand outward
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 2, 0, Math.sin(angle) * 2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.2f, 0.3f).glow(128, 0, 255).interpolation(3, 0);
                ringHandles.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Expand the ring outward rapidly
            float progress = ticksAlive / 80.0f;
            double radius = 2.0 + progress * 22.0; // expands 2 -> 24 blocks
            float alpha = Math.max(0f, 1f - progress * 1.4f);

            for (int i = 0; i < ringHandles.size(); i++) {
                double angle = (2 * Math.PI * i) / ringHandles.size();
                Location newLoc = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                BlockDisplay bd = (BlockDisplay) ringHandles.get(i).entity();
                bd.teleport(newLoc);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.1f, -0.15f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.3f * alpha, 0.2f * alpha, 0.3f * alpha),
                        new AxisAngle4f(0, 0, 1, 0)
                ));
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            }

            // At tick 10: fire the pulse visual + knockback simulation via particles
            if (ticksAlive == 10 && !pulseFired) {
                pulseFired = true;
                DisplayBuilder.purpleDust(center, 80, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.3f);
                // Ring outward particle burst
                for (int i = 0; i < 32; i++) {
                    double angle = (2 * Math.PI * i) / 32;
                    Location pLoc = center.clone().add(Math.cos(angle) * 8, 0.5, Math.sin(angle) * 8);
                    DisplayBuilder.purpleDust(pLoc, 5, 0.5);
                }
            }

            // Continuous ambient particle pulse
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.purpleDust(center, 12, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBreathes(plugin); }
    }

    // =========================================================================
    // 52. DIMENSIONAL PRESSURE — oppressive sky press, arena-wide debuff visual
    // =========================================================================
    public static class DimensionalPressure extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> pressureSlabs = new ArrayList<>();

        public DimensionalPressure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dimensional_pressure", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(3.0);
            config.setDamageRadius(25.0); // covers full arena
            config.setDurationTicks(400); // 20 seconds
            config.setCooldownTicks(900); // 45 seconds
            config.setTicksBetweenDamage(60); // once every 3 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Four corner pressure blocks high up — visual of sky pressing down
            double[][] corners = {{-8, 10, -8}, {8, 10, -8}, {-8, 10, 8}, {8, 10, 8}};
            for (double[] off : corners) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(4f, 0.15f, 4f).glow(128, 0, 255).interpolation(10, 0);
                pressureSlabs.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.4f);
            DisplayBuilder.purpleDust(center, 40, 10.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Slabs slowly descend, building pressure
            float descent = Math.min(ticksAlive * 0.04f, 4.0f);
            double[][] corners = {{-8, 0, -8}, {8, 0, -8}, {-8, 0, 8}, {8, 0, 8}};
            for (int i = 0; i < pressureSlabs.size(); i++) {
                double[] off = corners[i];
                Location newLoc = center.clone().add(off[0], 10 - descent, off[2]);
                BlockDisplay bd = (BlockDisplay) pressureSlabs.get(i).entity();
                bd.teleport(newLoc);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(3);
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.purpleDust(center, 15, 8.0);
            }

            // Peak pressure — crimson flash
            if (ticksAlive == 80) {
                DisplayBuilder.crimsonDust(center, 60, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DimensionalPressure(plugin); }
    }

    // =========================================================================
    // 53. VOID HEARTBEAT — rhythmic arena-wide pulse, damage on each beat
    // =========================================================================
    public static class VoidHeartbeat extends EnvironmentalAttack {

        private int lastBeatTick = -100;
        private int beatCount = 0;
        private static final int BEAT_INTERVAL = 60; // every 3 seconds
        private static final int MAX_BEATS = 5;
        private final List<BlockDisplayHandle> waveRing = new ArrayList<>();

        public VoidHeartbeat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_heartbeat", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0); // damage handled manually per beat
            config.setDamageRadius(0.0);
            config.setDurationTicks(440); // enough for 5 beats + ramp
            config.setCooldownTicks(1000); // 50 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spawn a ring of deepslate tiles at ground level for the pulse visual
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                Location loc = center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.1f, 0.5f).glow(128, 0, 255).interpolation(4, 0);
                waveRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // First 3 beats are warning (no damage), beats 4-8 deal damage
            int timeSinceLastBeat = ticksAlive - lastBeatTick;
            boolean onBeat = (timeSinceLastBeat >= BEAT_INTERVAL);

            if (onBeat && beatCount < MAX_BEATS + 3) {
                lastBeatTick = ticksAlive;
                beatCount++;
                boolean isDamaging = beatCount > 3; // first 3 are warning

                // Visual pulse — ring expands outward from arena center
                for (int i = 0; i < waveRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / waveRing.size();
                    float pulseR = isDamaging ? 10f : 6f;
                    Location pulseLoc = center.clone().add(Math.cos(angle) * pulseR, 0, Math.sin(angle) * pulseR);
                    BlockDisplay bd = (BlockDisplay) waveRing.get(i).entity();
                    bd.teleport(pulseLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(4);
                }

                if (isDamaging) {
                    // Deal damage to all players within arena
                    for (Player p : w.getPlayers()) {
                        if (isExempt(p)) continue;
                        if (p.getLocation().distanceSquared(center) <= 625) { // 25 block radius
                            p.damage(3.0); // 1.5 hearts
                        }
                    }
                    DisplayBuilder.crimsonDust(center, 50, 15.0);
                    DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, isDamaging ? 0.5f : 0.8f);
                } else {
                    DisplayBuilder.purpleDust(center, 30, 10.0);
                    DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 0.5f);
                }
            }

            // Gentle ambient pulse between beats
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.purpleDust(center, 8, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidHeartbeat(plugin); }
    }

    // =========================================================================
    // 54. MADDENING WHISPER — pure audio horror, no visual, arena-wide damage
    // =========================================================================
    public static class MaddeningWhisper extends EnvironmentalAttack {

        public MaddeningWhisper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("maddening_whisper", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // replaces Nausea/Slowness — intermittent damage
            config.setDamageRadius(25.0);
            config.setDurationTicks(240); // 12 seconds
            config.setCooldownTicks(1200); // 60 seconds
            config.setTicksBetweenDamage(40); // every 2 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Near-invisible whisper — very faint particle near center only
            DisplayBuilder.purpleDust(center, 3, 1.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Escalating whisper sounds over the duration
            if (ticksAlive == 40) {
                DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 0.5f);
            }
            if (ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 0.4f);
                DisplayBuilder.purpleDust(center, 10, 3.0);
            }
            if (ticksAlive == 120) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 0.7f);
                DisplayBuilder.purpleDust(center, 20, 6.0);
            }
            if (ticksAlive == 180) {
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.9f, 0.6f);
                DisplayBuilder.crimsonDust(center, 25, 8.0);
            }

            // Faint ambient particles — almost invisible
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 4, 2.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MaddeningWhisper(plugin); }
    }

    // =========================================================================
    // 55. TOTAL DARKNESS — dimming visual + flash damage on restoration
    // =========================================================================
    public static class TotalDarkness extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> darkPanels = new ArrayList<>();
        private boolean flashTriggered = false;

        public TotalDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("total_darkness", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(240); // 12 seconds (8s darkness + ramp)
            config.setCooldownTicks(2400); // 120 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Large flat black panels lowering from above — dim the arena visually
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI / 2) * i;
                Location loc = center.clone().add(Math.cos(angle) * 12, 12, Math.sin(angle) * 12);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(12f, 0.1f, 12f).glow(0, 0, 20).interpolation(8, 0);
                darkPanels.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dimming sequence sounds
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.9f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Panels descend over 3 seconds (60 ticks) to full blackout height
            if (ticksAlive <= 60) {
                float descent = ticksAlive * 0.1f;
                double[][] cornerOffsets = {{-12, 0, -12}, {12, 0, -12}, {-12, 0, 12}, {12, 0, 12}};
                for (int i = 0; i < darkPanels.size(); i++) {
                    double[] off = cornerOffsets[i];
                    Location newLoc = center.clone().add(off[0], 12 - descent, off[2]);
                    darkPanels.get(i).entity().teleport(newLoc);
                    darkPanels.get(i).entity().setInterpolationDuration(3);
                    darkPanels.get(i).entity().setInterpolationDelay(0);
                }
            }

            // Darkness holds from tick 60–220
            if (ticksAlive == 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 0.2f);
            }

            // At tick 220: flash of light — all players in arena take 1 heart
            if (ticksAlive == 220 && !flashTriggered) {
                flashTriggered = true;
                DisplayBuilder.cyanDust(center, 80, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.5f);
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 625) {
                        p.damage(2.0); // 1 heart flash damage
                    }
                }
                // Panels ascend rapidly
                for (BlockDisplayHandle h : darkPanels) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    Location up = bd.getLocation().add(0, 15, 0);
                    bd.teleport(up);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(10);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TotalDarkness(plugin); }
    }

    // =========================================================================
    // 56. REALITY BLEED — ghostly double of arena, disorienting visual + damage
    // =========================================================================
    public static class RealityBleed extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> ghostBlocks = new ArrayList<>();

        public RealityBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reality_bleed", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // disorientation damage proxy
            config.setDamageRadius(25.0);
            config.setDurationTicks(160); // 8 seconds
            config.setCooldownTicks(1500); // 75 seconds
            config.setTicksBetweenDamage(60); // 3 seconds between ticks
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Ghost overlapping copies — purple-tinted obsidian offset 3 blocks
            double offsetX = 3.0, offsetZ = 0.0;
            double[][] positions = {
                {-5, 0, -5}, {0, 0, -5}, {5, 0, -5},
                {-5, 0, 0},               {5, 0, 0},
                {-5, 0, 5},  {0, 0, 5},  {5, 0, 5}
            };
            for (double[] pos : positions) {
                Location loc = center.clone().add(pos[0] + offsetX, 0.05, pos[2] + offsetZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(1f, 0.05f, 1f).glow(128, 0, 255).interpolation(4, 0);
                ghostBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
            DisplayBuilder.purpleDust(center, 40, 10.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Ghost blocks flicker — random opacity via scale pulsing
            if (ticksAlive % 6 == 0) {
                float flicker = 0.03f + (float)(Math.random() * 0.05f);
                for (BlockDisplayHandle h : ghostBlocks) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.025f, -0.5f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(1f, flicker, 1f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.purpleDust(center, 15, 8.0);
            }

            // Fade out near the end
            if (ticksAlive > 120) {
                float alpha = Math.max(0f, 1f - (ticksAlive - 120) / 40.0f);
                DisplayBuilder.purpleDust(center, (int)(alpha * 10), 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RealityBleed(plugin); }
    }

    // =========================================================================
    // 57. VOID SCREAM — inward convergence + central detonation
    // =========================================================================
    public static class VoidScream extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> convergenceRing = new ArrayList<>();
        private boolean detonationFired = false;

        public VoidScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_scream", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(100); // 5 seconds
            config.setCooldownTicks(1300); // 65 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 3-second rising scream warning
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.4f);

            // Outer convergence ring — will collapse inward
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                Location loc = center.clone().add(Math.cos(angle) * 18, 1, Math.sin(angle) * 18);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.6f, 0.6f).glow(128, 0, 255).interpolation(3, 0);
                convergenceRing.add(h);
                spawnedEntities.add(h.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Convergence: outer ring collapses inward over 60 ticks
            if (ticksAlive <= 60) {
                float progress = ticksAlive / 60.0f;
                double radius = 18.0 * (1.0 - progress) + 0.5;
                for (int i = 0; i < convergenceRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / convergenceRing.size();
                    Location newLoc = center.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius);
                    BlockDisplay bd = (BlockDisplay) convergenceRing.get(i).entity();
                    bd.teleport(newLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(2);
                }
            }

            // Convergence damage at tick 60 (inward pull)
            if (ticksAlive == 60 && !detonationFired) {
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.0f, 1.5f);
                DisplayBuilder.purpleDust(center, 80, 5.0);

                // Convergence: 3 hearts to all near arena
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 625) {
                        p.damage(6.0); // 3 hearts convergence
                    }
                }
            }

            // Detonation at tick 65: outward blast + extra damage near center
            if (ticksAlive == 65 && !detonationFired) {
                detonationFired = true;
                DisplayBuilder.crimsonDust(center, 100, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.3f);

                // Center players: 3 extra hearts
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 16) { // 4 blocks
                        p.damage(6.0); // 3 hearts detonation
                    }
                }

                // Ring explodes outward
                for (int i = 0; i < convergenceRing.size(); i++) {
                    double angle = (2 * Math.PI * i) / convergenceRing.size();
                    Location blastLoc = center.clone().add(Math.cos(angle) * 14, 1, Math.sin(angle) * 14);
                    BlockDisplay bd = (BlockDisplay) convergenceRing.get(i).entity();
                    bd.teleport(blastLoc);
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidScream(plugin); }
    }

    // =========================================================================
    // 58. NECROTIC PULSE — necrotic wave passes through arena, damage + drain
    // =========================================================================
    public static class NecroticPulse extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> necroticTiles = new ArrayList<>();
        private boolean pulseFired = false;

        public NecroticPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("necrotic_pulse", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(3.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(220); // ~11 seconds
            config.setCooldownTicks(1100); // 55 seconds
            config.setTicksBetweenDamage(40); // every 2 seconds
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Gray necrotic tiles covering the arena floor in a cross pattern
            int[][] pattern = {
                {0,0}, {2,0}, {-2,0}, {0,2}, {0,-2},
                {4,0}, {-4,0}, {0,4}, {0,-4}, {2,2}, {-2,-2}, {2,-2}, {-2,2}
            };
            for (int[] pos : pattern) {
                Location loc = center.clone().add(pos[0], 0.02, pos[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.GRAY_CONCRETE);
                h.scale(1.8f, 0.04f, 1.8f).glow(80, 0, 160).interpolation(5, 0);
                necroticTiles.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_AMBIENT, 0.8f, 0.4f);
            DisplayBuilder.purpleDust(center, 50, 10.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Pre-warning: 1 second of dark glow
            if (ticksAlive < 20) {
                if (ticksAlive % 5 == 0) DisplayBuilder.purpleDust(center, 10, 5.0);
                return;
            }

            // The pulse itself at tick 20
            if (ticksAlive == 20 && !pulseFired) {
                pulseFired = true;
                DisplayBuilder.crimsonDust(center, 80, 12.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.9f, 0.5f);
            }

            // Necrotic tiles pulse with scale animation
            if (ticksAlive % 10 == 0) {
                float pulse = 1.8f + (float)(Math.sin(ticksAlive * 0.2) * 0.3f);
                for (BlockDisplayHandle h : necroticTiles) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-pulse / 2, -0.02f, -pulse / 2),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(pulse, 0.04f, pulse),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
                DisplayBuilder.purpleDust(center, 12, 7.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NecroticPulse(plugin); }
    }

    // =========================================================================
    // 59. VOID GRAVITY SPIKE — all players slammed down, fall damage proxy
    // =========================================================================
    public static class VoidGravitySpike extends EnvironmentalAttack {

        private boolean spikeFired = false;
        private final List<BlockDisplayHandle> crackLines = new ArrayList<>();

        public VoidGravitySpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_gravity_spike", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(0.0);
            config.setDamageRadius(0.0);
            config.setDurationTicks(80);
            config.setCooldownTicks(800); // 40 seconds
            config.setTicksBetweenDamage(999);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Brief weightlessness visual — small upward floaty particles
            DisplayBuilder.cyanDust(center, 30, 8.0);
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.5f);

            // Floor crack lines radiate outward (pre-spawn)
            double[][] crackDirs = {{1,0},{-1,0},{0,1},{0,-1},{0.7,0.7},{-0.7,0.7},{0.7,-0.7},{-0.7,-0.7}};
            for (double[] dir : crackDirs) {
                for (int step = 1; step <= 6; step++) {
                    Location loc = center.clone().add(dir[0] * step * 2, 0.01, dir[1] * step * 2);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRACKED_DEEPSLATE_TILES);
                    h.scale(0.3f, 0.02f, 0.3f).glow(80, 0, 160).interpolation(2, 0);
                    crackLines.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // 1 second weightlessness, then SPIKE
            if (ticksAlive == 20 && !spikeFired) {
                spikeFired = true;

                DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.3f);
                DisplayBuilder.crimsonDust(center, 100, 15.0);

                // Gravity spike — damage all players (simulate fall damage)
                for (Player p : w.getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(center) <= 625) {
                        // Minimum 2 hearts; airborne players take more (approximated by velocity)
                        double velocityY = Math.abs(p.getVelocity().getY());
                        double extraDamage = Math.min(velocityY * 4.0, 8.0);
                        p.damage(4.0 + extraDamage); // 2-6 hearts
                    }
                }
            }

            // Crack lines expand slightly on impact
            if (ticksAlive == 22) {
                for (BlockDisplayHandle h : crackLines) {
                    BlockDisplay bd = (BlockDisplay) h.entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.25f, -0.01f, -0.25f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.5f, 0.02f, 0.5f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(3);
                }
            }

            if (ticksAlive % 8 == 0 && ticksAlive > 20) {
                DisplayBuilder.purpleDust(center, 8, 4.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidGravitySpike(plugin); }
    }

    // =========================================================================
    // 60. THE LONG NOTE — persistent resonance rings + damage + sensory masking
    // =========================================================================
    public static class TheLongNote extends EnvironmentalAttack {

        private final List<BlockDisplayHandle> resonanceRings = new ArrayList<>();
        private int ringPhase = 0;

        public TheLongNote(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_long_note", AttackType.ENVIRONMENTAL, 1));
            config.setDamage(2.0); // replaces Weakness/Slowness — moderate damage
            config.setDamageRadius(25.0);
            config.setDurationTicks(300); // 15 seconds
            config.setCooldownTicks(1800); // 90 seconds
            config.setTicksBetweenDamage(40);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Sub-audible start — 4 second ramp
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.2f);

            // Ground resonance rings — concentric, will pulse outward
            double[] radii = {3.0, 6.0, 9.0, 12.0};
            for (double r : radii) {
                for (int i = 0; i < 10; i++) {
                    double angle = (2 * Math.PI * i) / 10;
                    Location loc = center.clone().add(Math.cos(angle) * r, 0.01, Math.sin(angle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                    h.scale(0.4f, 0.02f, 0.4f).glow(128, 0, 255).interpolation(4, 0);
                    resonanceRings.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Note reaches damaging frequency at tick 80 (4 seconds)
            if (ticksAlive == 80) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 0.1f);
                DisplayBuilder.purpleDust(center, 30, 10.0);
            }

            // Resonance ring outward pulse — each ring pulses in sequence
            if (ticksAlive % 20 == 0) {
                ringPhase = (ringPhase + 1) % 4;
                DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.2f + ringPhase * 0.1f);

                // Pulse the active ring outward then back
                int start = ringPhase * 10;
                int end = start + 10;
                for (int i = start; i < end && i < resonanceRings.size(); i++) {
                    BlockDisplay bd = (BlockDisplay) resonanceRings.get(i).entity();
                    bd.setTransformation(new Transformation(
                            new Vector3f(-0.3f, -0.01f, -0.3f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.02f, 0.7f),
                            new AxisAngle4f(0, 0, 1, 0)
                    ));
                    bd.setInterpolationDelay(0);
                    bd.setInterpolationDuration(5);
                }
            }

            // Ambient glow throughout
            if (ticksAlive % 15 == 0 && ticksAlive > 80) {
                DisplayBuilder.purpleDust(center, 10, 8.0);
            }

            // Climax near end
            if (ticksAlive > 250) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center, 12, 10.0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TheLongNote(plugin); }
    }
}
