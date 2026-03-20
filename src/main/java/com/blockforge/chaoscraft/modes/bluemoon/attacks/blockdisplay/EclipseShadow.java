package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Blue Moon Mode — ECLIPSE SHADOW
 * 13 dark eclipse/shadow block display attacks.
 * Materials: DEEPSLATE, CRYING_OBSIDIAN, BLACK_CONCRETE, GRAY_CONCRETE, SEA_LANTERN
 * Particle colors: Deep indigo (60, 60, 160), Dark purple (80, 0, 120)
 * Sounds: AMBIENT_CAVE, BLOCK_SCULK_CATALYST_BLOOM, BLOCK_SCULK_SPREAD
 */
public final class EclipseShadow {

    private EclipseShadow() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowObelisk(plugin));
        registry.register(new EclipseDisc(plugin));
        registry.register(new UmbraSpikes(plugin));
        registry.register(new PenumbraRing(plugin));
        registry.register(new VoidTendril(plugin));
        registry.register(new DarkSideHemisphere(plugin));
        registry.register(new ShadowClone(plugin));
        registry.register(new NightmareGate(plugin));
        registry.register(new EclipseHammer(plugin));
        registry.register(new CreepingDark(plugin));
        registry.register(new LunarVoidRift(plugin));
        registry.register(new ShadowSentinel(plugin));
        registry.register(new Totality(plugin));
    }

    // ================================================================
    // 1. SHADOW OBELISK — 16 DEEPSLATE+CRYING_OBSIDIAN tall obelisk, dark particles
    // ================================================================
    public static class ShadowObelisk extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> obeliskBlocks = new ArrayList<>();

        public ShadowObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_obelisk", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(5.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tapered obelisk: wider base, narrow top, 10 blocks tall
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double y = t * 10.0;
                double taper = 1.0 - t * 0.6; // narrows toward top
                double angle = (Math.PI * 2 * (i % 4)) / 4.0 + t * 0.3;
                double radius = 0.6 * taper;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, y, z);
                Material mat = i % 3 == 0 ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.6f * (float) taper + 0.2f;
                h.scale(scale, 0.7f, scale).glow(60, 60, 160).interpolation(3, 0);
                obeliskBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow rotation of the entire obelisk
            double rotAngle = Math.toRadians(ticksAlive * 0.5);

            for (int i = 0; i < obeliskBlocks.size(); i++) {
                double t = i / 15.0;
                double y = t * 10.0;
                double taper = 1.0 - t * 0.6;
                double baseAngle = (Math.PI * 2 * (i % 4)) / 4.0 + t * 0.3;
                double angle = baseAngle + rotAngle;
                double radius = 0.6 * taper;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                obeliskBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Dark particles radiate outward
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 10, 2.0, 60, 60, 160, 1.5f);
                DisplayBuilder.dustParticles(c, 6, 4.0, 80, 0, 120, 1.0f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowObelisk(plugin); }
    }

    // ================================================================
    // 2. ECLIPSE DISC — 14 BLACK_CONCRETE center + SEA_LANTERN corona, hovers
    // ================================================================
    public static class EclipseDisc extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> coronaBlocks = new ArrayList<>();

        public EclipseDisc(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_disc", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Dark center: 6 BLACK_CONCRETE in tight disc
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6.0;
                double radius = 0.6;
                Location loc = center.clone().add(Math.cos(angle) * radius, 6, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.7f, 0.3f, 0.7f).glow(20, 20, 30).interpolation(3, 0);
                coreBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Corona ring: 8 SEA_LANTERN around the edge
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double radius = 2.5;
                Location loc = center.clone().add(Math.cos(angle) * radius, 6, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.25f, 0.5f).glow(255, 200, 100).interpolation(3, 0);
                coronaBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double hoverY = 6.0 + Math.sin(ticksAlive * 0.04) * 0.5;
            double coronaSpin = Math.toRadians(ticksAlive * 1.5);

            // Core stays centered
            for (int i = 0; i < coreBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6.0;
                coreBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * 0.6, hoverY, Math.sin(angle) * 0.6));
            }

            // Corona rotates
            for (int i = 0; i < coronaBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8.0 + coronaSpin;
                double radius = 2.5 + Math.sin(ticksAlive * 0.1 + i) * 0.3;
                coronaBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * radius, hoverY, Math.sin(angle) * radius));
            }

            // Shadow damage particles beneath
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 10, 3.0, 60, 60, 160, 1.5f);
            }

            // Corona shimmer
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, hoverY, 0), 6, 2.5, 255, 200, 100, 0.8f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EclipseDisc(plugin); }
    }

    // ================================================================
    // 3. UMBRA SPIKES — 12 DEEPSLATE radial spikes, quick eruption then retract
    // ================================================================
    public static class UmbraSpikes extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final double[] spikeAngles = new double[12];

        public UmbraSpikes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("umbra_spikes", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 spikes radiating outward from center at ground level
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12.0;
                spikeAngles[i] = angle;
                double radius = 1.5 + (i % 3) * 0.8;
                Location loc = center.clone().add(Math.cos(angle) * radius, -1.5, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.35f, 2.0f, 0.35f).glow(60, 60, 160).interpolation(3, 0);
                // Tilt outward
                h.rotate(25f, (float) -Math.sin(angle), 0, (float) Math.cos(angle));
                spikeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double riseProgress;
            if (ticksAlive < 20) {
                // Quick eruption over 20 ticks
                riseProgress = ticksAlive / 20.0;
            } else if (ticksAlive < 100) {
                // Linger
                riseProgress = 1.0;
            } else {
                // Retract over 40 ticks
                riseProgress = 1.0 - Math.min(1.0, (ticksAlive - 100) / 40.0);
            }

            double y = -1.5 + riseProgress * 3.0;

            for (int i = 0; i < spikeBlocks.size(); i++) {
                double angle = spikeAngles[i];
                double radius = 1.5 + (i % 3) * 0.8;
                spikeBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius));
            }

            if (ticksAlive % 4 == 0 && riseProgress > 0.5) {
                DisplayBuilder.dustParticles(c, 8, 3.0, 80, 0, 120, 1.2f);
            }

            if (ticksAlive == 18) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new UmbraSpikes(plugin); }
    }

    // ================================================================
    // 4. PENUMBRA RING — 16 GRAY_CONCRETE+BLACK_CONCRETE ring descending from Y+10
    // ================================================================
    public static class PenumbraRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();

        public PenumbraRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("penumbra_ring", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 blocks in ring at Y+10
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16.0;
                double radius = 4.0;
                Location loc = center.clone().add(Math.cos(angle) * radius, 10, Math.sin(angle) * radius);
                Material mat = i % 2 == 0 ? Material.GRAY_CONCRETE : Material.BLACK_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.55f, 0.55f, 0.55f).glow(60, 60, 160).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descend from Y+10 to Y+1 over 120 ticks, then hover
            double descendProgress = Math.min(1.0, ticksAlive / 120.0);
            double currentY = 10.0 - descendProgress * 9.0;
            double rotAngle = Math.toRadians(ticksAlive * 2.0);

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 16.0 + rotAngle;
                double radius = 4.0;
                ringBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * radius, currentY, Math.sin(angle) * radius));
            }

            // Shadow damage zone beneath the ring
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY - 1, 0), 8, 3.5, 60, 60, 160, 1.2f);
            }

            if (ticksAlive % 3 == 0 && descendProgress > 0.5) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 6, 4.0, 80, 0, 120, 1.0f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PenumbraRing(plugin); }
    }

    // ================================================================
    // 5. VOID TENDRIL — 15 CRYING_OBSIDIAN curving tendril toward player, sways
    // ================================================================
    public static class VoidTendril extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tendrilBlocks = new ArrayList<>();
        private double dirX;
        private double dirZ;

        public VoidTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_tendril", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double dirAngle = Math.random() * Math.PI * 2;
            dirX = Math.cos(dirAngle);
            dirZ = Math.sin(dirAngle);

            // 15 segments curving outward and upward
            for (int i = 0; i < 15; i++) {
                double t = i / 14.0;
                double dist = t * 6.0;
                double y = t * 3.0 + Math.sin(t * Math.PI) * 2.0;
                double lateralSway = Math.sin(t * Math.PI * 2) * 1.0;
                double x = dirX * dist - dirZ * lateralSway;
                double z = dirZ * dist + dirX * lateralSway;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                float scale = (float)(0.5 - t * 0.2); // thins toward tip
                h.scale(scale, scale, scale).glow(80, 0, 120).interpolation(3, 0);
                tendrilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double swayPhase = ticksAlive * 0.06;

            for (int i = 0; i < tendrilBlocks.size(); i++) {
                double t = i / 14.0;
                double dist = t * 6.0;
                double y = t * 3.0 + Math.sin(t * Math.PI) * 2.0;
                // Dynamic sway increases toward tip
                double lateralSway = Math.sin(t * Math.PI * 2 + swayPhase) * (1.0 + t * 1.5);
                double verticalSway = Math.cos(swayPhase + i * 0.3) * t * 0.5;
                double x = dirX * dist - dirZ * lateralSway;
                double z = dirZ * dist + dirX * lateralSway;
                tendrilBlocks.get(i).entity().teleport(c.clone().add(x, y + verticalSway, z));
            }

            // Dark particles at tip
            if (ticksAlive % 3 == 0) {
                Location tip = tendrilBlocks.get(14).entity().getLocation();
                DisplayBuilder.dustParticles(tip, 4, 0.5, 80, 0, 120, 1.2f);
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 4, 1.5, 60, 60, 160, 0.8f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidTendril(plugin); }
    }

    // ================================================================
    // 6. DARK SIDE HEMISPHERE — 18 DEEPSLATE half-sphere, rotates to face player
    // ================================================================
    public static class DarkSideHemisphere extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> hemiBlocks = new ArrayList<>();
        private final double[][] localOffsets = new double[18][3];
        private double facingAngle;

        public DarkSideHemisphere(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_side_hemisphere", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            facingAngle = Math.random() * Math.PI * 2;

            // Half-sphere: 3 layers of blocks forming hemisphere
            int idx = 0;
            // Bottom ring: 8 blocks
            for (int i = 0; i < 8 && idx < 18; i++) {
                double angle = (Math.PI * i) / 7.0 - Math.PI / 2; // half circle
                localOffsets[idx][0] = Math.cos(angle) * 3.0;
                localOffsets[idx][1] = 0;
                localOffsets[idx][2] = Math.sin(angle) * 3.0;
                idx++;
            }
            // Middle ring: 6 blocks
            for (int i = 0; i < 6 && idx < 18; i++) {
                double angle = (Math.PI * i) / 5.0 - Math.PI / 2;
                localOffsets[idx][0] = Math.cos(angle) * 2.2;
                localOffsets[idx][1] = 1.5;
                localOffsets[idx][2] = Math.sin(angle) * 2.2;
                idx++;
            }
            // Top cap: 4 blocks
            for (int i = 0; i < 4 && idx < 18; i++) {
                double angle = (Math.PI * i) / 3.0 - Math.PI / 2;
                localOffsets[idx][0] = Math.cos(angle) * 1.2;
                localOffsets[idx][1] = 2.8;
                localOffsets[idx][2] = Math.sin(angle) * 1.2;
                idx++;
            }

            for (int i = 0; i < 18; i++) {
                double rx = localOffsets[i][0] * Math.cos(facingAngle) - localOffsets[i][2] * Math.sin(facingAngle);
                double rz = localOffsets[i][0] * Math.sin(facingAngle) + localOffsets[i][2] * Math.cos(facingAngle);
                Location loc = center.clone().add(rx, localOffsets[i][1], rz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.6f, 0.6f, 0.6f).glow(60, 60, 160).interpolation(3, 0);
                hemiBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly rotate facing angle
            facingAngle += 0.02;

            for (int i = 0; i < hemiBlocks.size(); i++) {
                double rx = localOffsets[i][0] * Math.cos(facingAngle) - localOffsets[i][2] * Math.sin(facingAngle);
                double rz = localOffsets[i][0] * Math.sin(facingAngle) + localOffsets[i][2] * Math.cos(facingAngle);
                hemiBlocks.get(i).entity().teleport(c.clone().add(rx, localOffsets[i][1], rz));
            }

            // Shadow in front of hemisphere
            if (ticksAlive % 4 == 0) {
                double frontX = Math.cos(facingAngle) * 2.0;
                double frontZ = Math.sin(facingAngle) * 2.0;
                DisplayBuilder.dustParticles(c.clone().add(frontX, 1, frontZ), 8, 2.0, 60, 60, 160, 1.3f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DarkSideHemisphere(plugin); }
    }

    // ================================================================
    // 7. SHADOW CLONE — 12 BLACK_CONCRETE vaguely humanoid, mimics with delay
    // ================================================================
    public static class ShadowClone extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cloneBlocks = new ArrayList<>();
        private final double[][] bodyOffsets = new double[12][3];
        private double wanderAngle;

        public ShadowClone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_clone", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            wanderAngle = Math.random() * Math.PI * 2;

            // Humanoid shape: head(2), torso(4), arms(4), legs(2)
            double[][] shape = {
                // Head
                {0, 1.8, 0}, {0, 2.1, 0},
                // Torso
                {0, 1.2, 0}, {0, 0.8, 0}, {0.15, 1.0, 0.1}, {-0.15, 1.0, -0.1},
                // Left arm
                {0.5, 1.4, 0}, {0.7, 1.0, 0},
                // Right arm
                {-0.5, 1.4, 0}, {-0.7, 1.0, 0},
                // Legs
                {0.2, 0.3, 0}, {-0.2, 0.3, 0}
            };

            for (int i = 0; i < 12; i++) {
                bodyOffsets[i][0] = shape[i][0];
                bodyOffsets[i][1] = shape[i][1];
                bodyOffsets[i][2] = shape[i][2];
                Location loc = center.clone().add(shape[i][0], shape[i][1], shape[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                float scale = i < 2 ? 0.45f : (i < 6 ? 0.5f : 0.3f);
                h.scale(scale, scale, scale).glow(30, 30, 50).interpolation(3, 0);
                cloneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Wander in circle with slight delay-mimicking motion
            wanderAngle += 0.03;
            double wanderRadius = 2.0 + Math.sin(ticksAlive * 0.02) * 1.5;
            double moveX = Math.cos(wanderAngle) * wanderRadius;
            double moveZ = Math.sin(wanderAngle) * wanderRadius;

            // Limb swing for walking
            double limbSwing = Math.sin(ticksAlive * 0.15) * 0.3;

            for (int i = 0; i < cloneBlocks.size(); i++) {
                double ox = bodyOffsets[i][0];
                double oy = bodyOffsets[i][1];
                double oz = bodyOffsets[i][2];

                // Add limb animation
                if (i >= 6 && i <= 7) oy += limbSwing;       // left arm
                if (i >= 8 && i <= 9) oy -= limbSwing;       // right arm
                if (i == 10) oy += limbSwing * 0.5;           // left leg
                if (i == 11) oy -= limbSwing * 0.5;           // right leg

                cloneBlocks.get(i).entity().teleport(c.clone().add(moveX + ox, oy, moveZ + oz));
            }

            // Shadow trail
            if (ticksAlive % 3 == 0) {
                Location pos = c.clone().add(moveX, 0.5, moveZ);
                DisplayBuilder.dustParticles(pos, 4, 0.5, 30, 30, 50, 1.0f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.3f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowClone(plugin); }
    }

    // ================================================================
    // 8. NIGHTMARE GATE — 16 CRYING_OBSIDIAN+DEEPSLATE archway, portal particles, pull
    // ================================================================
    public static class NightmareGate extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gateBlocks = new ArrayList<>();

        public NightmareGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_gate", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            // Archway: 2 pillars (5 each) + arch top (6)
            // Left pillar
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(-2.0, i * 1.2, 0);
                Material mat = i % 2 == 0 ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.7f, 0.6f).glow(80, 0, 120).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right pillar
            for (int i = 0; i < 5; i++) {
                Location loc = center.clone().add(2.0, i * 1.2, 0);
                Material mat = i % 2 == 0 ? Material.CRYING_OBSIDIAN : Material.DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.7f, 0.6f).glow(80, 0, 120).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Arch top: curved from left to right
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double x = -2.0 + t * 4.0;
                double y = 5.5 + Math.sin(t * Math.PI) * 1.5;
                Location loc = center.clone().add(x, y, 0);
                Material mat = i % 2 == 0 ? Material.DEEPSLATE : Material.CRYING_OBSIDIAN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.6f, 0.6f).glow(80, 0, 120).interpolation(3, 0);
                gateBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Portal particle effect inside the arch
            if (ticksAlive % 2 == 0) {
                double px = (Math.random() - 0.5) * 3.0;
                double py = 1.0 + Math.random() * 4.5;
                Location portalLoc = c.clone().add(px, py, 0);
                c.getWorld().spawnParticle(Particle.PORTAL, portalLoc, 5, 0.2, 0.2, 0.2, 0.1);
            }

            // Dark pull particles converging on gate center
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 10, 3.5, 80, 0, 120, 1.3f);
            }

            // Dripping crying obsidian particles
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 5, 0), 4, 1.5, 60, 60, 160, 0.8f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.6f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new NightmareGate(plugin); }
    }

    // ================================================================
    // 9. ECLIPSE HAMMER — 14 DEEPSLATE hammer, rises to Y+10, slams down
    // ================================================================
    public static class EclipseHammer extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> hammerBlocks = new ArrayList<>();
        private final double[][] hammerOffsets = new double[14][3];

        public EclipseHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("eclipse_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(16.0);
            config.setImpactRadius(4.5);
            config.setDurationTicks(180);
            config.setCooldownTicks(250);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Hammer head: 8 blocks (2x2x2 cube)
            int idx = 0;
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = 0; z < 2; z++) {
                        hammerOffsets[idx][0] = (x - 0.5) * 0.9;
                        hammerOffsets[idx][1] = 0 + y * 0.9;
                        hammerOffsets[idx][2] = (z - 0.5) * 0.9;
                        idx++;
                    }
                }
            }

            // Handle: 6 blocks vertical shaft
            for (int i = 0; i < 6; i++) {
                hammerOffsets[idx][0] = 0;
                hammerOffsets[idx][1] = -(i + 1) * 0.9;
                hammerOffsets[idx][2] = 0;
                idx++;
            }

            for (int i = 0; i < 14; i++) {
                Location loc = center.clone().add(hammerOffsets[i][0], hammerOffsets[i][1] + 2, hammerOffsets[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float scale = i < 8 ? 0.8f : 0.45f; // head is bigger
                h.scale(scale, scale, scale).glow(60, 60, 160).interpolation(3, 0);
                hammerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double currentY;
            if (ticksAlive < 40) {
                // Rise to Y+10 over 40 ticks
                currentY = 2.0 + (ticksAlive / 40.0) * 8.0;
            } else if (ticksAlive < 55) {
                // Hang briefly at top
                currentY = 10.0;
            } else if (ticksAlive < 65) {
                // SLAM down over 10 ticks (fast)
                double slamProgress = (ticksAlive - 55) / 10.0;
                currentY = 10.0 - slamProgress * 10.0;
            } else {
                currentY = 0;
            }

            for (int i = 0; i < hammerBlocks.size(); i++) {
                hammerBlocks.get(i).entity().teleport(
                    c.clone().add(hammerOffsets[i][0], hammerOffsets[i][1] + currentY, hammerOffsets[i][2]));
            }

            // Rising particles
            if (ticksAlive < 40 && ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY, 0), 6, 1.0, 60, 60, 160, 1.0f);
            }

            // Impact shockwave
            if (ticksAlive == 65) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.5f, 0.2f);
                DisplayBuilder.particleRing(c, 4.0, Particle.EXPLOSION, 10, null);
                DisplayBuilder.dustParticles(c, 20, 4.0, 80, 0, 120, 2.0f);
            }

            // Ominous hum while rising
            if (ticksAlive % 20 == 0 && ticksAlive < 55) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EclipseHammer(plugin); }
    }

    // ================================================================
    // 10. CREEPING DARK — 10 BLACK_CONCRETE ground level, spawns more at edges expanding
    // ================================================================
    public static class CreepingDark extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> darkBlocks = new ArrayList<>();
        private final List<double[]> blockPositions = new ArrayList<>();
        private int lastExpansionTick = 0;

        public CreepingDark(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("creeping_dark", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(3.0); // starts small, grows via block spread
            config.setDurationTicks(300);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Initial 10 blocks in small cluster at ground level
            for (int i = 0; i < 10; i++) {
                double angle = (Math.PI * 2 * i) / 10.0;
                double radius = 0.5 + Math.random() * 1.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0.05, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.6f, 0.1f, 0.6f).glow(20, 20, 30).interpolation(3, 0);
                darkBlocks.add(h);
                spawnedEntities.add(h.entity());
                blockPositions.add(new double[]{x, z});
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn new blocks at edges every 20 ticks, up to 30 total
            if (ticksAlive - lastExpansionTick >= 20 && darkBlocks.size() < 30) {
                lastExpansionTick = ticksAlive;
                // Pick a random edge block and spawn adjacent
                int edgeIdx = (int) (Math.random() * blockPositions.size());
                double[] edge = blockPositions.get(edgeIdx);
                double spreadAngle = Math.random() * Math.PI * 2;
                double nx = edge[0] + Math.cos(spreadAngle) * 0.8;
                double nz = edge[1] + Math.sin(spreadAngle) * 0.8;
                Location loc = c.clone().add(nx, 0.05, nz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.6f, 0.1f, 0.6f).glow(20, 20, 30).interpolation(3, 0);
                darkBlocks.add(h);
                spawnedEntities.add(h.entity());
                blockPositions.add(new double[]{nx, nz});
            }

            // Subtle undulating motion
            for (int i = 0; i < darkBlocks.size(); i++) {
                double[] pos = blockPositions.get(i);
                double yWave = 0.05 + Math.sin(ticksAlive * 0.1 + i * 0.5) * 0.03;
                darkBlocks.get(i).entity().teleport(c.clone().add(pos[0], yWave, pos[1]));
            }

            // Dark mist particles
            if (ticksAlive % 5 == 0) {
                double spread = 1.0 + (blockPositions.size() / 10.0) * 1.5;
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 6, spread, 30, 30, 50, 1.0f);
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CreepingDark(plugin); }
    }

    // ================================================================
    // 11. LUNAR VOID RIFT — 15 CRYING_OBSIDIAN vertical tear, pull effect
    // ================================================================
    public static class LunarVoidRift extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> riftBlocks = new ArrayList<>();

        public LunarVoidRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_void_rift", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 15 blocks forming jagged vertical rift / tear
            for (int i = 0; i < 15; i++) {
                double t = i / 14.0;
                double y = t * 7.0;
                // Jagged zigzag pattern
                double xJag = ((i % 2 == 0) ? 0.5 : -0.5) * (0.5 + Math.random() * 0.5);
                double zJag = Math.sin(t * Math.PI * 3) * 0.3;
                Location loc = center.clone().add(xJag, y, zJag);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CRYING_OBSIDIAN);
                float scale = 0.3f + 0.2f * (float) Math.sin(t * Math.PI); // wider in middle
                h.scale(scale, 0.5f, scale).glow(80, 0, 120).interpolation(3, 0);
                riftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.2f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rift blocks jitter/pulse
            for (int i = 0; i < riftBlocks.size(); i++) {
                double t = i / 14.0;
                double y = t * 7.0;
                double xJag = ((i % 2 == 0) ? 0.5 : -0.5) * (0.5 + Math.sin(ticksAlive * 0.2 + i) * 0.3);
                double zJag = Math.sin(t * Math.PI * 3 + ticksAlive * 0.05) * 0.3;
                riftBlocks.get(i).entity().teleport(c.clone().add(xJag, y, zJag));

                // Pulse scale
                float baseScale = 0.3f + 0.2f * (float) Math.sin(t * Math.PI);
                float pulse = 1.0f + 0.2f * (float) Math.sin(ticksAlive * 0.15 + i);
                riftBlocks.get(i).scale(baseScale * pulse, 0.5f, baseScale * pulse);
                riftBlocks.get(i).interpolation(2, 0);
            }

            // Pull effect particles converging on rift
            if (ticksAlive % 3 == 0) {
                double pullAngle = Math.random() * Math.PI * 2;
                double pullDist = 3.0 + Math.random() * 2.0;
                Location pullStart = c.clone().add(
                    Math.cos(pullAngle) * pullDist, 1.0 + Math.random() * 5.0, Math.sin(pullAngle) * pullDist);
                DisplayBuilder.dustParticles(pullStart, 3, 0.3, 80, 0, 120, 1.0f);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3.5, 0), 8, 1.0, 60, 60, 160, 1.5f);
            }

            if (ticksAlive % 45 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarVoidRift(plugin); }
    }

    // ================================================================
    // 12. SHADOW SENTINEL — 14 DEEPSLATE standing figure, rotates, dark beam
    // ================================================================
    public static class ShadowSentinel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> sentinelBlocks = new ArrayList<>();
        private final double[][] bodyOffsets = new double[14][3];
        private double rotAngle;

        public ShadowSentinel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_sentinel", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            rotAngle = Math.random() * Math.PI * 2;

            // Tall standing figure: base(3), body(5), shoulders(2), head(2), crown(2)
            double[][] shape = {
                // Base/feet
                {0.3, 0, 0}, {-0.3, 0, 0}, {0, 0, 0.2},
                // Body column
                {0, 0.8, 0}, {0, 1.6, 0}, {0.1, 2.4, 0.1}, {-0.1, 2.4, -0.1}, {0, 3.2, 0},
                // Shoulders
                {0.8, 3.2, 0}, {-0.8, 3.2, 0},
                // Head
                {0, 3.8, 0}, {0, 4.2, 0},
                // Crown spikes
                {0.3, 4.6, 0}, {-0.3, 4.6, 0}
            };

            for (int i = 0; i < 14; i++) {
                bodyOffsets[i][0] = shape[i][0];
                bodyOffsets[i][1] = shape[i][1];
                bodyOffsets[i][2] = shape[i][2];
                Location loc = center.clone().add(shape[i][0], shape[i][1], shape[i][2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float scale = (i < 3) ? 0.5f : (i < 8 ? 0.55f : (i < 10 ? 0.4f : 0.45f));
                h.scale(scale, scale, scale).glow(60, 60, 160).interpolation(3, 0);
                sentinelBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_CATALYST_BLOOM, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly rotate to track
            rotAngle += 0.015;

            for (int i = 0; i < sentinelBlocks.size(); i++) {
                double ox = bodyOffsets[i][0];
                double oy = bodyOffsets[i][1];
                double oz = bodyOffsets[i][2];
                double rx = ox * Math.cos(rotAngle) - oz * Math.sin(rotAngle);
                double rz = ox * Math.sin(rotAngle) + oz * Math.cos(rotAngle);
                sentinelBlocks.get(i).entity().teleport(c.clone().add(rx, oy, rz));
            }

            // Dark particle beam from head forward
            if (ticksAlive % 2 == 0) {
                double beamDist = 2.0 + (ticksAlive % 20) * 0.3;
                double beamX = Math.cos(rotAngle) * beamDist;
                double beamZ = Math.sin(rotAngle) * beamDist;
                DisplayBuilder.dustParticles(c.clone().add(beamX, 4.0, beamZ), 4, 0.3, 80, 0, 120, 1.2f);
            }

            // Aura around sentinel
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 1.5, 60, 60, 160, 1.0f);
            }

            if (ticksAlive % 55 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_CAVE, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShadowSentinel(plugin); }
    }

    // ================================================================
    // 13. TOTALITY — 18 blocks layered eclipse (black center, dark ring, bright corona), descends
    // ================================================================
    public static class Totality extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> centerBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> darkRingBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> coronaBlocks = new ArrayList<>();

        public Totality(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("totality", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Inner core: 4 BLACK_CONCRETE
            for (int i = 0; i < 4; i++) {
                double angle = (Math.PI * 2 * i) / 4.0;
                Location loc = center.clone().add(Math.cos(angle) * 0.4, 10, Math.sin(angle) * 0.4);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACK_CONCRETE);
                h.scale(0.7f, 0.3f, 0.7f).glow(10, 10, 15).interpolation(3, 0);
                centerBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Dark ring: 6 DEEPSLATE
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2 * i) / 6.0;
                Location loc = center.clone().add(Math.cos(angle) * 1.8, 10, Math.sin(angle) * 1.8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.6f, 0.25f, 0.6f).glow(40, 40, 60).interpolation(3, 0);
                darkRingBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Bright corona: 8 SEA_LANTERN
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 10, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.SEA_LANTERN);
                h.scale(0.5f, 0.2f, 0.5f).glow(255, 220, 150).interpolation(3, 0);
                coronaBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.5f, 0.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Descend from Y+10 to Y+2 over 100 ticks
            double descendProgress = Math.min(1.0, ticksAlive / 100.0);
            double currentY = 10.0 - descendProgress * 8.0;

            // Slow rotation for each layer at different speeds
            double innerRot = Math.toRadians(ticksAlive * 0.5);
            double middleRot = Math.toRadians(ticksAlive * 1.0);
            double outerRot = Math.toRadians(ticksAlive * 1.5);

            // Inner core
            for (int i = 0; i < centerBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 4.0 + innerRot;
                centerBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * 0.4, currentY, Math.sin(angle) * 0.4));
            }

            // Dark ring
            for (int i = 0; i < darkRingBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 6.0 + middleRot;
                darkRingBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * 1.8, currentY, Math.sin(angle) * 1.8));
            }

            // Corona — pulsing radius
            double coronaRadius = 3.5 + Math.sin(ticksAlive * 0.08) * 0.5;
            for (int i = 0; i < coronaBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 8.0 + outerRot;
                coronaBlocks.get(i).entity().teleport(
                    c.clone().add(Math.cos(angle) * coronaRadius, currentY, Math.sin(angle) * coronaRadius));
            }

            // Shadow zone beneath
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 12, 4.0, 60, 60, 160, 1.5f);
            }

            // Corona glow particles
            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY, 0), 8, coronaRadius, 255, 220, 150, 1.0f);
            }

            // Dark center particles
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY, 0), 6, 0.8, 10, 10, 15, 1.8f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_CATALYST_BLOOM, 0.8f, 0.3f);
            }

            // Ominous low sound as it descends
            if (ticksAlive % 30 == 0 && descendProgress < 1.0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new Totality(plugin); }
    }
}
