package com.blockforge.chaoscraft.modes.seer.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import java.util.*;

/**
 * Seer Mode — PSYCHIC ASSAULTS
 * 12 psychic-themed block display attacks.
 * Palette: Deep purple (80,0,160), Psychic magenta (200,0,180), Pale eye white (240,230,255)
 */
public final class PsychicAssaults {

    private PsychicAssaults() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MindCage(plugin));
        registry.register(new ThoughtSpike(plugin));
        registry.register(new TelekineticThrow(plugin));
        registry.register(new PsychicBarrier(plugin));
        registry.register(new NeuralWeb(plugin));
        registry.register(new BrainwavePulse(plugin));
        registry.register(new MindMirror(plugin));
        registry.register(new PsychicLance(plugin));
        registry.register(new ConfusionOrbs(plugin));
        registry.register(new TelekineticCrush(plugin));
        registry.register(new ThoughtStorm(plugin));
        registry.register(new PsionicBlade(plugin));
    }

    // ================================================================
    // 1. MIND CAGE — Purpur cage trapping player
    // ================================================================
    public static class MindCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cageBlocks = new ArrayList<>();

        public MindCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mind_cage", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 8 vertical bars + 4 top + 4 bottom
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double r = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPUR_BLOCK);
                h.scale(0.3f, 3.0f, 0.3f).glow(200, 0, 180).interpolation(3, 0);
                cageBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location top = center.clone().add(Math.cos(angle) * 1.5, 3, Math.sin(angle) * 1.5);
                BlockDisplayHandle ht = displayBuilder.spawnBlock(top, Material.PURPUR_BLOCK);
                ht.scale(0.5f, 0.3f, 0.5f).glow(200, 0, 180).interpolation(3, 0);
                cageBlocks.add(ht);
                spawnedEntities.add(ht.entity());
                Location bot = center.clone().add(Math.cos(angle) * 1.5, -0.3, Math.sin(angle) * 1.5);
                BlockDisplayHandle hb = displayBuilder.spawnBlock(bot, Material.PURPUR_BLOCK);
                hb.scale(0.5f, 0.3f, 0.5f).glow(200, 0, 180).interpolation(3, 0);
                cageBlocks.add(hb);
                spawnedEntities.add(hb.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 8, 2.5, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MindCage(plugin); }
    }

    // ================================================================
    // 2. THOUGHT SPIKE — Amethyst thin spikes erupting in sequence
    // ================================================================
    public static class ThoughtSpike extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private int lastSpikeIndex = 0;

        public ThoughtSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thought_spike", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add((i - 5.5) * 1.2, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 2.0f, 0.3f).glow(240, 230, 255).interpolation(3, 0);
                spikeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            lastSpikeIndex = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int currentSpike = Math.min(11, ticksAlive / 5);
            if (currentSpike > lastSpikeIndex) {
                for (int i = lastSpikeIndex; i <= currentSpike; i++) {
                    spikeBlocks.get(i).entity().teleport(c.clone().add((i - 5.5) * 1.2, 0, 0));
                    Location spikeLoc = c.clone().add((i - 5.5) * 1.2, 0, 0);
                    triggerImpactDamage(spikeLoc);
                    DisplayBuilder.dustParticles(spikeLoc, 6, 1.0, 240, 230, 255, 1.5f);
                    DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.5f, 1.5f);
                }
                lastSpikeIndex = currentSpike;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThoughtSpike(plugin); }
    }

    // ================================================================
    // 3. TELEKINETIC THROW — Mixed blocks launched at player at high speed
    // ================================================================
    public static class TelekineticThrow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> throwBlocks = new ArrayList<>();
        private boolean launched = false;
        private static final Random RNG = new Random();

        public TelekineticThrow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("telekinetic_throw", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Material[] mats = {Material.AMETHYST_BLOCK, Material.PURPUR_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                    Material.PURPLE_STAINED_GLASS, Material.BLACKSTONE, Material.SCULK, Material.END_STONE,
                    Material.AMETHYST_BLOCK, Material.PURPUR_BLOCK, Material.MAGENTA_STAINED_GLASS,
                    Material.OBSIDIAN, Material.PURPLE_STAINED_GLASS, Material.BLACKSTONE};
            for (int i = 0; i < 14; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 8;
                double oy = 3 + RNG.nextDouble() * 4;
                double oz = (RNG.nextDouble() - 0.5) * 8;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                throwBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Float and shake for 20 ticks, then launch
            if (ticksAlive < 20) {
                for (BlockDisplayHandle h : throwBlocks) {
                    Location loc = h.entity().getLocation();
                    loc.add((RNG.nextDouble() - 0.5) * 0.1, 0.02, (RNG.nextDouble() - 0.5) * 0.1);
                    h.entity().teleport(loc);
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 8, 5.0, 200, 0, 180, 1.0f);
                }
            }
            if (!launched && ticksAlive >= 20) {
                launched = true;
                // Launch all toward center
                for (BlockDisplayHandle h : throwBlocks) {
                    h.entity().teleport(c.clone().add(0, 1, 0));
                }
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 20, 3.0, 80, 0, 160, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TelekineticThrow(plugin); }
    }

    // ================================================================
    // 4. PSYCHIC BARRIER — Purple glass wall sliding forward
    // ================================================================
    public static class PsychicBarrier extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private double offsetZ = 0;

        public PsychicBarrier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_barrier", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add((x - 1.5) * 0.8, y * 0.8, -6);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.8f, 0.8f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            offsetZ = -6;
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            offsetZ += 0.08;
            int idx = 0;
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    wallBlocks.get(idx).entity().teleport(c.clone().add((x - 1.5) * 0.8, y * 0.8, offsetZ));
                    idx++;
                }
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, offsetZ), 6, 2.0, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_STEP, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PsychicBarrier(plugin); }
    }

    // ================================================================
    // 5. NEURAL WEB — Amethyst + purple glass in web pattern, large area
    // ================================================================
    public static class NeuralWeb extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> webBlocks = new ArrayList<>();

        public NeuralWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("neural_web", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Hub-and-spoke web pattern
            // Center hub
            BlockDisplayHandle hub = displayBuilder.spawnBlock(center.clone().add(0, 2, 0), Material.AMETHYST_BLOCK);
            hub.scale(0.6f, 0.3f, 0.6f).glow(240, 230, 255).interpolation(3, 0);
            webBlocks.add(hub);
            spawnedEntities.add(hub.entity());
            // 6 spokes with 2 nodes each + 5 ring connectors
            for (int spoke = 0; spoke < 6; spoke++) {
                double angle = (2 * Math.PI * spoke) / 6;
                for (int node = 1; node <= 2; node++) {
                    double r = node * 2.5;
                    Location loc = center.clone().add(Math.cos(angle) * r, 2, Math.sin(angle) * r);
                    Material mat = node == 1 ? Material.AMETHYST_BLOCK : Material.PURPLE_STAINED_GLASS;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                    h.scale(0.4f, 0.2f, 0.4f).glow(200, 0, 180).interpolation(3, 0);
                    webBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Ring connectors
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5 + 0.3;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 2, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.3f, 0.2f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                webBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_VEIN_PLACE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Pulse glow on web nodes
            if (ticksAlive % 8 == 0) {
                int pulseNode = (ticksAlive / 8) % webBlocks.size();
                DisplayBuilder.dustParticles(webBlocks.get(pulseNode).entity().getLocation(), 4, 0.5, 240, 230, 255, 1.5f);
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 4, 5.0, 200, 0, 180, 0.8f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NeuralWeb(plugin); }
    }

    // ================================================================
    // 6. BRAINWAVE PULSE — Amethyst compressed sphere expanding as pulse
    // ================================================================
    public static class BrainwavePulse extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pulseBlocks = new ArrayList<>();
        private boolean pulsed = false;

        public BrainwavePulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brainwave_pulse", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(6.0);
            config.setDurationTicks(30);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 10);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                Location loc = center.clone().add(0, 2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 180).interpolation(3, 0);
                pulseBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 2.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive < 15) {
                // Compress
                for (int i = 0; i < pulseBlocks.size(); i++) {
                    pulseBlocks.get(i).entity().teleport(c.clone().add(0, 2, 0));
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 4, 0.5, 200, 0, 180, 1.5f);
            }
            if (!pulsed && ticksAlive >= 15) {
                pulsed = true;
                for (int i = 0; i < pulseBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 10);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double r = 6.0;
                    pulseBlocks.get(i).entity().teleport(c.clone().add(
                            Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 2, Math.sin(theta) * Math.sin(phi) * r));
                }
                triggerImpactDamage(c.clone().add(0, 2, 0));
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 30, 6.0, 240, 230, 255, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 2.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BrainwavePulse(plugin); }
    }

    // ================================================================
    // 7. MIND MIRROR — Purple glass flat mirror sweeping beam
    // ================================================================
    public static class MindMirror extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mirrorBlocks = new ArrayList<>();

        public MindMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mind_mirror", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // 3x4 flat glass panel + 2 frame blocks
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 4; y++) {
                    Location loc = center.clone().add((x - 1) * 0.6, y * 0.6 + 1, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.6f, 0.6f, 0.1f).glow(200, 0, 180).interpolation(3, 0);
                    mirrorBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            // Frame accent
            BlockDisplayHandle top = displayBuilder.spawnBlock(center.clone().add(0, 3.5, 0), Material.AMETHYST_BLOCK);
            top.scale(2.0f, 0.2f, 0.2f).glow(240, 230, 255).interpolation(3, 0);
            mirrorBlocks.add(top);
            spawnedEntities.add(top.entity());
            BlockDisplayHandle bot = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.AMETHYST_BLOCK);
            bot.scale(2.0f, 0.2f, 0.2f).glow(240, 230, 255).interpolation(3, 0);
            mirrorBlocks.add(bot);
            spawnedEntities.add(bot.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double rot = ticksAlive * 0.04;
            for (int i = 0; i < mirrorBlocks.size(); i++) {
                Location loc = mirrorBlocks.get(i).entity().getLocation();
                double dx = loc.getX() - c.getX();
                double dz = loc.getZ() - c.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 0.1) dist = 0.1;
                double currentAngle = Math.atan2(dz, dx) + 0.04;
                Location newLoc = c.clone().add(Math.cos(currentAngle) * dist, loc.getY() - c.getY(), Math.sin(currentAngle) * dist);
                mirrorBlocks.get(i).entity().teleport(newLoc);
            }
            // Beam from mirror
            if (ticksAlive % 3 == 0) {
                double beamAngle = rot + Math.PI;
                for (int d = 1; d < 8; d++) {
                    Location beamLoc = c.clone().add(Math.cos(beamAngle) * d, 2, Math.sin(beamAngle) * d);
                    DisplayBuilder.dustParticles(beamLoc, 2, 0.3, 240, 230, 255, 1.2f);
                }
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.3f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MindMirror(plugin); }
    }

    // ================================================================
    // 8. PSYCHIC LANCE — Purpur javelin, hovers then launches
    // ================================================================
    public static class PsychicLance extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> lanceBlocks = new ArrayList<>();
        private boolean launched = false;

        public PsychicLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psychic_lance", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(450);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, 5, (i - 5.5) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPUR_BLOCK);
                float s = 0.5f - Math.abs(i - 5.5f) * 0.06f;
                h.scale(s, s, 0.5f).glow(200, 0, 180).interpolation(3, 0);
                lanceBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive < 20) {
                // Hover and wobble
                for (int i = 0; i < lanceBlocks.size(); i++) {
                    double wobble = Math.sin(ticksAlive * 0.3 + i * 0.2) * 0.1;
                    lanceBlocks.get(i).entity().teleport(c.clone().add(wobble, 5 + wobble, (i - 5.5) * 0.5));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 4, 2.0, 200, 0, 180, 1.0f);
                }
            }
            if (!launched && ticksAlive >= 20) {
                launched = true;
                for (BlockDisplayHandle h : lanceBlocks) {
                    h.entity().teleport(c.clone().add(0, 1, 0));
                }
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 20, 3.0, 200, 0, 180, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PsychicLance(plugin); }
    }

    // ================================================================
    // 9. CONFUSION ORBS — Small amethyst orbs at random positions
    // ================================================================
    public static class ConfusionOrbs extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> orbBlocks = new ArrayList<>();
        private static final Random RNG = new Random();

        public ConfusionOrbs(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("confusion_orbs", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 15; i++) {
                double ox = (RNG.nextDouble() - 0.5) * 10;
                double oy = 1 + RNG.nextDouble() * 5;
                double oz = (RNG.nextDouble() - 0.5) * 10;
                Location loc = center.clone().add(ox, oy, oz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.3f, 0.3f, 0.3f).glow(240, 230, 255).interpolation(3, 0);
                orbBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Random teleport orbs periodically
            if (ticksAlive % 10 == 0) {
                for (BlockDisplayHandle h : orbBlocks) {
                    double ox = (RNG.nextDouble() - 0.5) * 10;
                    double oy = 1 + RNG.nextDouble() * 5;
                    double oz = (RNG.nextDouble() - 0.5) * 10;
                    h.entity().teleport(c.clone().add(ox, oy, oz));
                }
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, 5.0, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 6 == 0) {
                int idx = RNG.nextInt(orbBlocks.size());
                DisplayBuilder.dustParticles(orbBlocks.get(idx).entity().getLocation(), 4, 0.5, 240, 230, 255, 1.5f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.3f, RNG.nextFloat() + 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ConfusionOrbs(plugin); }
    }

    // ================================================================
    // 10. TELEKINETIC CRUSH — Blocks closing in from all directions
    // ================================================================
    public static class TelekineticCrush extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> crushBlocks = new ArrayList<>();
        private final double[][] startOffsets = new double[14][3];

        public TelekineticCrush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("telekinetic_crush", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Material[] mats = {Material.AMETHYST_BLOCK, Material.PURPUR_BLOCK, Material.OBSIDIAN, Material.CRYING_OBSIDIAN,
                    Material.PURPLE_STAINED_GLASS, Material.BLACKSTONE, Material.SCULK, Material.AMETHYST_BLOCK,
                    Material.PURPUR_BLOCK, Material.OBSIDIAN, Material.PURPLE_STAINED_GLASS, Material.BLACKSTONE,
                    Material.END_STONE, Material.MAGENTA_STAINED_GLASS};
            for (int i = 0; i < 14; i++) {
                double angle = (2 * Math.PI * i) / 14;
                double r = 6.0;
                double y = 1 + (i % 3) * 1.5;
                startOffsets[i] = new double[]{Math.cos(angle) * r, y, Math.sin(angle) * r};
                Location loc = center.clone().add(startOffsets[i][0], startOffsets[i][1], startOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mats[i]);
                h.scale(0.6f, 0.6f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                crushBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.5f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double progress = Math.min(1.0, ticksAlive / 150.0);
            for (int i = 0; i < crushBlocks.size(); i++) {
                double x = startOffsets[i][0] * (1.0 - progress);
                double y = startOffsets[i][1];
                double z = startOffsets[i][2] * (1.0 - progress);
                crushBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 4.0 * (1 - (float) progress), 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 10 == 0) {
                float pitch = 0.5f + (float) progress * 1.5f;
                DisplayBuilder.playSound(c, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.3f, Math.min(2.0f, pitch));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TelekineticCrush(plugin); }
    }

    // ================================================================
    // 11. THOUGHT STORM — Amethyst + purpur spinning rapidly overhead
    // ================================================================
    public static class ThoughtStorm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stormBlocks = new ArrayList<>();

        public ThoughtStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thought_storm", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 16; i++) {
                Material mat = i % 2 == 0 ? Material.AMETHYST_BLOCK : Material.PURPUR_BLOCK;
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 6, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 0, 180).interpolation(3, 0);
                stormBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double speed = ticksAlive * 0.2;
            for (int i = 0; i < stormBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 16 + speed;
                double r = 3.5 + Math.sin(ticksAlive * 0.05 + i) * 1.0;
                double y = 6 + Math.cos(ticksAlive * 0.08 + i) * 1.0;
                stormBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 6, 0), 6, 4.0, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThoughtStorm(plugin); }
    }

    // ================================================================
    // 12. PSIONIC BLADE — Purpur crescent blade sweeping horizontally
    // ================================================================
    public static class PsionicBlade extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();

        public PsionicBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("psionic_blade", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                double angle = Math.toRadians(-60 + (120.0 * i / 11));
                double r = 3.0;
                Location loc = center.clone().add(Math.cos(angle) * r, 1.5, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPUR_BLOCK);
                float s = 0.5f + 0.2f * (float) Math.sin(Math.PI * i / 11.0);
                h.scale(s, 0.3f, s).glow(200, 0, 180).interpolation(3, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double rotAngle = Math.toRadians(ticksAlive * 4.0);
            for (int i = 0; i < bladeBlocks.size(); i++) {
                double arcAngle = Math.toRadians(-60 + (120.0 * i / 11));
                double totalAngle = arcAngle + rotAngle;
                double r = 3.0;
                bladeBlocks.get(i).entity().teleport(c.clone().add(Math.cos(totalAngle) * r, 1.5, Math.sin(totalAngle) * r));
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 6, 3.0, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.3f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PsionicBlade(plugin); }
    }
}
