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
 * Seer Mode — EYE STRUCTURES
 * 13 block display attacks themed around eyes, vision, and sight.
 * Palette: Deep purple (80,0,160), Eye red (200,0,50), Void black (20,0,40),
 *          Psychic magenta (200,0,180), Pale eye white (240,230,255)
 */
public final class EyeStructures {

    private EyeStructures() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FloatingEye(plugin));
        registry.register(new IrisRing(plugin));
        registry.register(new PupilBeam(plugin));
        registry.register(new EyelidSlam(plugin));
        registry.register(new AllSeeingOrb(plugin));
        registry.register(new EyeStorm(plugin));
        registry.register(new GazePillar(plugin));
        registry.register(new VisionCage(plugin));
        registry.register(new RetinaBurn(plugin));
        registry.register(new CorneaShield(plugin));
        registry.register(new TearDrop(plugin));
        registry.register(new BlinkingFlash(plugin));
        registry.register(new OcularVortex(plugin));
    }

    // ================================================================
    // 1. FLOATING EYE — Amethyst+purple glass sphere with black pupil, rotates to face player
    // ================================================================
    public static class FloatingEye extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();

        public FloatingEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("floating_eye", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Outer sphere: 10 amethyst + 3 purple glass
            for (int i = 0; i < 10; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 13);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double r = 2.0;
                Location loc = center.clone().add(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                eyeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            for (int i = 10; i < 13; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 13);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double r = 2.0;
                Location loc = center.clone().add(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 180).interpolation(3, 0);
                eyeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Pupil: 1 obsidian center
            BlockDisplayHandle pupil = displayBuilder.spawnBlock(center, Material.OBSIDIAN);
            pupil.scale(0.8f, 0.8f, 0.8f).glow(20, 0, 40).interpolation(3, 0);
            eyeBlocks.add(pupil);
            spawnedEntities.add(pupil.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double rot = Math.toRadians(ticksAlive * 2.0);
            for (int i = 0; i < Math.min(13, eyeBlocks.size()); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 13);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + rot;
                double r = 2.0;
                eyeBlocks.get(i).entity().teleport(c.clone().add(
                        Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, 2.0, 200, 0, 50, 1.2f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FloatingEye(plugin); }
    }

    // ================================================================
    // 2. IRIS RING — Purple glass ring that contracts and expands
    // ================================================================
    public static class IrisRing extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();

        public IrisRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iris_ring", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                Location loc = center.clone().add(Math.cos(angle) * 3.0, 0, Math.sin(angle) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(80, 0, 160).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double pulse = 3.0 + 1.5 * Math.sin(ticksAlive * 0.08);
            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 16;
                ringBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * pulse, 0, Math.sin(angle) * pulse));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c, 8, (float) pulse, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.3f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new IrisRing(plugin); }
    }

    // ================================================================
    // 3. PUPIL BEAM — Obsidian+sculk vertical column with warning circle
    // ================================================================
    public static class PupilBeam extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> beamBlocks = new ArrayList<>();
        private boolean impacted = false;

        public PupilBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pupil_beam", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 12; i++) {
                Material mat = i % 2 == 0 ? Material.OBSIDIAN : Material.SCULK;
                Location loc = center.clone().add(0, 10 + i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.8f, 0.4f).glow(20, 0, 40).interpolation(3, 0);
                beamBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Warning circle on ground
            if (ticksAlive < 20 && ticksAlive % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 3.0, Particle.DUST,
                        12, new Particle.DustOptions(Color.fromRGB(200, 0, 50), 1.2f));
            }
            // Beam descends
            double dropProgress = Math.min(1.0, ticksAlive / 20.0);
            for (int i = 0; i < beamBlocks.size(); i++) {
                double targetY = i * 0.8;
                double currentY = 10 + i * 0.8 - (10 * dropProgress);
                beamBlocks.get(i).entity().teleport(c.clone().add(0, currentY, 0));
            }
            if (!impacted && ticksAlive >= 20) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 20, 3.0, 200, 0, 50, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PupilBeam(plugin); }
    }

    // ================================================================
    // 4. EYELID SLAM — Blackstone upper+lower lids slam shut
    // ================================================================
    public static class EyelidSlam extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> upperLid = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerLid = new ArrayList<>();
        private boolean impacted = false;

        public EyelidSlam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eyelid_slam", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(60);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 9; i++) {
                double x = (i - 4) * 0.7;
                Location upper = center.clone().add(x, 5, 0);
                BlockDisplayHandle hu = displayBuilder.spawnBlock(upper, Material.BLACKSTONE);
                hu.scale(0.7f, 0.5f, 0.7f).glow(20, 0, 40).interpolation(3, 0);
                upperLid.add(hu);
                spawnedEntities.add(hu.entity());
                Location lower = center.clone().add(x, -2, 0);
                BlockDisplayHandle hl = displayBuilder.spawnBlock(lower, Material.BLACKSTONE);
                hl.scale(0.7f, 0.5f, 0.7f).glow(20, 0, 40).interpolation(3, 0);
                lowerLid.add(hl);
                spawnedEntities.add(hl.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double closeProgress = Math.min(1.0, ticksAlive / 30.0);
            double upperY = 5 - (closeProgress * 3.5);
            double lowerY = -2 + (closeProgress * 3.5);
            for (int i = 0; i < 9; i++) {
                double x = (i - 4) * 0.7;
                upperLid.get(i).entity().teleport(c.clone().add(x, upperY, 0));
                lowerLid.get(i).entity().teleport(c.clone().add(x, lowerY, 0));
            }
            if (!impacted && ticksAlive >= 30) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 15, 4.0, 20, 0, 40, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyelidSlam(plugin); }
    }

    // ================================================================
    // 5. ALL SEEING ORB — Amethyst sphere at Y+8, emits tracking particles
    // ================================================================
    public static class AllSeeingOrb extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> orbBlocks = new ArrayList<>();

        public AllSeeingOrb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("all_seeing_orb", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location orbCenter = center.clone().add(0, 8, 0);
            for (int i = 0; i < 16; i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i;
                double r = 1.5;
                Location loc = orbCenter.clone().add(Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.AMETHYST_BLOCK);
                h.scale(0.4f, 0.4f, 0.4f).glow(240, 230, 255).interpolation(3, 0);
                orbBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location orbCenter = c.clone().add(0, 8, 0);
            double rot = Math.toRadians(ticksAlive * 1.5);
            for (int i = 0; i < orbBlocks.size(); i++) {
                double phi = Math.acos(1 - 2.0 * (i + 0.5) / 16);
                double theta = Math.PI * (1 + Math.sqrt(5)) * i + rot;
                double r = 1.5;
                orbBlocks.get(i).entity().teleport(orbCenter.clone().add(
                        Math.cos(theta) * Math.sin(phi) * r, Math.cos(phi) * r, Math.sin(theta) * Math.sin(phi) * r));
            }
            // Tracking particles toward players
            if (ticksAlive % 4 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 36) {
                        Vector dir = p.getLocation().add(0, 1, 0).toVector().subtract(orbCenter.toVector()).normalize();
                        for (int s = 0; s < 5; s++) {
                            Location pLoc = orbCenter.clone().add(dir.clone().multiply(s * 1.5));
                            DisplayBuilder.dustParticles(pLoc, 1, 0.2, 200, 0, 180, 1.0f);
                        }
                    }
                }
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(orbCenter, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AllSeeingOrb(plugin); }
    }

    // ================================================================
    // 6. EYE STORM — Small purple glass orbs orbiting center rapidly
    // ================================================================
    public static class EyeStorm extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> stormBlocks = new ArrayList<>();

        public EyeStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eye_storm", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int i = 0; i < 15; i++) {
                double angle = (2 * Math.PI * i) / 15;
                double r = 3.0 + (i % 3) * 0.5;
                Location loc = center.clone().add(Math.cos(angle) * r, 1 + (i % 3) * 0.5, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                h.scale(0.3f, 0.3f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                stormBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double speed = ticksAlive * 0.15;
            for (int i = 0; i < stormBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 15 + speed;
                double r = 3.0 + (i % 3) * 0.5;
                double y = 1 + (i % 3) * 0.5 + Math.sin(ticksAlive * 0.1 + i) * 0.5;
                stormBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 6, 4.0, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_VEX_AMBIENT, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EyeStorm(plugin); }
    }

    // ================================================================
    // 7. GAZE PILLAR — Tall crying obsidian pillar with purple beam from top
    // ================================================================
    public static class GazePillar extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();

        public GazePillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gaze_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int i = 0; i < 12; i++) {
                Location loc = center.clone().add(0, i * 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                h.scale(0.6f, 0.8f, 0.6f).glow(80, 0, 160).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            Location top = c.clone().add(0, 10, 0);
            // Purple beam from top sweeping
            if (ticksAlive % 2 == 0) {
                double angle = ticksAlive * 0.05;
                for (int d = 0; d < 8; d++) {
                    Location beamPoint = top.clone().add(Math.cos(angle) * d * 0.5, -d * 0.3, Math.sin(angle) * d * 0.5);
                    DisplayBuilder.dustParticles(beamPoint, 2, 0.2, 80, 0, 160, 1.5f);
                }
            }
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(top, 4, 1.0, 200, 0, 50, 1.2f);
            }
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BEACON_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GazePillar(plugin); }
    }

    // ================================================================
    // 8. VISION CAGE — Purple glass bars around player, contracts
    // ================================================================
    public static class VisionCage extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> cageBlocks = new ArrayList<>();

        public VisionCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("vision_cage", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
                for (int y = 0; y < 1; y++) {
                    Location loc = center.clone().add(Math.cos(angle) * 4.0, y * 2, Math.sin(angle) * 4.0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPLE_STAINED_GLASS);
                    h.scale(0.3f, 2.0f, 0.3f).glow(80, 0, 160).interpolation(3, 0);
                    cageBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double radius = 4.0 - (ticksAlive / (double) config.getDurationTicks()) * 3.0;
            radius = Math.max(0.5, radius);
            for (int i = 0; i < cageBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 16;
                cageBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius));
            }
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c, 8, (float) radius, 200, 0, 180, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_STEP, 0.3f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VisionCage(plugin); }
    }

    // ================================================================
    // 9. RETINA BURN — Magenta glass compressed then explode outward
    // ================================================================
    public static class RetinaBurn extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> burnBlocks = new ArrayList<>();
        private boolean exploded = false;

        public RetinaBurn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("retina_burn", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(400);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                Location loc = center.clone().add(Math.cos(angle) * 3.0, 1, Math.sin(angle) * 3.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.MAGENTA_STAINED_GLASS);
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 0, 180).interpolation(3, 0);
                burnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (ticksAlive < 20) {
                double radius = 3.0 * (1.0 - ticksAlive / 20.0);
                for (int i = 0; i < burnBlocks.size(); i++) {
                    double angle = (2 * Math.PI * i) / 10;
                    burnBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * radius, 1, Math.sin(angle) * radius));
                }
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.dustParticles(c, 6, (float) radius, 200, 0, 180, 1.5f);
                }
            }
            if (!exploded && ticksAlive >= 20) {
                exploded = true;
                for (int i = 0; i < burnBlocks.size(); i++) {
                    double angle = (2 * Math.PI * i) / 10;
                    burnBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * 6.0, 1, Math.sin(angle) * 6.0));
                }
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 25, 5.0, 200, 0, 50, 2.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RetinaBurn(plugin); }
    }

    // ================================================================
    // 10. CORNEA SHIELD — Purpur curved wall sliding toward player
    // ================================================================
    public static class CorneaShield extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> shieldBlocks = new ArrayList<>();
        private double offsetZ = 0;

        public CorneaShield(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cornea_shield", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int i = 0; i < 14; i++) {
                double angle = Math.toRadians(-60 + (120.0 * i / 13));
                double r = 3.5;
                Location loc = center.clone().add(Math.cos(angle) * r, (i % 3) * 0.8, Math.sin(angle) * r - 6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PURPUR_BLOCK);
                h.scale(0.6f, 0.8f, 0.4f).glow(200, 0, 180).interpolation(3, 0);
                shieldBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            offsetZ = -6;
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            offsetZ += 0.08;
            for (int i = 0; i < shieldBlocks.size(); i++) {
                double angle = Math.toRadians(-60 + (120.0 * i / 13));
                double r = 3.5;
                shieldBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, (i % 3) * 0.8, Math.sin(angle) * r + offsetZ));
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 1, offsetZ), 6, 3.0, 80, 0, 160, 1.0f);
            }
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_STEP, 0.3f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CorneaShield(plugin); }
    }

    // ================================================================
    // 11. TEAR DROP — Blue ice + purple glass falling from Y+20
    // ================================================================
    public static class TearDrop extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> dropBlocks = new ArrayList<>();
        private boolean impacted = false;

        public TearDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tear_drop", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(40);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            Location high = center.clone().add(0, 20, 0);
            for (int i = 0; i < 12; i++) {
                Material mat = i < 6 ? Material.BLUE_ICE : Material.PURPLE_STAINED_GLASS;
                double ox = (i % 4 - 1.5) * 0.4;
                double oy = -(i / 4) * 0.5;
                Location loc = high.clone().add(ox, oy, ox * 0.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.5f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                dropBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.6f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double dropY = 20 - (ticksAlive / 20.0) * 20;
            dropY = Math.max(0, dropY);
            for (int i = 0; i < dropBlocks.size(); i++) {
                double ox = (i % 4 - 1.5) * 0.4;
                double oy = -(i / 4) * 0.5;
                dropBlocks.get(i).entity().teleport(c.clone().add(ox, dropY + oy, ox * 0.3));
            }
            if (!impacted && dropY <= 0) {
                impacted = true;
                triggerImpactDamage(c);
                DisplayBuilder.dustParticles(c, 20, 3.0, 80, 0, 160, 2.0f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.8f);
            }
            if (ticksAlive % 3 == 0 && !impacted) {
                DisplayBuilder.dustParticles(c.clone().add(0, dropY, 0), 4, 0.5, 200, 0, 180, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TearDrop(plugin); }
    }

    // ================================================================
    // 12. BLINKING FLASH — Sea lantern + amethyst alternating bright/dark
    // ================================================================
    public static class BlinkingFlash extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> flashBlocks = new ArrayList<>();

        public BlinkingFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blinking_flash", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 14; i++) {
                Material mat = i % 2 == 0 ? Material.SEA_LANTERN : Material.AMETHYST_BLOCK;
                double angle = (2 * Math.PI * i) / 14;
                double r = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * r, 2, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.5f, 0.5f, 0.5f).glow(240, 230, 255).interpolation(3, 0);
                flashBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            boolean bright = (ticksAlive / 10) % 2 == 0;
            for (int i = 0; i < flashBlocks.size(); i++) {
                if (bright) {
                    flashBlocks.get(i).glow(240, 230, 255).interpolation(3, 0);
                } else {
                    flashBlocks.get(i).glow(20, 0, 40).interpolation(3, 0);
                }
                double angle = (2 * Math.PI * i) / 14 + ticksAlive * 0.02;
                flashBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * 2.5, 2, Math.sin(angle) * 2.5));
            }
            if (bright && ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c, 15, 6.0, 240, 230, 255, 2.0f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 2.0f);
            }
            if (!bright && ticksAlive % 10 == 0) {
                DisplayBuilder.dustParticles(c, 8, 4.0, 20, 0, 40, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BlinkingFlash(plugin); }
    }

    // ================================================================
    // 13. OCULAR VORTEX — Amethyst+obsidian spiraling funnel with pull effect
    // ================================================================
    public static class OcularVortex extends BlockDisplayAttack {
        private final List<BlockDisplayHandle> vortexBlocks = new ArrayList<>();

        public OcularVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ocular_vortex", AttackType.BLOCK_DISPLAY, 1, "modes/seer/attacks"));
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
            for (int i = 0; i < 18; i++) {
                Material mat = i % 2 == 0 ? Material.AMETHYST_BLOCK : Material.OBSIDIAN;
                double angle = (2 * Math.PI * i) / 18;
                double r = 3.0;
                double y = (i / 18.0) * 4.0;
                Location loc = center.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 0.4f, 0.4f).glow(80, 0, 160).interpolation(3, 0);
                vortexBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            double spin = ticksAlive * 0.12;
            for (int i = 0; i < vortexBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 18 + spin;
                double r = 3.0 - (i / 18.0) * 1.5;
                double y = (i / 18.0) * 4.0;
                vortexBlocks.get(i).entity().teleport(c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r));
            }
            // Pull effect on nearby players
            if (ticksAlive % 4 == 0) {
                for (Player p : c.getWorld().getPlayers()) {
                    if (isExempt(p)) continue;
                    if (p.getLocation().distanceSquared(c) <= 25) {
                        Vector pull = c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.15);
                        p.setVelocity(p.getVelocity().add(pull));
                    }
                }
            }
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 8, 3.0, 200, 0, 180, 1.2f);
            }
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new OcularVortex(plugin); }
    }
}
