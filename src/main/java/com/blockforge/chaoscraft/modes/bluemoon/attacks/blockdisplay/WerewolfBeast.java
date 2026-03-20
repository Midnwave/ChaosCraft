package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import java.util.*;

/**
 * Blue Moon Mode — WEREWOLF BEAST
 * 13 primal beast-themed block display attacks.
 * Materials: IRON_BLOCK, CALCITE, DEEPSLATE, GRAY_CONCRETE, WHITE_CONCRETE
 * Sounds: ENTITY_WOLF_HOWL, ENTITY_WOLF_GROWL, ENTITY_WOLF_AMBIENT, BLOCK_BONE_BLOCK_BREAK
 */
public final class WerewolfBeast {

    private WerewolfBeast() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new MoonFang(plugin));
        registry.register(new ClawSwipe(plugin));
        registry.register(new WolfJaw(plugin));
        registry.register(new HowlingPillar(plugin));
        registry.register(new PackCircle(plugin));
        registry.register(new FeralLunge(plugin));
        registry.register(new BoneCage(plugin));
        registry.register(new MoonHowlRing(plugin));
        registry.register(new PredatorEyes(plugin));
        registry.register(new AlphaStrike(plugin));
        registry.register(new SpineRidge(plugin));
        registry.register(new TerritorialMarker(plugin));
        registry.register(new LunarPounce(plugin));
    }

    // ================================================================
    // 1. MOON FANG — 14 IRON_BLOCK+QUARTZ curved fang, slashes downward from Y+8
    // ================================================================
    public static class MoonFang extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> fangBlocks = new ArrayList<>();
        private double startY;

        public MoonFang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_fang", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            startY = center.getY() + 8;

            // 14 blocks in curved fang shape — arc from tip to base
            for (int i = 0; i < 14; i++) {
                double t = i / 13.0;
                double curveAngle = Math.toRadians(180 * t);
                double x = Math.sin(curveAngle) * 2.0;
                double y = -t * 5.0; // fang curves downward
                double z = Math.cos(curveAngle) * 0.8;
                Location loc = center.clone().add(x, startY - center.getY() + y, z);
                Material mat = i % 2 == 0 ? Material.IRON_BLOCK : Material.QUARTZ_BLOCK;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.5f + 0.3f * (1.0f - (float) t); // thicker at base
                h.scale(scale, scale * 1.5f, scale).glow(200, 200, 210).interpolation(3, 0);
                fangBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slash downward over first 40 ticks
            double dropProgress = Math.min(1.0, ticksAlive / 40.0);
            double yOffset = (startY - c.getY()) * (1.0 - dropProgress);

            for (int i = 0; i < fangBlocks.size(); i++) {
                double t = i / 13.0;
                double curveAngle = Math.toRadians(180 * t);
                double x = Math.sin(curveAngle) * 2.0;
                double y = -t * 5.0 + yOffset;
                double z = Math.cos(curveAngle) * 0.8;
                fangBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c, 6, 2.0, 200, 200, 210, 1.0f);
            }

            // Impact sound at landing
            if (ticksAlive == 40) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 1.2f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonFang(plugin); }
    }

    // ================================================================
    // 2. CLAW SWIPE — 15 IRON_BLOCK, 3 parallel curved claws, horizontal sweep
    // ================================================================
    public static class ClawSwipe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> clawBlocks = new ArrayList<>();
        private final double[][] clawOffsets = new double[15][3];

        public ClawSwipe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("claw_swipe", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            // 3 claws, 5 blocks each, curved arcs at different Y offsets
            int idx = 0;
            for (int claw = 0; claw < 3; claw++) {
                double yBase = 1.0 + claw * 1.2;
                for (int seg = 0; seg < 5; seg++) {
                    double t = seg / 4.0;
                    double x = t * 4.0 - 2.0;
                    double y = yBase + Math.sin(t * Math.PI) * 0.8;
                    double z = Math.sin(t * Math.PI) * 1.5;
                    clawOffsets[idx][0] = x;
                    clawOffsets[idx][1] = y;
                    clawOffsets[idx][2] = z;
                    Location loc = center.clone().add(x, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    float scale = 0.4f + 0.15f * (float) Math.sin(t * Math.PI);
                    h.scale(scale, scale, scale * 1.8f).glow(190, 190, 200).interpolation(3, 0);
                    clawBlocks.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Horizontal sweep rotation
            double sweepAngle = Math.toRadians(ticksAlive * 4.0);

            for (int i = 0; i < clawBlocks.size(); i++) {
                double ox = clawOffsets[i][0];
                double oy = clawOffsets[i][1];
                double oz = clawOffsets[i][2];
                double rx = ox * Math.cos(sweepAngle) - oz * Math.sin(sweepAngle);
                double rz = ox * Math.sin(sweepAngle) + oz * Math.cos(sweepAngle);
                clawBlocks.get(i).entity().teleport(c.clone().add(rx, oy, rz));
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c, 8, 3.5, 190, 190, 200, 1.2f);
            }

            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_AMBIENT, 0.4f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ClawSwipe(plugin); }
    }

    // ================================================================
    // 3. WOLF JAW — 18 CALCITE+IRON_BLOCK upper+lower jaw, slam shut
    // ================================================================
    public static class WolfJaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> upperJaw = new ArrayList<>();
        private final List<BlockDisplayHandle> lowerJaw = new ArrayList<>();

        public WolfJaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wolf_jaw", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.5);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Upper jaw: 9 blocks in arc above center
            for (int i = 0; i < 9; i++) {
                double angle = Math.toRadians(-80 + (160.0 * i / 8));
                double radius = 2.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 3.5, z);
                Material mat = i % 3 == 0 ? Material.IRON_BLOCK : Material.CALCITE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.45f + 0.15f * (float) Math.abs(Math.sin(Math.PI * i / 8.0));
                h.scale(scale, scale * 1.5f, scale).glow(210, 210, 220).interpolation(3, 0);
                upperJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            // Lower jaw: 9 blocks in arc below center
            for (int i = 0; i < 9; i++) {
                double angle = Math.toRadians(-80 + (160.0 * i / 8));
                double radius = 2.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, -1.5, z);
                Material mat = i % 3 == 0 ? Material.IRON_BLOCK : Material.CALCITE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = 0.45f + 0.15f * (float) Math.abs(Math.sin(Math.PI * i / 8.0));
                h.scale(scale, scale * 1.5f, scale).glow(210, 210, 220).interpolation(3, 0);
                lowerJaw.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Start open (Y offset +-3.5), slam shut over 30 ticks
            double closeProgress = Math.min(1.0, ticksAlive / 30.0);
            double upperY = 3.5 * (1.0 - closeProgress);
            double lowerY = -1.5 * (1.0 - closeProgress);

            for (int i = 0; i < 9; i++) {
                double angle = Math.toRadians(-80 + (160.0 * i / 8));
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                upperJaw.get(i).entity().teleport(c.clone().add(x, upperY, z));
                lowerJaw.get(i).entity().teleport(c.clone().add(x, lowerY, z));
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, 2.0, 210, 210, 220, 1.0f);
            }

            if (ticksAlive == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 1.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WolfJaw(plugin); }
    }

    // ================================================================
    // 4. HOWLING PILLAR — 12 DEEPSLATE tall twisted column, shockwave ring
    // ================================================================
    public static class HowlingPillar extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();

        public HowlingPillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("howling_pillar", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 DEEPSLATE blocks spiraling upward
            for (int i = 0; i < 12; i++) {
                double t = i / 11.0;
                double angle = t * Math.PI * 3; // 1.5 full twists
                double radius = 0.5 + t * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = t * 8.0; // 8 blocks tall
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                h.scale(0.6f, 0.7f, 0.6f).glow(100, 100, 120).interpolation(3, 0);
                h.rotate((float) Math.toDegrees(angle), 0, 1, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.5f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow twist animation
            double twistOffset = Math.toRadians(ticksAlive * 1.5);

            for (int i = 0; i < pillarBlocks.size(); i++) {
                double t = i / 11.0;
                double angle = t * Math.PI * 3 + twistOffset;
                double radius = 0.5 + t * 0.3;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                double y = t * 8.0;
                pillarBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            // Expanding shockwave ring every 40 ticks
            if (ticksAlive % 40 < 20) {
                double ringRadius = (ticksAlive % 40) * 0.4;
                DisplayBuilder.particleRing(c, ringRadius, Particle.SOUL_FIRE_FLAME, 16, null);
            }

            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 4, 0), 8, 1.5, 100, 100, 120, 1.2f);
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 0.8f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new HowlingPillar(plugin); }
    }

    // ================================================================
    // 5. PACK CIRCLE — 16 IRON_BLOCK wolf silhouettes facing inward, contract
    // ================================================================
    public static class PackCircle extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> packBlocks = new ArrayList<>();
        private final double[] blockAngles = new double[16];

        public PackCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("pack_circle", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 blocks in circle, pairs forming wolf head silhouettes
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 * i) / 16.0;
                blockAngles[i] = angle;
                double radius = 5.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                // Alternate heights for head/body shapes
                double y = (i % 2 == 0) ? 1.2 : 0.6;
                Location loc = center.clone().add(x, y, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                float scale = (i % 2 == 0) ? 0.5f : 0.6f;
                h.scale(scale, scale * 1.2f, scale).glow(180, 180, 195).interpolation(3, 0);
                // Face inward
                h.rotate((float) Math.toDegrees(angle + Math.PI), 0, 1, 0);
                packBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly contract circle from radius 5 to 1.5
            double contractProgress = Math.min(1.0, ticksAlive / 200.0);
            double currentRadius = 5.0 - contractProgress * 3.5;

            for (int i = 0; i < packBlocks.size(); i++) {
                double angle = blockAngles[i];
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                double y = (i % 2 == 0) ? 1.2 : 0.6;
                packBlocks.get(i).entity().teleport(c.clone().add(x, y, z));
            }

            if (ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 6, currentRadius, 180, 180, 195, 1.0f);
            }

            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_AMBIENT, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PackCircle(plugin); }
    }

    // ================================================================
    // 6. FERAL LUNGE — 10 IRON_BLOCK+GRAY_CONCRETE leaping beast, high speed launch
    // ================================================================
    public static class FeralLunge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beastBlocks = new ArrayList<>();
        private final double[][] bodyOffsets = new double[10][3];
        private double launchX;
        private double launchZ;

        public FeralLunge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("feral_lunge", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double dirAngle = Math.random() * Math.PI * 2;
            launchX = Math.cos(dirAngle) * 0.6;
            launchZ = Math.sin(dirAngle) * 0.6;

            // Beast body shape: 4 torso, 2 head, 4 legs
            double[][] shape = {
                {0, 0.5, 0}, {0.5, 0.5, 0}, {-0.5, 0.5, 0}, {0, 0.5, 0.5},    // torso
                {0.8, 0.9, 0}, {1.1, 1.1, 0},                                     // head
                {-0.4, 0, 0.3}, {-0.4, 0, -0.3}, {0.4, 0, 0.3}, {0.4, 0, -0.3}  // legs
            };

            for (int i = 0; i < 10; i++) {
                bodyOffsets[i][0] = shape[i][0];
                bodyOffsets[i][1] = shape[i][1];
                bodyOffsets[i][2] = shape[i][2];
                Location loc = center.clone().add(shape[i][0], shape[i][1], shape[i][2]);
                Material mat = i < 4 ? Material.IRON_BLOCK : (i < 6 ? Material.IRON_BLOCK : Material.GRAY_CONCRETE);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = i < 6 ? 0.5f : 0.35f;
                h.scale(scale, scale, scale).glow(170, 170, 180).interpolation(3, 0);
                beastBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.2f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Parabolic lunge: forward + upward arc
            double moveX = launchX * ticksAlive;
            double moveZ = launchZ * ticksAlive;
            double moveY = Math.max(0, 3.0 * Math.sin(Math.PI * Math.min(1.0, ticksAlive / 60.0)));

            for (int i = 0; i < beastBlocks.size(); i++) {
                double ox = bodyOffsets[i][0];
                double oy = bodyOffsets[i][1];
                double oz = bodyOffsets[i][2];
                beastBlocks.get(i).entity().teleport(c.clone().add(moveX + ox, moveY + oy, moveZ + oz));
            }

            if (ticksAlive % 2 == 0) {
                Location trail = c.clone().add(moveX, moveY, moveZ);
                DisplayBuilder.dustParticles(trail, 4, 0.5, 170, 170, 180, 1.0f);
            }

            if (ticksAlive == 60) {
                DisplayBuilder.playSound(c.clone().add(moveX, 0, moveZ), Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FeralLunge(plugin); }
    }

    // ================================================================
    // 7. BONE CAGE — 14 CALCITE ribcage enclosure, bars close inward
    // ================================================================
    public static class BoneCage extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ribBlocks = new ArrayList<>();
        private final double[] ribAngles = new double[14];

        public BoneCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_cage", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            // 14 CALCITE vertical bars in a circle, like ribs
            for (int i = 0; i < 14; i++) {
                double angle = (Math.PI * 2 * i) / 14.0;
                ribAngles[i] = angle;
                double radius = 3.5;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                h.scale(0.3f, 2.5f, 0.3f).glow(220, 220, 230).interpolation(3, 0);
                // Tilt inward slightly
                h.rotate(10f, (float) Math.sin(angle), 0, (float) -Math.cos(angle));
                ribBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Bars contract from radius 3.5 to 1.0
            double contractProgress = Math.min(1.0, ticksAlive / 120.0);
            double currentRadius = 3.5 - contractProgress * 2.5;

            for (int i = 0; i < ribBlocks.size(); i++) {
                double angle = ribAngles[i];
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                ribBlocks.get(i).entity().teleport(c.clone().add(x, 0, z));
            }

            if (ticksAlive % 8 == 0) {
                DisplayBuilder.dustParticles(c, 4, currentRadius, 220, 220, 230, 0.8f);
            }

            if (ticksAlive % 25 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new BoneCage(plugin); }
    }

    // ================================================================
    // 8. MOON HOWL RING — 12 WHITE_CONCRETE ring, note particle pulses outward
    // ================================================================
    public static class MoonHowlRing extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringBlocks = new ArrayList<>();

        public MoonHowlRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("moon_howl_ring", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
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

            // 12 WHITE_CONCRETE in a ring at Y+2
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12.0;
                double radius = 3.0;
                Location loc = center.clone().add(Math.cos(angle) * radius, 2.0, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.WHITE_CONCRETE);
                h.scale(0.5f, 0.5f, 0.5f).glow(240, 240, 250).interpolation(3, 0);
                ringBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 1.2f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Gentle vertical bob
            double bob = Math.sin(ticksAlive * 0.08) * 0.3;

            for (int i = 0; i < ringBlocks.size(); i++) {
                double angle = (Math.PI * 2 * i) / 12.0;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                ringBlocks.get(i).entity().teleport(c.clone().add(x, 2.0 + bob, z));
            }

            // Note particle pulse outward every 30 ticks
            if (ticksAlive % 30 < 15) {
                double pulseRadius = (ticksAlive % 30) * 0.5;
                DisplayBuilder.particleRing(c.clone().add(0, 2, 0), pulseRadius, Particle.NOTE, 12, null);
            }

            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 2, 0), 6, 3.0, 240, 240, 250, 1.0f);
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MoonHowlRing(plugin); }
    }

    // ================================================================
    // 9. PREDATOR EYES — 10 SEA_LANTERN pairs as glowing eyes, approach slowly
    // ================================================================
    public static class PredatorEyes extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> eyeBlocks = new ArrayList<>();
        private final double[] eyeAngles = new double[10];

        public PredatorEyes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("predator_eyes", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 5 pairs of glowing eyes (10 blocks) around the perimeter
            for (int pair = 0; pair < 5; pair++) {
                double angle = (Math.PI * 2 * pair) / 5.0;
                double radius = 6.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                // Left eye
                Location locL = center.clone().add(x - Math.sin(angle) * 0.3, 1.0, z + Math.cos(angle) * 0.3);
                BlockDisplayHandle hL = displayBuilder.spawnBlock(locL, Material.SEA_LANTERN);
                hL.scale(0.4f, 0.4f, 0.4f).glow(200, 220, 100).interpolation(3, 0);
                eyeBlocks.add(hL);
                spawnedEntities.add(hL.entity());
                eyeAngles[pair * 2] = angle;

                // Right eye
                Location locR = center.clone().add(x + Math.sin(angle) * 0.3, 1.0, z - Math.cos(angle) * 0.3);
                BlockDisplayHandle hR = displayBuilder.spawnBlock(locR, Material.SEA_LANTERN);
                hR.scale(0.4f, 0.4f, 0.4f).glow(200, 220, 100).interpolation(3, 0);
                eyeBlocks.add(hR);
                spawnedEntities.add(hR.entity());
                eyeAngles[pair * 2 + 1] = angle;
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_AMBIENT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slowly approach center from radius 6 to 1.5
            double approachProgress = Math.min(1.0, ticksAlive / 250.0);
            double currentRadius = 6.0 - approachProgress * 4.5;

            for (int pair = 0; pair < 5; pair++) {
                double angle = (Math.PI * 2 * pair) / 5.0;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;

                // Slight sway
                double sway = Math.sin(ticksAlive * 0.05 + pair) * 0.2;

                eyeBlocks.get(pair * 2).entity().teleport(
                    c.clone().add(x - Math.sin(angle) * 0.3, 1.0 + sway, z + Math.cos(angle) * 0.3));
                eyeBlocks.get(pair * 2 + 1).entity().teleport(
                    c.clone().add(x + Math.sin(angle) * 0.3, 1.0 + sway, z - Math.cos(angle) * 0.3));
            }

            // Blink effect: briefly scale to 0 and back
            if (ticksAlive % 80 > 75) {
                for (BlockDisplayHandle h : eyeBlocks) {
                    h.scale(0.1f, 0.1f, 0.1f).interpolation(2, 0);
                }
            } else if (ticksAlive % 80 == 0) {
                for (BlockDisplayHandle h : eyeBlocks) {
                    h.scale(0.4f, 0.4f, 0.4f).interpolation(2, 0);
                }
            }

            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_AMBIENT, 0.3f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new PredatorEyes(plugin); }
    }

    // ================================================================
    // 10. ALPHA STRIKE — 16 IRON_BLOCK paw print (pad+4 toes), stomps from above
    // ================================================================
    public static class AlphaStrike extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pawBlocks = new ArrayList<>();
        private double startY;

        public AlphaStrike(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("alpha_strike", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            startY = center.getY() + 12;

            // Central pad: 8 blocks in filled circle
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double radius = 0.8;
                Location loc = center.clone().add(Math.cos(angle) * radius, 12, Math.sin(angle) * radius);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                h.scale(0.7f, 0.4f, 0.7f).glow(180, 180, 195).interpolation(3, 0);
                pawBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 toe clusters (2 blocks each = 8 blocks)
            double[][] toePositions = {{0, 2.0}, {1.5, 1.5}, {-1.5, 1.5}, {0, -2.0}};
            for (double[] toe : toePositions) {
                for (int j = 0; j < 2; j++) {
                    double ox = toe[0] + (j == 0 ? 0.2 : -0.2);
                    double oz = toe[1] + (j == 0 ? 0.2 : -0.2);
                    Location loc = center.clone().add(ox, 12, oz);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.IRON_BLOCK);
                    h.scale(0.55f, 0.4f, 0.55f).glow(180, 180, 195).interpolation(3, 0);
                    pawBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Stomp down over 30 ticks
            double dropProgress = Math.min(1.0, ticksAlive / 30.0);
            // Ease-in for heavy stomp
            double easedProgress = dropProgress * dropProgress;
            double currentY = 12.0 * (1.0 - easedProgress);

            // Move all paw blocks down
            // First 8 = pad circle
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2 * i) / 8.0;
                double x = Math.cos(angle) * 0.8;
                double z = Math.sin(angle) * 0.8;
                pawBlocks.get(i).entity().teleport(c.clone().add(x, currentY, z));
            }

            // 4 toe clusters
            double[][] toePositions = {{0, 2.0}, {1.5, 1.5}, {-1.5, 1.5}, {0, -2.0}};
            for (int t = 0; t < 4; t++) {
                for (int j = 0; j < 2; j++) {
                    int idx = 8 + t * 2 + j;
                    double ox = toePositions[t][0] + (j == 0 ? 0.2 : -0.2);
                    double oz = toePositions[t][1] + (j == 0 ? 0.2 : -0.2);
                    pawBlocks.get(idx).entity().teleport(c.clone().add(ox, currentY, oz));
                }
            }

            if (ticksAlive % 3 == 0 && ticksAlive < 30) {
                DisplayBuilder.dustParticles(c.clone().add(0, currentY, 0), 8, 2.0, 180, 180, 195, 1.5f);
            }

            // Shockwave on impact
            if (ticksAlive == 30) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 1.5f, 0.3f);
                DisplayBuilder.particleRing(c, 3.0, Particle.EXPLOSION, 8, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AlphaStrike(plugin); }
    }

    // ================================================================
    // 11. SPINE RIDGE — 14 DEEPSLATE spine rising from ground in sequence
    // ================================================================
    public static class SpineRidge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spineBlocks = new ArrayList<>();
        private final double[][] spinePositions = new double[14][2];

        public SpineRidge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spine_ridge", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 DEEPSLATE vertebrae in a line with slight S-curve
            double dirAngle = Math.random() * Math.PI * 2;
            double dx = Math.cos(dirAngle);
            double dz = Math.sin(dirAngle);

            for (int i = 0; i < 14; i++) {
                double dist = (i - 6.5) * 0.8;
                double lateralOffset = Math.sin(i * 0.5) * 0.5; // S-curve
                double x = dx * dist - dz * lateralOffset;
                double z = dz * dist + dx * lateralOffset;
                spinePositions[i][0] = x;
                spinePositions[i][1] = z;

                // Start underground
                Location loc = center.clone().add(x, -1.0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DEEPSLATE);
                float heightScale = 0.8f + 0.4f * (float) Math.sin(Math.PI * i / 13.0);
                h.scale(0.4f, heightScale, 0.4f).glow(90, 90, 100).interpolation(3, 0);
                // Slight tilt for organic look
                h.rotate(15f * (float) Math.sin(i * 0.7), 0, 0, 1);
                spineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 0.8f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rise in sequence: each vertebra rises 5 ticks after the previous
            for (int i = 0; i < spineBlocks.size(); i++) {
                int riseStart = i * 5;
                double riseProgress = Math.min(1.0, Math.max(0, (ticksAlive - riseStart) / 20.0));
                double y = -1.0 + riseProgress * 2.5; // Rise to 1.5 above ground
                spineBlocks.get(i).entity().teleport(
                    c.clone().add(spinePositions[i][0], y, spinePositions[i][1]));
            }

            if (ticksAlive % 5 == 0) {
                int activeIdx = Math.min(13, ticksAlive / 5);
                Location particleLoc = c.clone().add(spinePositions[activeIdx][0], 1.0, spinePositions[activeIdx][1]);
                DisplayBuilder.dustParticles(particleLoc, 4, 0.5, 90, 90, 100, 1.0f);
            }

            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BONE_BLOCK_BREAK, 0.6f, 1.0f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SpineRidge(plugin); }
    }

    // ================================================================
    // 12. TERRITORIAL MARKER — 12 CALCITE pillars perimeter, escalating damage
    // ================================================================
    public static class TerritorialMarker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();

        public TerritorialMarker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("territorial_marker", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(4.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(200);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 12 CALCITE pillars in a circle at varying heights
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12.0;
                double radius = 4.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.CALCITE);
                float height = 1.5f + 0.5f * (float) Math.sin(i * 0.8);
                h.scale(0.4f, height, 0.4f).glow(220, 220, 230).interpolation(3, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_HOWL, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pillars grow taller over time (escalating threat)
            float growthFactor = 1.0f + (ticksAlive / 350.0f) * 1.5f;

            for (int i = 0; i < pillarBlocks.size(); i++) {
                float baseHeight = 1.5f + 0.5f * (float) Math.sin(i * 0.8);
                pillarBlocks.get(i).scale(0.4f, baseHeight * growthFactor, 0.4f);
                pillarBlocks.get(i).interpolation(3, 0);
            }

            // Pulsing glow at base of pillars
            if (ticksAlive % 6 == 0) {
                for (int i = 0; i < 12; i++) {
                    double angle = (Math.PI * 2 * i) / 12.0;
                    Location pillarBase = c.clone().add(Math.cos(angle) * 4.0, 0, Math.sin(angle) * 4.0);
                    DisplayBuilder.dustParticles(pillarBase, 2, 0.3, 220, 220, 230, 0.8f);
                }
            }

            // Interior warning particles intensify
            if (ticksAlive % 10 == 0) {
                int particleCount = 4 + (ticksAlive / 50);
                DisplayBuilder.dustParticles(c, particleCount, 3.5, 220, 180, 180, 1.2f);
            }

            if (ticksAlive % 70 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_GROWL, 0.5f, 0.4f + (ticksAlive / 700.0f));
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TerritorialMarker(plugin); }
    }

    // ================================================================
    // 13. LUNAR POUNCE — 15 blocks crouched beast, charges 30 ticks then leaps
    // ================================================================
    public static class LunarPounce extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> beastBlocks = new ArrayList<>();
        private final double[][] bodyOffsets = new double[15][3];
        private double leapX;
        private double leapZ;

        public LunarPounce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lunar_pounce", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(200);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double dirAngle = Math.random() * Math.PI * 2;
            leapX = Math.cos(dirAngle);
            leapZ = Math.sin(dirAngle);

            // Crouched beast: low to the ground, compact shape
            // Body core (5 blocks)
            double[][] shape = {
                {0, 0.3, 0}, {0.4, 0.3, 0.2}, {-0.4, 0.3, 0.2}, {0.4, 0.3, -0.2}, {-0.4, 0.3, -0.2},
                // Head (3 blocks)
                {0.8, 0.5, 0}, {1.0, 0.6, 0.15}, {1.0, 0.6, -0.15},
                // Haunches (4 blocks, crouched low)
                {-0.7, 0.2, 0.3}, {-0.7, 0.2, -0.3}, {-0.9, 0.4, 0.3}, {-0.9, 0.4, -0.3},
                // Tail (3 blocks)
                {-1.2, 0.4, 0}, {-1.5, 0.5, 0.1}, {-1.8, 0.6, 0.2}
            };

            for (int i = 0; i < 15; i++) {
                // Rotate shape to face leap direction
                double ox = shape[i][0] * leapX - shape[i][2] * leapZ;
                double oz = shape[i][0] * leapZ + shape[i][2] * leapX;
                bodyOffsets[i][0] = ox;
                bodyOffsets[i][1] = shape[i][1];
                bodyOffsets[i][2] = oz;
                Location loc = center.clone().add(ox, shape[i][1], oz);
                Material mat;
                if (i < 5) mat = Material.IRON_BLOCK;
                else if (i < 8) mat = Material.IRON_BLOCK;
                else if (i < 12) mat = Material.GRAY_CONCRETE;
                else mat = Material.GRAY_CONCRETE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scale = i < 5 ? 0.45f : (i < 8 ? 0.35f : 0.3f);
                h.scale(scale, scale, scale).glow(170, 170, 180).interpolation(3, 0);
                beastBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_WOLF_GROWL, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            double moveX = 0;
            double moveY = 0;
            double moveZ = 0;

            if (ticksAlive <= 30) {
                // Charging phase: crouch lower, tremble
                double tremble = Math.sin(ticksAlive * 2.0) * 0.05;
                moveY = -0.1 * (ticksAlive / 30.0); // crouch down
                moveX = tremble;
            } else {
                // Leap phase: parabolic arc forward
                int leapTick = ticksAlive - 30;
                double leapProgress = Math.min(1.0, leapTick / 40.0);
                double dist = leapProgress * 8.0;
                moveX = leapX * dist;
                moveZ = leapZ * dist;
                moveY = 4.0 * Math.sin(leapProgress * Math.PI); // parabolic arc
            }

            for (int i = 0; i < beastBlocks.size(); i++) {
                beastBlocks.get(i).entity().teleport(
                    c.clone().add(bodyOffsets[i][0] + moveX, bodyOffsets[i][1] + moveY, bodyOffsets[i][2] + moveZ));
            }

            if (ticksAlive <= 30 && ticksAlive % 5 == 0) {
                DisplayBuilder.dustParticles(c, 4, 0.5, 170, 170, 180, 0.8f);
            }

            if (ticksAlive > 30 && ticksAlive % 2 == 0) {
                Location trail = c.clone().add(moveX, moveY, moveZ);
                DisplayBuilder.dustParticles(trail, 6, 0.8, 170, 170, 180, 1.2f);
            }

            if (ticksAlive == 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WOLF_HOWL, 1.2f, 1.2f);
            }

            if (ticksAlive == 70) {
                Location landing = c.clone().add(leapX * 8, 0, leapZ * 8);
                DisplayBuilder.playSound(landing, Sound.BLOCK_BONE_BLOCK_BREAK, 1.2f, 0.5f);
                DisplayBuilder.particleRing(landing, 3.0, Particle.EXPLOSION, 6, null);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new LunarPounce(plugin); }
    }
}
