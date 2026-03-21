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
 * Seer Mode — DARK VISIONS
 * 12 shadow/nightmare block display attacks.
 * Palette: Void black (20,0,40), Deep purple (80,0,160), Eye red (200,0,50)
 */
public final class DarkVisions {

    private DarkVisions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowClone(plugin));
        registry.register(new NightmareConstruct(plugin));
        registry.register(new PhantomWall(plugin));
        registry.register(new DarkMirror(plugin));
        registry.register(new FearTotem(plugin));
        registry.register(new NightmareGate(plugin));
        registry.register(new ShadowSpike(plugin));
        registry.register(new DarkLantern(plugin));
        registry.register(new VisionShatter(plugin));
        registry.register(new CreepingShadow(plugin));
        registry.register(new NightmareCage(plugin));
        registry.register(new DarkSentinel(plugin));
    }

    // ================================================================
    // 1. SHADOW CLONE — Blackstone vaguely humanoid, mimics movement delayed
    // ================================================================
    public static class ShadowClone extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cloneBlocks = new ArrayList<>();

        public ShadowClone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_clone", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Humanoid shape: head(1), torso(4), arms(4), legs(3)
            double[][] offsets = {{0,2.2,0},{0,1.6,0},{0,1.2,0},{0,0.8,0},{0,0.4,0},
                    {-0.6,1.6,0},{-0.6,1.2,0},{0.6,1.6,0},{0.6,1.2,0},
                    {-0.3,0,0},{-0.3,-0.4,0},{0.3,0,0}};
            for (double[] off : offsets) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                float s = off[1] > 2 ? 0.5f : 0.4f;
                h.scale(s, s, s).glow(20, 0, 40).interpolation(3, 0);
                cloneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Sway the clone
            double sway = Math.sin(ticksAlive * 0.06) * 0.5;
            double[][] offsets = {{0,2.2,0},{0,1.6,0},{0,1.2,0},{0,0.8,0},{0,0.4,0},
                    {-0.6,1.6,0},{-0.6,1.2,0},{0.6,1.6,0},{0.6,1.2,0},
                    {-0.3,0,0},{-0.3,-0.4,0},{0.3,0,0}};
            for (int i = 0; i < cloneBlocks.size(); i++) {
                cloneBlocks.get(i).entity().teleport(c.clone().add(offsets[i][0] + sway, offsets[i][1], offsets[i][2]));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(sway, 1, 0), 4, 1.0, 20, 0, 40, 1.2f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowClone(plugin); }
    }

    // ================================================================
    // 2. NIGHTMARE CONSTRUCT — Sculk + blackstone large twisted shape
    // ================================================================
    public static class NightmareConstruct extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> constructBlocks = new ArrayList<>();

        public NightmareConstruct(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_construct", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
                Material mat = i % 2 == 0 ? Material.SCULK : Material.BLACKSTONE;
                double angle = i * 0.8;
                double r = 1.0 + (i % 4) * 0.5;
                double y = i * 0.4;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.5f, 0.6f).glow(20, 0, 40).interpolation(3, 0);
                constructBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_ANGRY, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double twist = ticksAlive * 0.02;
            for (int i = 0; i < constructBlocks.size(); i++) {
                double angle = i * 0.8 + twist;
                double r = 1.0 + (i % 4) * 0.5;
                double y = i * 0.4;
                constructBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 3.0, 20, 0, 40, 1.2f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NightmareConstruct(plugin); }
    }

    // ================================================================
    // 3. PHANTOM WALL — Blackstone wall sweeping through area
    // ================================================================
    public static class PhantomWall extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> wallBlocks = new ArrayList<>();
        private double offsetX = 0;

        public PhantomWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_wall", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int z = 0; z < 7; z++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add(-8, y * 0.8, (z - 3) * 0.8);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.8f, 0.8f, 0.8f).glow(20, 0, 40).interpolation(3, 0);
                    wallBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            offsetX = -8;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_STEP, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            offsetX += 0.12;
            int idx = 0;
            for (int z = 0; z < 7; z++) {
                for (int y = 0; y < 2; y++) {
                    wallBlocks.get(idx).entity().teleport(c.clone().add(offsetX, y * 0.8, (z - 3) * 0.8));
                    idx++;
                }
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(offsetX, 1, 0), 6, 3.0, 20, 0, 40, 1.2f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_STEP, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PhantomWall(plugin); }
    }

    // ================================================================
    // 4. DARK MIRROR — Obsidian + purple glass mirror reflecting particles
    // ================================================================
    public static class DarkMirror extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> mirrorBlocks = new ArrayList<>();

        public DarkMirror(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_mirror", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Obsidian frame (8) + purple glass surface (8)
            double[][] frameOff = {{-1.5,0,0},{-1.5,1,0},{-1.5,2,0},{-1.5,3,0},{1.5,0,0},{1.5,1,0},{1.5,2,0},{1.5,3,0}};
            for (double[] off : frameOff) {
                Location loc = center.clone().add(off[0], off[1], off[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.4f, 0.8f, 0.4f).glow(20, 0, 40).interpolation(3, 0);
                mirrorBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 2; y++) {
                    Location loc = center.clone().add((x - 1.5) * 0.7, y * 1.2 + 0.5, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.7f, 1.0f, 0.1f).glow(80, 0, 160).interpolation(3, 0);
                    mirrorBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Reflecting particles
            if (ticksAlive % 4 == 0) {
                double oy = 0.5 + Math.random() * 2.5;
                Location mirrorPoint = c.clone().add((Math.random() - 0.5) * 2, oy, 0);
                DisplayBuilder.dustParticles(mirrorPoint, 3, 0.3, 200, 0, 180, 1.5f);
                // Reflected beam
                for (int d = 1; d < 5; d++) {
                    DisplayBuilder.dustParticles(mirrorPoint.clone().add(0, 0, d * 0.8), 1, 0.2, 200, 0, 50, 1.0f);
                }
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkMirror(plugin); }
    }

    // ================================================================
    // 5. FEAR TOTEM — Sculk + crying obsidian totem pole, escalating damage
    // ================================================================
    public static class FearTotem extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> totemBlocks = new ArrayList<>();

        public FearTotem(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fear_totem", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Material mat = i % 3 == 0 ? Material.CRYING_OBSIDIAN : Material.SCULK;
                Location loc = center.clone().add(0, i * 0.6, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float s = 0.7f - (i * 0.03f);
                h.scale(s, 0.6f, s).glow(80, 0, 160).interpolation(3, 0);
                totemBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_HEARTBEAT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Escalate damage over time: 4 -> 10
            double progress = Math.min(1.0, ticksAlive / (double) config.getDurationTicks());
            config.setDamage(4.0 + progress * 6.0);
            // Pulse glow intensity increases
            if (ticksAlive % 6 == 0) {
                int intensity = (int) (80 + progress * 120);
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 2.0, intensity, 0, (int) (160 - progress * 110), 1.2f);
            }
            if (ticksAlive % 20 == 0) {
                float pitch = 0.5f + (float) progress * 1.0f;
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_HEARTBEAT, 0.5f, pitch);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FearTotem(plugin); }
    }

    // ================================================================
    // 6. NIGHTMARE GATE — Crying obsidian + blackstone archway, pull + damage
    // ================================================================
    public static class NightmareGate extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> gateBlocks = new ArrayList<>();

        public NightmareGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_gate", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Left pillar: 5 crying obsidian
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-2.5, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.8f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right pillar
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(2.5, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.8f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Arch: 8 blackstone
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 7;
                Location loc = center.clone().add(Math.cos(angle) * 2.5, 4 + Math.sin(angle) * 1.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.5f, 0.5f, 0.5f).glow(20, 0, 40).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.6f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Pull effect
            if (ticksAlive % 4 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 25) {
                        Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.1);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 2.0, 200, 0, 50, 1.2f);
            }
            if (ticksAlive % 6 == 0) {
                c.getWorld().spawnParticle(Particle.PORTAL, c.clone().add(0, 2, 0), 5, 1.0, 1.5, 0.5, 0.1);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_PORTAL_AMBIENT, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NightmareGate(plugin); }
    }

    // ================================================================
    // 7. SHADOW SPIKE — Blackstone spikes erupting from ground in line
    // ================================================================
    public static class ShadowSpike extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private int lastSpikeIndex = 0;

        public ShadowSpike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_spike", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(70);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add((i - 6.5) * 1.0, -2, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 2.0f, 0.4f).glow(20, 0, 40).interpolation(3, 0);
                spikeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            lastSpikeIndex = 0;
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_EMERGE, 0.6f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            int currentSpike = Math.min(13, ticksAlive / 5);
            if (currentSpike > lastSpikeIndex) {
                for (int i = lastSpikeIndex; i <= currentSpike; i++) {
                    spikeBlocks.get(i).entity().teleport(c.clone().add((i - 6.5) * 1.0, 0, 0));
                    Location spikeLoc = c.clone().add((i - 6.5) * 1.0, 0, 0);
                    triggerImpactDamage(spikeLoc);
                    DisplayBuilder.dustParticles(spikeLoc, 6, 1.0, 20, 0, 40, 1.5f);
                    DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_BASALT_BREAK, 0.5f, 0.5f);
                }
                lastSpikeIndex = currentSpike;
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowSpike(plugin); }
    }

    // ================================================================
    // 8. DARK LANTERN — Sculk + sea lantern hanging, emits dark particles
    // ================================================================
    public static class DarkLantern extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> lanternBlocks = new ArrayList<>();

        public DarkLantern(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_lantern", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Chain: 4 sculk
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, 6 + i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SCULK);
                h.scale(0.2f, 0.5f, 0.2f).glow(20, 0, 40).interpolation(3, 0);
                lanternBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Lantern body: 4 sculk frame + 2 sea lantern glow
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 0.4, 5.5, Math.sin(angle) * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SCULK);
                h.scale(0.3f, 0.5f, 0.3f).glow(20, 0, 40).interpolation(3, 0);
                lanternBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0, 5.5 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.4f, 0.3f, 0.4f).glow(200, 0, 180).interpolation(3, 0);
                lanternBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Sway
            double sway = Math.sin(ticksAlive * 0.04) * 0.3;
            for (BlockDisplayHandle h : lanternBlocks) {
                Location loc = h.entity().getLocation();
                h.entity().teleport(new Location(c.getWorld(), c.getX() + sway, loc.getY(), c.getZ()));
            }
            // Dark particle drip
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(sway, 5, 0), 4, 1.0, 20, 0, 40, 1.5f);
                DisplayBuilder.dustParticles(c.clone().add(sway, 3, 0), 2, 0.5, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkLantern(plugin); }
    }

    // ================================================================
    // 9. VISION SHATTER — Purple glass sphere that shatters, fragments fly outward
    // ================================================================
    public static class VisionShatter extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shatterBlocks = new ArrayList<>();
        private boolean shattered = false;

        public VisionShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vision_shatter", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 16; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double r = 1.5;
                Location loc = center.clone().add(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 3, Math.sin(theta) * Math.sin(phi) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                shatterBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive < 20) {
                // Pulse before shatter
                float scale = 0.4f + (ticksAlive / 20.0f) * 0.3f;
                for (BlockDisplayHandle h : shatterBlocks) {
                    h.scale(scale, scale, scale).interpolation(3, 0);
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 6, 1.5, 200, 0, 180, 1.5f);
                }
            }
            if (!shattered && ticksAlive >= 20) {
                shattered = true;
                for (int i = 0; i < shatterBlocks.size(); i++) {
                    double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                    double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                    double r = 6.0;
                    shatterBlocks.get(i).entity().teleport(c.clone().add(
                            Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r + 3, Math.sin(theta) * Math.sin(phi) * r));
                }
                triggerImpactDamage(c.clone().add(0, 3, 0));
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 25, 5.0, 80, 0, 160, 2.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VisionShatter(plugin); }
    }

    // ================================================================
    // 10. CREEPING SHADOW — Blackstone blocks spreading on ground
    // ================================================================
    public static class CreepingShadow extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shadowBlocks = new ArrayList<>();
        private int currentRadius = 0;

        public CreepingShadow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_shadow", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(15);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Start with center block
            BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.BLACKSTONE);
            h.scale(0.8f, 0.1f, 0.8f).glow(20, 0, 40).interpolation(3, 0);
            shadowBlocks.add(h);
            spawnedEntities.add(h.entity());
            currentRadius = 0;
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Expand every 20 ticks, up to 12 blocks total
            int targetRadius = Math.min(5, ticksAlive / 20);
            if (targetRadius > currentRadius && shadowBlocks.size() < 12) {
                currentRadius = targetRadius;
                int newBlocks = Math.min(3, 12 - shadowBlocks.size());
                for (int i = 0; i < newBlocks; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double r = currentRadius * 0.8;
                    Location loc = c.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.8f, 0.1f, 0.8f).glow(20, 0, 40).interpolation(3, 0);
                    shadowBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
                config.setDamageRadius(2.0 + currentRadius);
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.3f, 0.5f);
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 4, (float) (2 + currentRadius), 20, 0, 40, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CreepingShadow(plugin); }
    }

    // ================================================================
    // 11. NIGHTMARE CAGE — Sculk bars enclosing
    // ================================================================
    public static class NightmareCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cageBlocks = new ArrayList<>();

        public NightmareCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_cage", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 5, 0, Math.sin(angle) * 5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SCULK);
                h.scale(0.3f, 2.5f, 0.3f).glow(20, 0, 40).interpolation(3, 0);
                cageBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double radius = 5.0 - (ticksAlive / (double) config.getDurationTicks()) * 4.0;
            radius = Math.max(0.5, radius);
            for (int i = 0; i < cageBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 16;
                cageBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c, 6, (float) radius, 20, 0, 40, 1.0f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_BREAK, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NightmareCage(plugin); }
    }

    // ================================================================
    // 12. DARK SENTINEL — Blackstone + obsidian standing figure, tracks player
    // ================================================================
    public static class DarkSentinel extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> sentinelBlocks = new ArrayList<>();

        public DarkSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_sentinel", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Tall sentinel: 8 blackstone body + 4 obsidian accents + 2 eye-red amethyst eyes
            for (int i = 0; i < 8; i++) {
                Location loc = center.clone().add(0, i * 0.5, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                float s = i < 2 ? 0.8f : (i > 6 ? 0.5f : 0.6f);
                h.scale(s, 0.5f, s).glow(20, 0, 40).interpolation(3, 0);
                sentinelBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 0.5, 2, Math.sin(angle) * 0.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 0.8f, 0.3f).glow(20, 0, 40).interpolation(3, 0);
                sentinelBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Eyes
            Location leftEye = center.clone().add(-0.2, 3.8, -0.4);
            BlockDisplayHandle le = displayBuilder.spawnBlock(leftEye, Material.AMETHYST_BLOCK);
            le.scale(0.15f, 0.15f, 0.15f).glow(200, 0, 50).interpolation(3, 0);
            sentinelBlocks.add(le);
            spawnedEntities.add(le.entity());
            Location rightEye = center.clone().add(0.2, 3.8, -0.4);
            BlockDisplayHandle re = displayBuilder.spawnBlock(rightEye, Material.AMETHYST_BLOCK);
            re.scale(0.15f, 0.15f, 0.15f).glow(200, 0, 50).interpolation(3, 0);
            sentinelBlocks.add(re);
            spawnedEntities.add(re.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_AMBIENT, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Eye tracking glow
            if (ticksAlive % 4 == 0) {
                Player target = getTargetPlayer();
                if (target != null && target.isOnline()) {
                    Location eyeCenter = c.clone().add(0, 3.8, 0);
                    Vector dir = target.getLocation().add(0, 1, 0).toVector().subtract(eyeCenter.toVector()).normalize();
                    for (int d = 1; d < 4; d++) {
                        DisplayBuilder.dustParticles(eyeCenter.clone().add(dir.clone().multiply(d)), 1, 0.2, 200, 0, 50, 1.0f);
                    }
                }
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 4, 1.0, 20, 0, 40, 1.0f);
            }
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_AMBIENT, 0.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSentinel(plugin); }
    }
}
