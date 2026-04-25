package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * DevilsDream — ENVIRONMENTAL DISPLAYS 51-60.
 *
 * Set-dressing hazards / atmosphere displays for the DevilsDream arena.
 * Each attack uses interpolated BlockDisplay/ItemDisplay structures with
 * particle and sound ambience. All structures spawn straight (zero yaw/pitch).
 * Multi-phase lifecycle: spawn -> active -> dissipate, driven by tick / duration.
 *
 *  51. Brimstone Geyser            (12 displays, active hazard)
 *  52. Dead Star Crater            (28 displays, atmosphere)
 *  53. Hanging Garden of Rot       (30 displays, active hazard)
 *  54. Penitent's Path             (30 displays, atmosphere)
 *  55. Forsaken Bell Tower         (26 displays, active hazard)
 *  56. The Drowned Chandelier      (34 displays, active hazard)
 *  57. Corrupted Altar             (28 displays, atmosphere/escalation)
 *  58. Blood Moon Rise             (30 displays, atmosphere)
 *  59. Skeleton Chariot Wreckage   (26 displays, atmosphere)
 *  60. Hellfire Brazier            (14 displays, active hazard)
 */
public final class DDEnvironmental1 {
    private DDEnvironmental1() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BrimstoneGeyser(plugin));
        registry.register(new DeadStarCrater(plugin));
        registry.register(new HangingGardenOfRot(plugin));
        registry.register(new PenitentsPath(plugin));
        registry.register(new ForsakenBellTower(plugin));
        registry.register(new DrownedChandelier(plugin));
        registry.register(new CorruptedAltar(plugin));
        registry.register(new BloodMoonRise(plugin));
        registry.register(new ChariotWreckage(plugin));
        registry.register(new HellfireBrazier(plugin));
    }

    // =================================================================
    // 51. BRIMSTONE GEYSER
    //     Volcanic vent — basalt tube, magma cap, blackstone crust ring.
    //     Pressure builds -> erupts column upward -> collapses back.
    //     12 displays. Active hazard around vent mouth + eruption column.
    // =================================================================
    public static class BrimstoneGeyser extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> tube = new ArrayList<>();
        private final List<BlockDisplayHandle> crust = new ArrayList<>();
        private final List<BlockDisplayHandle> column = new ArrayList<>();
        private BlockDisplayHandle cap;

        public BrimstoneGeyser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_geyser", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(5.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(20);
            config.setDamageDelayTicks(40);
            config.setDurationTicks(180);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.9f, 0.4f);

            // Basalt 2x2 tube, 3 tall = the vent body (4 displays of 3 height = 4)
            for (int i = 0; i < 4; i++) {
                double dx = (i % 2 == 0) ? -0.5 : 0.5;
                double dz = (i / 2 == 0) ? -0.5 : 0.5;
                Location p = center.clone().add(dx, 0, dz);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BASALT);
                h.scale(1.0f, 3.0f, 1.0f).interpolation(8, 0).brightness(15, 15);
                tube.add(h);
            }

            // Magma cap — pressurized top
            cap = displayBuilder.spawnBlock(center.clone().add(0, 3.0, 0), Material.MAGMA_BLOCK);
            cap.scale(1.6f, 0.6f, 1.6f).glow(255, 120, 30).interpolation(6, 0);

            // Blackstone crust ring around vent mouth (7 displays)
            for (int i = 0; i < 7; i++) {
                double angle = Math.PI * 2 * i / 7;
                Location p = center.clone().add(Math.cos(angle) * 1.6, 0.0, Math.sin(angle) * 1.6);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.6f, 0.4f, 0.6f).interpolation(8, 0);
                crust.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();
            int phase;
            if (tick < 40) phase = 0;             // build pressure
            else if (tick < 110) phase = 1;       // erupt
            else if (tick < dur - 20) phase = 2;  // sustain
            else phase = 3;                       // collapse

            if (phase == 0) {
                // Cap vibrates, tube subtly shakes
                if (tick % 3 == 0 && cap != null) {
                    float jx = (float) ((Math.random() - 0.5) * 0.15);
                    float jz = (float) ((Math.random() - 0.5) * 0.15);
                    cap.animateTo(new Vector3f(-0.8f + jx, 3.0f, -0.8f + jz),
                            new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                            new Vector3f(1.6f, 0.6f, 1.6f), 3);
                }
                if (tick % 8 == 0) {
                    w.spawnParticle(Particle.SMOKE, getCenter().clone().add(0, 3.2, 0),
                            6, 0.6, 0.2, 0.6, 0.02);
                    DisplayBuilder.dustParticles(getCenter().clone().add(0, 3.2, 0),
                            3, 0.5, 255, 100, 30, 1.2f);
                }
                if (tick % 12 == 0) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 0.5f);
                }
            } else if (phase == 1) {
                // Eruption — cap launches up, column of magma blocks rapidly cycles upward
                if (tick == 40) {
                    DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GENERIC_EXPLODE, 1.4f, 0.5f);
                    if (cap != null) {
                        cap.animateTo(new Vector3f(-0.8f, 14.0f, -0.8f),
                                new AxisAngle4f((float) (Math.PI * 2), 1, 0, 1),
                                new Vector3f(1.0f, 1.0f, 1.0f), 30);
                    }
                    // Spawn 7 column displays (magma cycling upward)
                    for (int i = 0; i < 7; i++) {
                        BlockDisplayHandle h = displayBuilder.spawnBlock(
                                getCenter().clone().add(0, 3, 0), Material.MAGMA_BLOCK);
                        h.scale(0.7f, 0.7f, 0.7f).glow(255, 80, 0).interpolation(20, i * 2);
                        h.animateTo(new Vector3f(-0.35f, 3.0f + i * 1.4f, -0.35f),
                                new AxisAngle4f((float) (Math.random() * Math.PI), 0, 1, 0),
                                new Vector3f(0.7f, 0.7f, 0.7f), 20);
                        column.add(h);
                    }
                }

                // Tube shake
                if (tick % 4 == 0) {
                    for (int i = 0; i < tube.size(); i++) {
                        float jx = (float) ((Math.random() - 0.5) * 0.2);
                        float jz = (float) ((Math.random() - 0.5) * 0.2);
                        double dx = (i % 2 == 0) ? -0.5 : 0.5;
                        double dz = (i / 2 == 0) ? -0.5 : 0.5;
                        tube.get(i).animateTo(
                                new Vector3f((float) dx + jx - 0.5f, 0, (float) dz + jz - 0.5f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.0f, 3.0f, 1.0f), 4);
                    }
                }

                // Heavy lava + fire column particles
                if (tick % 2 == 0) {
                    for (int y = 0; y < 12; y++) {
                        Location loc = getCenter().clone().add(
                                (Math.random() - 0.5) * 1.0, 3 + y * 0.9, (Math.random() - 0.5) * 1.0);
                        w.spawnParticle(Particle.LAVA, loc, 2, 0.2, 0.2, 0.2, 0.04);
                        w.spawnParticle(Particle.FLAME, loc, 2, 0.15, 0.15, 0.15, 0.03);
                    }
                }
            } else if (phase == 2) {
                // Sustain — heat shimmer, crust pulse
                if (tick % 6 == 0) {
                    for (int i = 0; i < crust.size(); i++) {
                        double angle = Math.PI * 2 * i / crust.size();
                        float s = 0.6f + (float) Math.sin(tick * 0.08 + i) * 0.1f;
                        crust.get(i).animateTo(
                                new Vector3f((float) (Math.cos(angle) * 1.6) - s / 2f, 0.0f,
                                        (float) (Math.sin(angle) * 1.6) - s / 2f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(s, 0.4f, s), 6);
                    }
                }
                if (tick % 3 == 0) {
                    DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.5, 0),
                            4, 2.0, 255, 120, 30, 1.4f);
                    w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(0, 8, 0),
                            3, 1.5, 0.3, 1.5, 0.02);
                }
            } else {
                // Collapse — column descends, cap returns
                if (tick == dur - 20) {
                    if (cap != null) {
                        cap.animateTo(new Vector3f(-0.8f, 3.0f, -0.8f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.6f, 0.6f, 1.6f), 18);
                    }
                    for (BlockDisplayHandle h : column) {
                        h.animateTo(new Vector3f(-0.35f, 3.0f, -0.35f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.01f, 0.01f, 0.01f), 18);
                    }
                    DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 0.4f);
                }
            }

            if (tick % 25 == 0 && phase >= 1) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            tube.clear(); crust.clear(); column.clear(); cap = null;
        }

        @Override public AbstractAttack newInstance() { return new BrimstoneGeyser(plugin); }
    }

    // =================================================================
    // 52. DEAD STAR CRATER
    //     Massive impact crater — blackstone rim ring (~8 wide), basalt
    //     scorched floor, tuff debris, obsidian glass center.
    //     28 displays. Pure atmosphere — pulsing center, occasional crumble.
    // =================================================================
    public static class DeadStarCrater extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> rim = new ArrayList<>();
        private final List<BlockDisplayHandle> floor = new ArrayList<>();
        private final List<BlockDisplayHandle> debris = new ArrayList<>();
        private BlockDisplayHandle obsidianCenter;
        private int crumbledIdx = 0;

        public DeadStarCrater(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dead_star_crater", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(360);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_BREAK, 1.2f, 0.3f);

            // Rim ring — 12 blackstone displays at radius 4
            for (int i = 0; i < 12; i++) {
                double angle = Math.PI * 2 * i / 12;
                Location p = center.clone().add(Math.cos(angle) * 4.0, 0.4, Math.sin(angle) * 4.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.9f, 0.7f, 0.9f).interpolation(10, 0);
                rim.add(h);
            }

            // Scorched basalt floor — 8 tiles inside the crater
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                double r = 1.5 + (i % 2) * 1.0;
                Location p = center.clone().add(Math.cos(angle) * r, -0.4, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BASALT);
                h.scale(1.2f, 0.2f, 1.2f).glow(60, 30, 20).interpolation(10, 0);
                floor.add(h);
            }

            // Tuff debris chunks (7) scattered around rim
            for (int i = 0; i < 7; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = 4.5 + Math.random() * 1.5;
                Location p = center.clone().add(Math.cos(angle) * r,
                        0.3 + Math.random() * 0.4, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.TUFF);
                h.scale(0.5f, 0.5f, 0.5f).interpolation(10, 0);
                debris.add(h);
            }

            // Obsidian glass impact center
            obsidianCenter = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 0), Material.OBSIDIAN);
            obsidianCenter.scale(1.4f, 0.15f, 1.4f).glow(90, 0, 130).interpolation(8, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Obsidian center pulse
            if (tick % 6 == 0 && obsidianCenter != null) {
                float s = 1.4f + (float) Math.sin(tick * 0.07) * 0.2f;
                obsidianCenter.animateTo(new Vector3f(-s / 2f, 0.05f, -s / 2f),
                        new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                        new Vector3f(s, 0.15f, s), 6);
            }

            // Debris shiver
            if (tick % 30 == 0 && !debris.isEmpty()) {
                int idx = (int) (Math.random() * debris.size());
                BlockDisplayHandle h = debris.get(idx);
                float jx = (float) ((Math.random() - 0.5) * 0.3);
                float jz = (float) ((Math.random() - 0.5) * 0.3);
                h.animateTo(new Vector3f(jx - 0.25f, 0.4f, jz - 0.25f),
                        new AxisAngle4f((float) (Math.random() * Math.PI), 0, 1, 0),
                        new Vector3f(0.5f, 0.5f, 0.5f), 8);
            }

            // Periodic rim crumble — one rim section scales to 0
            if (tick > 0 && tick % 90 == 0 && crumbledIdx < rim.size()) {
                BlockDisplayHandle h = rim.get(crumbledIdx++);
                Vector3f cur = new Vector3f(-0.45f, 0.4f, -0.45f);
                h.animateTo(cur, new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 16);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_STONE_BREAK, 0.9f, 0.4f);
                w.spawnParticle(Particle.BLOCK, getCenter(), 30, 4, 0.3, 4, 0.05,
                        Material.BLACKSTONE.createBlockData());
            }

            // Heat shimmer + ash drift
            if (tick % 4 == 0) {
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 0.6, 0),
                        4, 3.5, 90, 0, 130, 1.4f);
                w.spawnParticle(Particle.SMOKE, getCenter().clone().add(0, 1.5, 0),
                        2, 2.0, 0.8, 2.0, 0.01);
                w.spawnParticle(Particle.ASH, getCenter().clone().add(0, 2, 0),
                        3, 3.0, 1.0, 3.0, 0.005);
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_CAVE, 0.4f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            rim.clear(); floor.clear(); debris.clear(); obsidianCenter = null;
        }

        @Override public AbstractAttack newInstance() { return new DeadStarCrater(plugin); }
    }

    // =================================================================
    // 53. HANGING GARDEN OF ROT
    //     Overhead ceiling cluster — 6 vine hangs of warped stem at varying
    //     lengths, each ending in a sculk pod with nether wart, crying
    //     obsidian drip tips. 30 displays. Drip tips extend & retract.
    // =================================================================
    public static class HangingGardenOfRot extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> vines = new ArrayList<>();
        private final List<BlockDisplayHandle> pods = new ArrayList<>();
        private final List<BlockDisplayHandle> warts = new ArrayList<>();
        private final List<BlockDisplayHandle> drips = new ArrayList<>();
        private final double[] dripBaseY = new double[6];
        private final double[] vineBaseAngle = new double[6];

        public HangingGardenOfRot(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hanging_garden_of_rot", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_SPREAD, 1.0f, 0.4f);

            // 6 vine hangs, each: vine length 3-7 + pod + wart + drip = 5 each => 30 displays
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                double rx = Math.cos(angle) * 3.0;
                double rz = Math.sin(angle) * 3.0;
                int length = 3 + (i % 5);
                vineBaseAngle[i] = angle;

                // Vine — single warped stem display elongated
                Location vBase = center.clone().add(rx, 9.0, rz);
                BlockDisplayHandle vine = displayBuilder.spawnBlock(vBase, Material.WARPED_STEM);
                vine.scale(0.4f, length, 0.4f).glow(50, 220, 200).interpolation(12, 0);
                vines.add(vine);

                // Sculk pod cluster — 1 sculk + 1 wart per hang
                Location podLoc = center.clone().add(rx, 9.0 - length, rz);
                BlockDisplayHandle pod = displayBuilder.spawnBlock(podLoc, Material.SCULK);
                pod.scale(1.2f, 1.2f, 1.2f).glow(50, 220, 200).interpolation(10, 0);
                pods.add(pod);

                BlockDisplayHandle wart = displayBuilder.spawnBlock(
                        podLoc.clone().add(0.2, 0.2, 0.2), Material.NETHER_WART_BLOCK);
                wart.scale(0.7f, 0.7f, 0.7f).glow(140, 0, 30).interpolation(10, 0);
                warts.add(wart);

                // Crying obsidian drip tip below pod
                Location dripLoc = podLoc.clone().add(0, -0.8, 0);
                BlockDisplayHandle drip = displayBuilder.spawnBlock(dripLoc, Material.CRYING_OBSIDIAN);
                drip.scale(0.4f, 0.6f, 0.4f).glow(90, 0, 130).interpolation(8, 0);
                drips.add(drip);
                dripBaseY[i] = 9.0 - length - 0.8;
            }
            // 6+6+6+6 = 24, plus extra pod cluster around each = need 30 -> add 6 secondary pods
            for (int i = 0; i < 6; i++) {
                double angle = vineBaseAngle[i];
                Location p = center.clone().add(Math.cos(angle) * 3.0 + 0.4, 9.0 - (3 + (i % 5)) - 0.1,
                        Math.sin(angle) * 3.0 - 0.4);
                BlockDisplayHandle pod = displayBuilder.spawnBlock(p, Material.SCULK);
                pod.scale(0.6f, 0.6f, 0.6f).glow(50, 220, 200).interpolation(10, 0);
                pods.add(pod);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Vine sway — pendulum on X/Z
            if (tick % 4 == 0) {
                for (int i = 0; i < vines.size(); i++) {
                    double angle = vineBaseAngle[i];
                    double rx = Math.cos(angle) * 3.0;
                    double rz = Math.sin(angle) * 3.0;
                    int length = 3 + (i % 5);
                    float swayX = (float) (Math.sin(tick * 0.05 + i) * 0.4);
                    float swayZ = (float) (Math.cos(tick * 0.04 + i * 1.3) * 0.4);
                    vines.get(i).animateTo(
                            new Vector3f((float) rx + swayX - 0.2f, 9.0f, (float) rz + swayZ - 0.2f),
                            new AxisAngle4f((float) (Math.sin(tick * 0.03 + i) * 0.1), 0, 0, 1),
                            new Vector3f(0.4f, length, 0.4f), 4);
                }
            }

            // Pods slow rotation
            if (tick % 8 == 0) {
                for (int i = 0; i < 6 && i < pods.size(); i++) {
                    pods.get(i).animateTo(
                            new Vector3f(-0.6f, 0, -0.6f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                            new Vector3f(1.2f, 1.2f, 1.2f), 8);
                }
            }

            // Drip tips periodically extend then retract
            int dripCycle = tick % 60;
            if (dripCycle == 0) {
                for (int i = 0; i < drips.size(); i++) {
                    drips.get(i).animateTo(
                            new Vector3f(-0.2f, (float) dripBaseY[i] - 1.2f, -0.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 1.0f, 0.4f), 12);
                }
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.5f);
            } else if (dripCycle == 24) {
                for (int i = 0; i < drips.size(); i++) {
                    drips.get(i).animateTo(
                            new Vector3f(-0.2f, (float) dripBaseY[i], -0.2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.4f, 0.6f, 0.4f), 12);
                }
            }

            // Sculk soul + warped spore particles drifting downward
            if (tick % 3 == 0) {
                for (int i = 0; i < pods.size() && i < 6; i++) {
                    double angle = vineBaseAngle[i];
                    double rx = Math.cos(angle) * 3.0;
                    double rz = Math.sin(angle) * 3.0;
                    int length = 3 + (i % 5);
                    Location podLoc = getCenter().clone().add(rx, 9.0 - length, rz);
                    w.spawnParticle(Particle.SCULK_SOUL, podLoc.clone().add(0, -0.5, 0),
                            2, 0.3, 0.5, 0.3, 0.01);
                    w.spawnParticle(Particle.WARPED_SPORE, podLoc, 3, 0.6, 0.6, 0.6, 0.01);
                    if (Math.random() < 0.3) {
                        DisplayBuilder.dustParticles(podLoc.clone().add(0, -0.8, 0), 1, 0.3,
                                90, 0, 130, 1.2f);
                    }
                }
            }

            if (tick % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 0.5f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            vines.clear(); pods.clear(); warts.clear(); drips.clear();
        }

        @Override public AbstractAttack newInstance() { return new HangingGardenOfRot(plugin); }
    }

    // =================================================================
    // 54. PENITENT'S PATH
    //     Long winding deepslate brick path — 15+ floor panels in a curve,
    //     blackstone borders, crying obsidian directional markers.
    //     30 displays. Markers pulse, sections occasionally rotate/dim.
    // =================================================================
    public static class PenitentsPath extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> panels = new ArrayList<>();
        private final List<BlockDisplayHandle> borders = new ArrayList<>();
        private final List<BlockDisplayHandle> markers = new ArrayList<>();
        private final List<Vector> panelPositions = new ArrayList<>();

        public PenitentsPath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("penitents_path", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(320);
            config.setCooldownTicks(280);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_TILES_PLACE, 1.0f, 0.4f);

            // 15 panels along a curving line — panel index drives a curve param
            for (int i = 0; i < 15; i++) {
                double t = (i - 7) * 0.7;
                double x = t;
                double z = Math.sin(t * 0.4) * 2.5;
                Vector pos = new Vector(x, 0.05, z);
                panelPositions.add(pos);

                Location p = center.clone().add(pos.getX(), pos.getY(), pos.getZ());
                BlockDisplayHandle panel = displayBuilder.spawnBlock(p, Material.DEEPSLATE_BRICKS);
                panel.scale(1.1f, 0.15f, 1.1f).interpolation(10, 0);
                panels.add(panel);
            }

            // Borders — one blackstone per side of a sampled set of panels (10)
            for (int i = 0; i < 10; i++) {
                int idx = (int) (i * 1.5);
                if (idx >= panelPositions.size()) idx = panelPositions.size() - 1;
                Vector pos = panelPositions.get(idx);
                double sideX = (i % 2 == 0) ? -0.7 : 0.7;
                Location p = center.clone().add(pos.getX() + sideX, pos.getY(), pos.getZ());
                BlockDisplayHandle b = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                b.scale(0.4f, 0.5f, 1.0f).interpolation(10, 0);
                borders.add(b);
            }

            // Crying obsidian directional markers (5) inlaid in the path
            for (int i = 0; i < 5; i++) {
                int idx = i * 3 + 1;
                if (idx >= panelPositions.size()) idx = panelPositions.size() - 1;
                Vector pos = panelPositions.get(idx);
                Location p = center.clone().add(pos.getX(), pos.getY() + 0.1, pos.getZ());
                BlockDisplayHandle m = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                m.scale(0.5f, 0.05f, 0.5f).glow(90, 0, 130).interpolation(8, 0);
                markers.add(m);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Markers pulse
            if (tick % 5 == 0) {
                for (int i = 0; i < markers.size(); i++) {
                    float s = 0.5f + (float) (Math.sin(tick * 0.12 + i * 0.8) * 0.25 + 0.25);
                    int idx = i * 3 + 1;
                    if (idx >= panelPositions.size()) idx = panelPositions.size() - 1;
                    Vector pos = panelPositions.get(idx);
                    markers.get(i).animateTo(
                            new Vector3f((float) pos.getX() - s / 2f, (float) pos.getY() + 0.1f,
                                    (float) pos.getZ() - s / 2f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(s, 0.05f, s), 5);
                }
            }

            // Path shift — random panel rotates 5° then returns
            if (tick % 35 == 0 && !panels.isEmpty()) {
                int idx = (int) (Math.random() * panels.size());
                Vector pos = panelPositions.get(idx);
                panels.get(idx).animateTo(
                        new Vector3f((float) pos.getX() - 0.55f, (float) pos.getY(),
                                (float) pos.getZ() - 0.55f),
                        new AxisAngle4f((float) Math.toRadians(5), 0, 1, 0),
                        new Vector3f(1.1f, 0.15f, 1.1f), 6);
                // Restore after 12 ticks via a delayed animateTo with a phase
                final int restoreIdx = idx;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (restoreIdx < panels.size()) {
                        panels.get(restoreIdx).animateTo(
                                new Vector3f((float) pos.getX() - 0.55f, (float) pos.getY(),
                                        (float) pos.getZ() - 0.55f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(1.1f, 0.15f, 1.1f), 6);
                    }
                }, 12L);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_STONE_PLACE, 0.7f, 0.5f);
            }

            // Far-end markers occasionally go dark
            if (tick > 120 && tick % 80 == 0 && !markers.isEmpty()) {
                int idx = markers.size() - 1;
                int posIdx = idx * 3 + 1;
                if (posIdx >= panelPositions.size()) posIdx = panelPositions.size() - 1;
                Vector pos = panelPositions.get(posIdx);
                markers.get(idx).animateTo(
                        new Vector3f((float) pos.getX(), (float) pos.getY() + 0.1f, (float) pos.getZ()),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 16);
            }

            // Soul fire markers + crying obsidian drip particles
            if (tick % 4 == 0) {
                for (int i = 0; i < markers.size(); i++) {
                    int idx = i * 3 + 1;
                    if (idx >= panelPositions.size()) idx = panelPositions.size() - 1;
                    Vector pos = panelPositions.get(idx);
                    Location loc = getCenter().clone().add(pos.getX(), 0.3, pos.getZ());
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 1, 0.15, 0.1, 0.15, 0.005);
                    DisplayBuilder.dustParticles(loc, 1, 0.15, 90, 0, 130, 1.0f);
                }
                for (BlockDisplayHandle b : borders) {
                    if (Math.random() < 0.1) {
                        Location bl = b.entity().getLocation().add(0, -0.3, 0);
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, bl, 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, 0.3f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            panels.clear(); borders.clear(); markers.clear(); panelPositions.clear();
        }

        @Override public AbstractAttack newInstance() { return new PenitentsPath(plugin); }
    }

    // =================================================================
    // 55. FORSAKEN BELL TOWER
    //     Squat belfry frame (4x4x5) of blackstone bricks with arched
    //     deepslate openings, large hemispherical bell of blackstone, bone
    //     clapper. Bell sways, clapper strikes, shockwave rings.
    //     26 displays. Active hazard via shockwave on impact.
    // =================================================================
    public static class ForsakenBellTower extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> frame = new ArrayList<>();
        private final List<BlockDisplayHandle> arches = new ArrayList<>();
        private final List<BlockDisplayHandle> bell = new ArrayList<>();
        private BlockDisplayHandle clapper;
        private int lastImpactTick = -100;

        public ForsakenBellTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("forsaken_bell_tower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(280);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_DEEPSLATE_TILES_PLACE, 1.0f, 0.4f);

            // Frame: 4 corner pillars (5 tall = 4 displays, scale Y=5) + base = 8 frame pieces
            double[][] corners = {{-1.5, -1.5}, {1.5, -1.5}, {1.5, 1.5}, {-1.5, 1.5}};
            for (double[] c : corners) {
                Location p = center.clone().add(c[0], 0.0, c[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.5f, 5.0f, 0.5f).interpolation(10, 0);
                frame.add(h);
            }
            // Top crossbeams (4)
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI / 2 * i;
                Location p = center.clone().add(Math.cos(angle) * 1.5, 5.0, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(3.0f, 0.4f, 0.4f).interpolation(10, 0);
                if (i % 2 == 1) h.rotate((float) (Math.PI / 2), 0, 1, 0);
                frame.add(h);
            }

            // Arched openings — 4 deepslate tile arches at each face
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI / 2 * i;
                Location p = center.clone().add(Math.cos(angle) * 1.5, 2.5, Math.sin(angle) * 1.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(2.5f, 1.5f, 0.2f).interpolation(10, 0);
                if (i % 2 == 1) h.rotate((float) (Math.PI / 2), 0, 1, 0);
                arches.add(h);
            }

            // Bell — hemispherical cluster of 9 blackstone displays
            double[][] bellOff = {
                    {0, 0, 0}, {-0.7, 0, 0}, {0.7, 0, 0}, {0, 0, -0.7}, {0, 0, 0.7},
                    {-0.5, 0.4, 0}, {0.5, 0.4, 0}, {0, 0.4, -0.5}, {0, 0.4, 0.5}
            };
            for (double[] o : bellOff) {
                Location p = center.clone().add(o[0], 3.5 + o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.6f, 0.6f, 0.6f).interpolation(8, 0);
                bell.add(h);
            }

            // Bone clapper hanging below bell
            clapper = displayBuilder.spawnBlock(center.clone().add(0, 2.4, 0), Material.BONE_BLOCK);
            clapper.scale(0.3f, 1.0f, 0.3f).glow(240, 230, 200).interpolation(6, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Bell sways with sine — clapper delays slightly behind
            double bellAngle = Math.sin(tick * 0.08) * 0.35;
            double clapperAngle = Math.sin((tick - 4) * 0.08) * 0.5;

            if (tick % 4 == 0) {
                // Bell tilt
                for (int i = 0; i < bell.size(); i++) {
                    double[] o;
                    double[][] bellOff = {
                            {0, 0, 0}, {-0.7, 0, 0}, {0.7, 0, 0}, {0, 0, -0.7}, {0, 0, 0.7},
                            {-0.5, 0.4, 0}, {0.5, 0.4, 0}, {0, 0.4, -0.5}, {0, 0.4, 0.5}
                    };
                    o = bellOff[i];
                    float ox = (float) (o[0] + bellAngle * 0.8);
                    float oy = (float) (3.5 + o[1]);
                    float oz = (float) (o[2]);
                    bell.get(i).animateTo(
                            new Vector3f(ox - 0.3f, oy, oz - 0.3f),
                            new AxisAngle4f((float) bellAngle, 0, 0, 1),
                            new Vector3f(0.6f, 0.6f, 0.6f), 4);
                }

                // Clapper sway
                if (clapper != null) {
                    float cx = (float) (clapperAngle * 1.2);
                    clapper.animateTo(
                            new Vector3f(cx - 0.15f, 2.4f, -0.15f),
                            new AxisAngle4f((float) clapperAngle, 0, 0, 1),
                            new Vector3f(0.3f, 1.0f, 0.3f), 4);
                }
            }

            // Detect bell extreme — clapper contacts bell wall
            if (Math.abs(clapperAngle) > 0.45 && tick - lastImpactTick > 20) {
                lastImpactTick = tick;
                DisplayBuilder.playSound(getCenter().clone().add(0, 4, 0),
                        Sound.BLOCK_ANVIL_LAND, 1.5f, 0.3f);
                DisplayBuilder.playSound(getCenter().clone().add(0, 4, 0),
                        Sound.BLOCK_BELL_USE, 2.0f, 0.5f);

                // Tower shake — quick frame jitter
                for (BlockDisplayHandle f : frame) {
                    Vector3f cur = new Vector3f(
                            (float) ((Math.random() - 0.5) * 0.15),
                            0,
                            (float) ((Math.random() - 0.5) * 0.15));
                    f.animateTo(cur, new AxisAngle4f(0, 0, 1, 0),
                            f.entity().getTransformation().getScale(), 3);
                }

                // Shockwave ring — visible stone-dust expanding ring
                w.spawnParticle(Particle.BLOCK, getCenter().clone().add(0, 4, 0), 30, 1.5, 0.5, 1.5,
                        0.05, Material.BLACKSTONE.createBlockData());
            }

            // Expand shockwave ring after impact
            int sinceImpact = tick - lastImpactTick;
            if (sinceImpact > 0 && sinceImpact < 30 && sinceImpact % 2 == 0) {
                double r = sinceImpact * 0.25;
                int points = 24;
                for (int i = 0; i < points; i++) {
                    double angle = Math.PI * 2 * i / points;
                    Location loc = getCenter().clone().add(Math.cos(angle) * r, 0.3, Math.sin(angle) * r);
                    DisplayBuilder.dustParticles(loc, 1, 0.1, 240, 230, 200, 1.6f);
                    w.spawnParticle(Particle.SMOKE, loc, 1, 0.05, 0.05, 0.05, 0.005);
                }
            }

            // Ambient creak
            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_BREAK, 0.5f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            frame.clear(); arches.clear(); bell.clear(); clapper = null;
        }

        @Override public AbstractAttack newInstance() { return new ForsakenBellTower(plugin); }
    }

    // =================================================================
    // 56. THE DROWNED CHANDELIER
    //     Ornate chandelier — 4-display hub, 8 arms (3 each), shroomlight
    //     candle tips, crying obsidian wax drips, red glass dome.
    //     34 displays. Sways, drops, arm-snap-and-reform. Wax drop hazard.
    // =================================================================
    public static class DrownedChandelier extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> hub = new ArrayList<>();
        private final List<BlockDisplayHandle> arms = new ArrayList<>();
        private final List<BlockDisplayHandle> candles = new ArrayList<>();
        private final List<BlockDisplayHandle> waxDrips = new ArrayList<>();
        private BlockDisplayHandle dome;

        public DrownedChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("drowned_chandelier", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(4.5);
            config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.4f);
            Location anchor = center.clone().add(0, 8, 0);

            // Central hub — 4-display cluster
            double[][] hubOff = {{-0.3, 0, -0.3}, {0.3, 0, -0.3}, {-0.3, 0, 0.3}, {0.3, 0, 0.3}};
            for (double[] o : hubOff) {
                Location p = anchor.clone().add(o[0], o[1], o[2]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 50, 50).interpolation(8, 0);
                hub.add(h);
            }

            // 8 arms radiating outward — 3 blocks long each = 24 displays for arms
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                for (int j = 0; j < 3; j++) {
                    double r = 0.6 + j * 0.7;
                    Location p = anchor.clone().add(Math.cos(angle) * r, -j * 0.15, Math.sin(angle) * r);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                    h.scale(0.35f, 0.35f, 0.35f).interpolation(8, 0);
                    arms.add(h);
                }

                // Shroomlight candle at each arm tip
                Location tip = anchor.clone().add(Math.cos(angle) * 2.2, -0.3, Math.sin(angle) * 2.2);
                BlockDisplayHandle c = displayBuilder.spawnBlock(tip, Material.SHROOMLIGHT);
                c.scale(0.45f, 0.6f, 0.45f).glow(255, 200, 80).interpolation(8, 0);
                candles.add(c);

                // Wax drip below candle
                Location waxLoc = tip.clone().add(0, -0.6, 0);
                BlockDisplayHandle wax = displayBuilder.spawnBlock(waxLoc, Material.CRYING_OBSIDIAN);
                wax.scale(0.25f, 0.5f, 0.25f).glow(255, 50, 50).interpolation(6, 0);
                waxDrips.add(wax);
            }

            // Red glass dome above hub
            dome = displayBuilder.spawnBlock(anchor.clone().add(0, 0.5, 0), Material.RED_STAINED_GLASS);
            dome.scale(1.8f, 0.6f, 1.8f).glow(255, 30, 30).interpolation(8, 0);
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Circular swing — alternating X/Z rotation
            double swingX = Math.sin(tick * 0.06) * 0.25;
            double swingZ = Math.cos(tick * 0.06) * 0.25;

            if (tick % 4 == 0) {
                // Animate the entire chandelier's positional offset by drift on hub
                for (int i = 0; i < hub.size(); i++) {
                    double[][] hubOff = {{-0.3, 0, -0.3}, {0.3, 0, -0.3}, {-0.3, 0, 0.3}, {0.3, 0, 0.3}};
                    double[] o = hubOff[i];
                    hub.get(i).animateTo(
                            new Vector3f((float) (o[0] + swingX) - 0.25f, 8 + (float) o[1],
                                    (float) (o[2] + swingZ) - 0.25f),
                            new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                            new Vector3f(0.5f, 0.5f, 0.5f), 4);
                }

                // Arms follow swing
                for (int i = 0; i < arms.size(); i++) {
                    int armIdx = i / 3;
                    int seg = i % 3;
                    double angle = Math.PI * 2 * armIdx / 8;
                    double r = 0.6 + seg * 0.7;
                    float tx = (float) (Math.cos(angle) * r + swingX);
                    float ty = 8f - seg * 0.15f;
                    float tz = (float) (Math.sin(angle) * r + swingZ);
                    arms.get(i).animateTo(
                            new Vector3f(tx - 0.175f, ty, tz - 0.175f),
                            new AxisAngle4f((float) swingX, 0, 0, 1),
                            new Vector3f(0.35f, 0.35f, 0.35f), 4);
                }

                // Candles
                for (int i = 0; i < candles.size(); i++) {
                    double angle = Math.PI * 2 * i / 8;
                    float tx = (float) (Math.cos(angle) * 2.2 + swingX);
                    float ty = 7.7f;
                    float tz = (float) (Math.sin(angle) * 2.2 + swingZ);
                    float s = 0.45f + (float) Math.sin(tick * 0.2 + i) * 0.08f;
                    candles.get(i).animateTo(
                            new Vector3f(tx - s / 2f, ty, tz - s / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, 0.6f, s), 4);
                }

                // Wax drips follow swing
                for (int i = 0; i < waxDrips.size(); i++) {
                    double angle = Math.PI * 2 * i / 8;
                    float tx = (float) (Math.cos(angle) * 2.2 + swingX);
                    float ty = 7.1f;
                    float tz = (float) (Math.sin(angle) * 2.2 + swingZ);
                    waxDrips.get(i).animateTo(
                            new Vector3f(tx - 0.125f, ty, tz - 0.125f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.25f, 0.5f, 0.25f), 4);
                }
            }

            // Periodically the chandelier drops 2 blocks then yanks back
            int dropCycle = tick % 100;
            if (dropCycle == 50 && dome != null) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHAIN_BREAK, 1.2f, 0.4f);
                // Quick visual "drop": dome scales down and offsets down
                dome.animateTo(new Vector3f(-0.9f, 6.5f, -0.9f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.8f, 0.6f, 1.8f), 8);
            } else if (dropCycle == 70 && dome != null) {
                dome.animateTo(new Vector3f(-0.9f, 8.5f, -0.9f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(1.8f, 0.6f, 1.8f), 8);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.4f);
            }

            // Arm snap & reform (simulated by scaling to 0 and back)
            if (tick > 0 && tick % 80 == 0 && tick < dur - 40) {
                int snapIdx = (int) (Math.random() * candles.size());
                candles.get(snapIdx).animateTo(
                        new Vector3f(0, 8, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 12);
                final int reformIdx = snapIdx;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (reformIdx < candles.size()) {
                        double angle = Math.PI * 2 * reformIdx / 8;
                        float tx = (float) (Math.cos(angle) * 2.2);
                        float tz = (float) (Math.sin(angle) * 2.2);
                        candles.get(reformIdx).animateTo(
                                new Vector3f(tx - 0.225f, 7.7f, tz - 0.225f),
                                new AxisAngle4f(0, 0, 1, 0),
                                new Vector3f(0.45f, 0.6f, 0.45f), 12);
                    }
                }, 30L);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
            }

            // Particle effects: wax drip + candle fire + dome glow
            if (tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    Location candle = getCenter().clone().add(
                            Math.cos(angle) * 2.2 + swingX, 8.0, Math.sin(angle) * 2.2 + swingZ);
                    w.spawnParticle(Particle.FLAME, candle, 1, 0.1, 0.05, 0.1, 0.005);
                    w.spawnParticle(Particle.SMALL_FLAME, candle, 1, 0.08, 0.05, 0.08, 0.003);
                    Location wax = candle.clone().add(0, -0.7, 0);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, wax, 1, 0.05, 0.1, 0.05, 0);
                    if (Math.random() < 0.2) {
                        DisplayBuilder.dustParticles(wax.clone().add(0, -1.0, 0), 1, 0.1,
                                255, 50, 50, 1.4f);
                    }
                }
                w.spawnParticle(Particle.DUST,
                        getCenter().clone().add(swingX, 8.5, swingZ),
                        1, 0.5, 0.2, 0.5, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 30, 30), 1.6f));
            }

            if (tick % 35 == 0) {
                DisplayBuilder.playSound(getCenter().clone().add(0, 8, 0),
                        Sound.BLOCK_CANDLE_AMBIENT, 0.6f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            hub.clear(); arms.clear(); candles.clear(); waxDrips.clear(); dome = null;
        }

        @Override public AbstractAttack newInstance() { return new DrownedChandelier(plugin); }
    }

    // =================================================================
    // 57. CORRUPTED ALTAR
    //     Ritual altar — 5x3x2 base, carved front, shroomlight inlay,
    //     crying obsidian channels, sculk corruption creeping up.
    //     28 displays. Atmosphere with escalation indicator.
    // =================================================================
    public static class CorruptedAltar extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> base = new ArrayList<>();
        private final List<BlockDisplayHandle> sculkSpread = new ArrayList<>();
        private final List<BlockDisplayHandle> channels = new ArrayList<>();
        private BlockDisplayHandle frontPanel;
        private BlockDisplayHandle topLight;
        private int sculkSpawned = 0;

        public CorruptedAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrupted_altar", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(2.0);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(35);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_SCULK_PLACE, 1.0f, 0.4f);

            // Base — 5x3 footprint, 2 tall = 15 displays at top tier
            for (int x = -2; x <= 2; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location p = center.clone().add(x, 0.5, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                    h.scale(1.0f, 1.0f, 1.0f).interpolation(8, 0);
                    base.add(h);
                }
            }

            // Top shroomlight inlay
            topLight = displayBuilder.spawnBlock(center.clone().add(0, 1.1, 0), Material.SHROOMLIGHT);
            topLight.scale(2.0f, 0.15f, 1.4f).glow(255, 200, 80).interpolation(8, 0);

            // Carved front panel
            frontPanel = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 1.05), Material.BLACKSTONE);
            frontPanel.scale(3.0f, 1.5f, 0.2f).glow(140, 0, 30).interpolation(8, 0);

            // 4 crying obsidian channels carved into top surface
            double[][] chOff = {{-1.5, 0}, {1.5, 0}, {0, -0.7}, {0, 0.7}};
            for (double[] o : chOff) {
                Location p = center.clone().add(o[0], 1.05, o[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.4f, 0.05f, 0.4f).glow(90, 0, 130).interpolation(6, 0);
                channels.add(h);
            }

            // Initial sculk corruption — 4 displays at base of sides
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 2 * i / 4;
                Location p = center.clone().add(Math.cos(angle) * 2.4, 0.05, Math.sin(angle) * 1.3);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SCULK);
                h.scale(0.7f, 0.3f, 0.7f).glow(50, 220, 200).interpolation(8, 0);
                sculkSpread.add(h);
            }
            sculkSpawned = 4;
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Top shroomlight pulse
            if (tick % 5 == 0 && topLight != null) {
                float s = 2.0f + (float) Math.sin(tick * 0.08) * 0.25f;
                topLight.animateTo(new Vector3f(-s / 2f, 1.1f, -0.7f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(s, 0.15f, 1.4f), 5);
            }

            // Channels glow cycle
            if (tick % 6 == 0) {
                for (int i = 0; i < channels.size(); i++) {
                    float s = 0.4f + (float) Math.sin(tick * 0.1 + i * 1.0) * 0.15f;
                    double[][] chOff = {{-1.5, 0}, {1.5, 0}, {0, -0.7}, {0, 0.7}};
                    double[] o = chOff[i];
                    channels.get(i).animateTo(
                            new Vector3f((float) o[0] - s / 2f, 1.05f, (float) o[1] - s / 2f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(s, 0.05f, s), 6);
                }
            }

            // Sculk slowly spreads further up the sides — every 60 ticks add one
            if (tick > 0 && tick % 60 == 0 && sculkSpawned < 8) {
                double angle = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 1.0;
                Location p = getCenter().clone().add(Math.cos(angle) * 2.0,
                        0.4 + sculkSpawned * 0.1, Math.sin(angle) * 1.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SCULK);
                h.scale(0.01f, 0.01f, 0.01f).glow(50, 220, 200).interpolation(20, 0);
                h.animateTo(new Vector3f(-0.3f, 0.4f + sculkSpawned * 0.1f, -0.3f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.6f, 0.4f, 0.6f), 20);
                sculkSpread.add(h);
                sculkSpawned++;
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SPREAD, 0.7f, 0.4f);
            }

            // Carved front periodically opens (extends forward)
            int openCycle = tick % 120;
            if (openCycle == 60 && frontPanel != null) {
                frontPanel.animateTo(new Vector3f(-1.5f, 0.5f, 1.5f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(3.0f, 1.5f, 0.2f), 10);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_CHISELED_BOOKSHELF_PICKUP, 1.0f, 0.5f);
            } else if (openCycle == 95 && frontPanel != null) {
                frontPanel.animateTo(new Vector3f(-1.5f, 0.5f, 1.05f),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(3.0f, 1.5f, 0.2f), 10);
            }

            // Particles
            if (tick % 3 == 0) {
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 1.3, 0),
                        2, 1.2, 0.1, 0.7, 0.005);
                if (Math.random() < 0.3) {
                    w.spawnParticle(Particle.SCULK_CHARGE_POP,
                            getCenter().clone().add(
                                    (Math.random() - 0.5) * 4,
                                    0.5,
                                    (Math.random() - 0.5) * 2),
                            1, 0.2, 0.2, 0.2, 0);
                }
                for (double[] o : new double[][]{{-1.5, 0}, {1.5, 0}, {0, -0.7}, {0, 0.7}}) {
                    Location chLoc = getCenter().clone().add(o[0], 1.1, o[1]);
                    if (Math.random() < 0.4) {
                        w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, chLoc, 1, 0.1, 0.05, 0.1, 0);
                    }
                }
            }

            if (tick % 50 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_SCULK_SHRIEKER_PLACE, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            base.clear(); sculkSpread.clear(); channels.clear();
            frontPanel = null; topLight = null;
        }

        @Override public AbstractAttack newInstance() { return new CorruptedAltar(plugin); }
    }

    // =================================================================
    // 58. BLOOD MOON RISE
    //     Large vertical red-glass moon (~6 wide, 20+ disks), crying
    //     obsidian corona, blackstone crescent shadow. Rises slowly,
    //     corona pulses, shadow rotates, blood cascade.
    //     30 displays. Atmospheric backdrop.
    // =================================================================
    public static class BloodMoonRise extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> moonFace = new ArrayList<>();
        private final List<BlockDisplayHandle> corona = new ArrayList<>();
        private final List<BlockDisplayHandle> shadow = new ArrayList<>();
        private double yOffset = -8.0;

        public BloodMoonRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_moon_rise", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.AMBIENT_NETHER_WASTES_MOOD, 1.0f, 0.3f);
            // Moon face — 20 displays in a circular vertical disk pattern at radius 3
            int faceCount = 20;
            for (int i = 0; i < faceCount; i++) {
                // Concentric pattern: 1 center + 5 inner ring + 14 outer ring
                double angle, r;
                if (i == 0) { angle = 0; r = 0; }
                else if (i < 6) { angle = Math.PI * 2 * (i - 1) / 5; r = 1.2; }
                else { angle = Math.PI * 2 * (i - 6) / 14; r = 2.5; }

                Location p = center.clone().add(0, yOffset + Math.sin(angle) * r,
                        Math.cos(angle) * r * 0.0); // vertical face — X is screen-X, Y is height
                // The "moon" is a vertical disk parallel to XZ at fixed Z. So X=cos, Y=sin around center.
                Location pp = center.clone().add(Math.cos(angle) * r, yOffset + Math.sin(angle) * r, 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(pp, Material.RED_STAINED_GLASS);
                h.scale(0.8f, 0.8f, 0.2f).glow(255, 30, 30).interpolation(20, 0);
                moonFace.add(h);
            }

            // Corona — 8 crying obsidian halo displays around moon
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * i / 8;
                Location p = center.clone().add(Math.cos(angle) * 3.5, yOffset + Math.sin(angle) * 3.5, 8);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CRYING_OBSIDIAN);
                h.scale(0.5f, 0.5f, 0.15f).glow(140, 0, 30).interpolation(20, 0);
                corona.add(h);
            }

            // Shadow crescent — 2 blackstone displays on one edge
            for (int i = 0; i < 2; i++) {
                Location p = center.clone().add(2.0 + i * 0.4, yOffset + 0.5 - i * 0.3, 7.9);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(0.8f, 0.8f, 0.1f).interpolation(20, 0);
                shadow.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();
            int dur = config.getDurationTicks();

            // Rise phase — first 100 ticks, ascend yOffset to +6
            int riseEnd = 100;
            double targetY;
            if (tick < riseEnd) {
                targetY = -8.0 + (14.0 * tick / riseEnd);
            } else {
                targetY = 6.0;
            }

            if (tick % 6 == 0) {
                // Re-position all moon face displays for slow ascent
                for (int i = 0; i < moonFace.size(); i++) {
                    double angle, r;
                    if (i == 0) { angle = 0; r = 0; }
                    else if (i < 6) { angle = Math.PI * 2 * (i - 1) / 5; r = 1.2; }
                    else { angle = Math.PI * 2 * (i - 6) / 14; r = 2.5; }
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = (float) (targetY + Math.sin(angle) * r);
                    moonFace.get(i).animateTo(
                            new Vector3f(tx - 0.4f, ty, 7.9f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.2f), 6);
                }

                // Corona pulse + ascent
                for (int i = 0; i < corona.size(); i++) {
                    double angle = Math.PI * 2 * i / 8;
                    float s = 0.5f + (float) Math.sin(tick * 0.12 + i) * 0.15f;
                    float tx = (float) (Math.cos(angle) * 3.5);
                    float ty = (float) (targetY + Math.sin(angle) * 3.5);
                    corona.get(i).animateTo(
                            new Vector3f(tx - s / 2f, ty - s / 2f, 7.9f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 0, 1),
                            new Vector3f(s, s, 0.15f), 6);
                }

                // Shadow crescent — slowly orbits the face center (rotates around moon)
                double shadowOrbitAngle = tick * 0.03;
                for (int i = 0; i < shadow.size(); i++) {
                    double r = 2.0 + i * 0.4;
                    float tx = (float) (Math.cos(shadowOrbitAngle) * r);
                    float ty = (float) (targetY + Math.sin(shadowOrbitAngle) * r - i * 0.3);
                    shadow.get(i).animateTo(
                            new Vector3f(tx - 0.4f, ty, 7.85f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.1f), 6);
                }
            }

            // Bleed phase — every 80 ticks the moon "bleeds" red particles cascading
            if (tick % 80 == 40 && tick > riseEnd) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_GHAST_HURT, 0.8f, 0.3f);
                for (int i = 0; i < 20; i++) {
                    double dx = (Math.random() - 0.5) * 5.0;
                    double dy = targetY - 1.0;
                    Location p = getCenter().clone().add(dx, dy, 7.9);
                    DisplayBuilder.dustParticles(p, 2, 0.15, 200, 0, 0, 1.4f);
                }
            }
            // Bleed cascade — drips downward over 30 ticks after bleed pulse
            int bleedPhase = tick % 80;
            if (bleedPhase >= 40 && bleedPhase < 70 && tick > riseEnd) {
                int dropFrame = bleedPhase - 40;
                for (int i = 0; i < 6; i++) {
                    double dx = (Math.random() - 0.5) * 4.5;
                    double dy = targetY - 1.0 - dropFrame * 0.4;
                    Location p = getCenter().clone().add(dx, dy, 7.9);
                    DisplayBuilder.dustParticles(p, 1, 0.1, 200, 0, 0, 1.2f);
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR, p, 1, 0.1, 0.1, 0.1, 0);
                }
            }

            // Crimson glow halo
            if (tick % 4 == 0 && tick > 30) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * 2 * i / 8;
                    Location p = getCenter().clone().add(
                            Math.cos(angle) * 4.0,
                            targetY + Math.sin(angle) * 4.0, 7.9);
                    DisplayBuilder.dustParticles(p, 1, 0.2, 255, 30, 30, 1.6f);
                }
                w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,
                        getCenter().clone().add(0, targetY - 2.5, 7.9),
                        2, 2.5, 0.5, 0.2, 0);
            }

            if (tick % 60 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.AMBIENT_NETHER_WASTES_LOOP, 0.4f, 0.3f);
            }

            if (tick == dur - 30) {
                // Set down — moon descends
                for (int i = 0; i < moonFace.size(); i++) {
                    double angle, r;
                    if (i == 0) { angle = 0; r = 0; }
                    else if (i < 6) { angle = Math.PI * 2 * (i - 1) / 5; r = 1.2; }
                    else { angle = Math.PI * 2 * (i - 6) / 14; r = 2.5; }
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = (float) (-8.0 + Math.sin(angle) * r);
                    moonFace.get(i).animateTo(
                            new Vector3f(tx - 0.4f, ty, 7.9f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.8f, 0.8f, 0.2f), 28);
                }
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            moonFace.clear(); corona.clear(); shadow.clear();
        }

        @Override public AbstractAttack newInstance() { return new BloodMoonRise(plugin); }
    }

    // =================================================================
    // 59. SKELETON CHARIOT WRECKAGE
    //     Crashed chariot — 2 broken bone-block wheel arcs, blackstone axle,
    //     deepslate tile floor planks angled & broken, calcite fragments.
    //     26 displays. Static wreckage, drifting fragments, slow wheel spin.
    // =================================================================
    public static class ChariotWreckage extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> wheelA = new ArrayList<>();
        private final List<BlockDisplayHandle> wheelB = new ArrayList<>();
        private final List<BlockDisplayHandle> axle = new ArrayList<>();
        private final List<BlockDisplayHandle> body = new ArrayList<>();
        private final List<BlockDisplayHandle> fragments = new ArrayList<>();
        private int crumbledIdx = 0;

        public ChariotWreckage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chariot_wreckage", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(1.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(40);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.4f);

            // Two broken wheels — each is an arc of 6 bone block displays (not full circle)
            for (int side = 0; side < 2; side++) {
                List<BlockDisplayHandle> wheel = (side == 0) ? wheelA : wheelB;
                double sideX = (side == 0) ? -1.8 : 1.8;
                for (int i = 0; i < 6; i++) {
                    // Skip 2 of 8 positions => arc is 6/8
                    double angle = Math.PI * 2 * i / 8;
                    Location p = center.clone().add(sideX, 0.8 + Math.sin(angle) * 0.9,
                            Math.cos(angle) * 0.9);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BONE_BLOCK);
                    h.scale(0.35f, 0.35f, 0.35f).glow(240, 230, 200).interpolation(8, 0);
                    wheel.add(h);
                }
            }

            // Axle — 3 blackstone connecting wheels
            for (int i = 0; i < 3; i++) {
                Location p = center.clone().add(-1.0 + i * 1.0, 0.8, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BLACKSTONE);
                h.scale(1.2f, 0.3f, 0.3f).interpolation(8, 0);
                axle.add(h);
            }

            // Body planks — 6 deepslate tile displays angled and broken
            for (int i = 0; i < 6; i++) {
                double xOff = -1.5 + (i % 3) * 1.5;
                double zOff = (i / 3 == 0) ? -0.5 : 0.5;
                Location p = center.clone().add(xOff, 1.0 + Math.random() * 0.3, zOff);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.DEEPSLATE_TILES);
                h.scale(1.0f, 0.2f, 0.7f).interpolation(8, 0);
                h.rotate((float) (Math.random() * 0.6 - 0.3), 0, 0, 1);
                body.add(h);
            }

            // Calcite fragments — 5 scattered pieces
            for (int i = 0; i < 5; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = 1.5 + Math.random() * 2.0;
                Location p = center.clone().add(Math.cos(angle) * r,
                        0.2 + Math.random() * 0.4, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.CALCITE);
                h.scale(0.3f, 0.3f, 0.3f).glow(240, 230, 200).interpolation(8, 0);
                fragments.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Wheels slowly spin despite being broken
            if (tick % 6 == 0) {
                for (int side = 0; side < 2; side++) {
                    List<BlockDisplayHandle> wheel = (side == 0) ? wheelA : wheelB;
                    double sideX = (side == 0) ? -1.8 : 1.8;
                    for (int i = 0; i < wheel.size(); i++) {
                        double angle = Math.PI * 2 * i / 8 + tick * 0.025 * (side == 0 ? 1 : -1);
                        float tx = (float) sideX;
                        float ty = 0.8f + (float) Math.sin(angle) * 0.9f;
                        float tz = (float) Math.cos(angle) * 0.9f;
                        wheel.get(i).animateTo(
                                new Vector3f(tx - 0.175f, ty, tz - 0.175f),
                                new AxisAngle4f((float) angle, 1, 0, 0),
                                new Vector3f(0.35f, 0.35f, 0.35f), 6);
                    }
                }
            }

            // Fragments drift gently
            if (tick % 8 == 0) {
                for (int i = 0; i < fragments.size(); i++) {
                    float jx = (float) (Math.sin(tick * 0.03 + i) * 0.1);
                    float jy = (float) (Math.cos(tick * 0.04 + i) * 0.1);
                    float jz = (float) (Math.sin(tick * 0.025 + i * 1.3) * 0.1);
                    Location origin = fragments.get(i).entity().getLocation();
                    fragments.get(i).animateTo(
                            new Vector3f(jx - 0.15f, jy, jz - 0.15f),
                            new AxisAngle4f((float) (tick * 0.04), 0, 1, 0),
                            new Vector3f(0.3f, 0.3f, 0.3f), 8);
                }
            }

            // Occasional fragment crumbles to 0
            if (tick > 0 && tick % 100 == 0 && crumbledIdx < fragments.size()) {
                BlockDisplayHandle h = fragments.get(crumbledIdx++);
                h.animateTo(new Vector3f(0, 0, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f, 0.01f, 0.01f), 16);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_BONE_BLOCK_BREAK, 0.7f, 0.4f);
                w.spawnParticle(Particle.BLOCK, h.entity().getLocation(), 12, 0.2, 0.2, 0.2, 0.05,
                        Material.CALCITE.createBlockData());
            }

            // Bone dust + ember + calcite shards
            if (tick % 5 == 0) {
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 1.0, 0),
                        4, 2.0, 240, 230, 200, 1.2f);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, getCenter().clone().add(0, 0.6, 0),
                        2, 1.5, 0.1, 1.0, 0.005);
                if (Math.random() < 0.4) {
                    Location p = getCenter().clone().add(
                            (Math.random() - 0.5) * 4,
                            0.4 + Math.random() * 0.6,
                            (Math.random() - 0.5) * 2);
                    w.spawnParticle(Particle.WHITE_ASH, p, 1, 0.1, 0.1, 0.1, 0.005);
                }
            }

            if (tick % 70 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.ENTITY_SKELETON_AMBIENT, 0.4f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            wheelA.clear(); wheelB.clear(); axle.clear(); body.clear(); fragments.clear();
        }

        @Override public AbstractAttack newInstance() { return new ChariotWreckage(plugin); }
    }

    // =================================================================
    // 60. HELLFIRE BRAZIER
    //     Standing brazier — 3 basalt tripod legs, wide blackstone bowl
    //     (4 displays, oval), magma fire bed, shroomlight ember cluster.
    //     14 displays. Active hazard via overflow drips.
    // =================================================================
    public static class HellfireBrazier extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> legs = new ArrayList<>();
        private final List<BlockDisplayHandle> bowl = new ArrayList<>();
        private final List<BlockDisplayHandle> embers = new ArrayList<>();
        private BlockDisplayHandle fireBed;

        public HellfireBrazier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_brazier", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(3.5);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }

        @Override
        protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.0f, 0.5f);

            // 3 basalt tripod legs
            for (int i = 0; i < 3; i++) {
                double angle = Math.PI * 2 * i / 3;
                Location p = center.clone().add(Math.cos(angle) * 0.7, 0.5, Math.sin(angle) * 0.7);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.BASALT);
                h.scale(0.35f, 1.5f, 0.35f).interpolation(8, 0);
                // Slight outward tilt
                h.rotate((float) 0.2f, (float) Math.cos(angle), 0, (float) Math.sin(angle));
                legs.add(h);
            }

            // Wide blackstone bowl — 4 displays in oval
            double[][] bowlOff = {{-0.7, 0}, {0.7, 0}, {0, -0.5}, {0, 0.5}};
            for (double[] o : bowlOff) {
                Location p = center.clone().add(o[0], 1.6, o[1]);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.POLISHED_BLACKSTONE_BRICKS);
                h.scale(0.7f, 0.5f, 0.7f).interpolation(6, 0);
                bowl.add(h);
            }

            // Magma fire bed inside bowl
            fireBed = displayBuilder.spawnBlock(center.clone().add(0, 1.85, 0), Material.MAGMA_BLOCK);
            fireBed.scale(1.0f, 0.3f, 1.0f).glow(255, 120, 30).interpolation(6, 0);

            // Shroomlight embers — 6 cluster on top
            for (int i = 0; i < 6; i++) {
                double angle = Math.PI * 2 * i / 6;
                double r = 0.3;
                Location p = center.clone().add(Math.cos(angle) * r, 2.05, Math.sin(angle) * r);
                BlockDisplayHandle h = displayBuilder.spawnBlock(p, Material.SHROOMLIGHT);
                h.scale(0.3f, 0.3f, 0.3f).glow(255, 200, 80).interpolation(4, 0);
                embers.add(h);
            }
        }

        @Override
        protected void onTick(int tick) {
            if (getCenter() == null || getCenter().getWorld() == null) return;
            World w = getCenter().getWorld();

            // Embers pulse
            if (tick % 4 == 0) {
                for (int i = 0; i < embers.size(); i++) {
                    double angle = Math.PI * 2 * i / 6 + tick * 0.05;
                    double r = 0.3 + Math.sin(tick * 0.1 + i) * 0.1;
                    float tx = (float) (Math.cos(angle) * r);
                    float ty = 2.05f + (float) Math.sin(tick * 0.15 + i) * 0.15f;
                    float tz = (float) (Math.sin(angle) * r);
                    float s = 0.3f + (float) Math.sin(tick * 0.2 + i) * 0.1f;
                    embers.get(i).animateTo(
                            new Vector3f(tx - s / 2f, ty, tz - s / 2f),
                            new AxisAngle4f((float) (tick * 0.1), 0, 1, 0),
                            new Vector3f(s, s, s), 4);
                }
            }

            // Legs occasionally shudder
            if (tick % 50 == 0 && !legs.isEmpty()) {
                int idx = (int) (Math.random() * legs.size());
                double angle = Math.PI * 2 * idx / 3;
                float jx = (float) ((Math.random() - 0.5) * 0.15);
                float jz = (float) ((Math.random() - 0.5) * 0.15);
                legs.get(idx).animateTo(
                        new Vector3f((float) (Math.cos(angle) * 0.7) + jx - 0.175f, 0.5f,
                                (float) (Math.sin(angle) * 0.7) + jz - 0.175f),
                        new AxisAngle4f(0.2f, (float) Math.cos(angle), 0, (float) Math.sin(angle)),
                        new Vector3f(0.35f, 1.5f, 0.35f), 4);
            }

            // Bowl tilts and rights itself periodically
            int tiltCycle = tick % 90;
            if (tiltCycle == 0) {
                for (int i = 0; i < bowl.size(); i++) {
                    double[][] bowlOff = {{-0.7, 0}, {0.7, 0}, {0, -0.5}, {0, 0.5}};
                    double[] o = bowlOff[i];
                    bowl.get(i).animateTo(
                            new Vector3f((float) o[0] - 0.35f, 1.6f + (i == 0 ? 0.2f : 0),
                                    (float) o[1] - 0.35f),
                            new AxisAngle4f(0.2f, 0, 0, 1),
                            new Vector3f(0.7f, 0.5f, 0.7f), 10);
                }
            } else if (tiltCycle == 20) {
                for (int i = 0; i < bowl.size(); i++) {
                    double[][] bowlOff = {{-0.7, 0}, {0.7, 0}, {0, -0.5}, {0, 0.5}};
                    double[] o = bowlOff[i];
                    bowl.get(i).animateTo(
                            new Vector3f((float) o[0] - 0.35f, 1.6f, (float) o[1] - 0.35f),
                            new AxisAngle4f(0, 0, 1, 0),
                            new Vector3f(0.7f, 0.5f, 0.7f), 10);
                }
            }

            // Overflow — fire/lava display falls from bowl edge
            if (tick > 0 && tick % 70 == 0) {
                double angle = Math.random() * Math.PI * 2;
                Location dropStart = getCenter().clone().add(
                        Math.cos(angle) * 0.8, 1.9, Math.sin(angle) * 0.8);
                BlockDisplayHandle drop = displayBuilder.spawnBlock(dropStart, Material.MAGMA_BLOCK);
                drop.scale(0.25f, 0.25f, 0.25f).glow(255, 100, 0).interpolation(20, 0);
                drop.animateTo(
                        new Vector3f(-0.125f, -1.9f, -0.125f),
                        new AxisAngle4f((float) (Math.PI), 1, 0, 1),
                        new Vector3f(0.05f, 0.05f, 0.05f), 20);
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_LAVA_POP, 0.9f, 0.4f);
            }

            // Fire bed pulse
            if (tick % 5 == 0 && fireBed != null) {
                float s = 1.0f + (float) Math.sin(tick * 0.18) * 0.15f;
                fireBed.animateTo(new Vector3f(-s / 2f, 1.85f, -s / 2f),
                        new AxisAngle4f((float) (tick * 0.05), 0, 1, 0),
                        new Vector3f(s, 0.3f, s), 5);
            }

            // Constant fire + ember + smoke column
            if (tick % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    Location p = getCenter().clone().add(
                            (Math.random() - 0.5) * 0.8,
                            2.0 + Math.random() * 0.6,
                            (Math.random() - 0.5) * 0.8);
                    w.spawnParticle(Particle.FLAME, p, 1, 0.1, 0.1, 0.1, 0.01);
                    if (i % 2 == 0) w.spawnParticle(Particle.SMALL_FLAME, p, 1, 0.08, 0.05, 0.08, 0.005);
                }
                w.spawnParticle(Particle.LARGE_SMOKE, getCenter().clone().add(0, 3.5, 0),
                        2, 0.5, 0.5, 0.5, 0.02);
                DisplayBuilder.dustParticles(getCenter().clone().add(0, 2.2, 0),
                        2, 0.6, 255, 150, 30, 1.2f);
            }

            if (tick % 30 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            super.onCleanup();
            legs.clear(); bowl.clear(); embers.clear(); fireBed = null;
        }

        @Override public AbstractAttack newInstance() { return new HellfireBrazier(plugin); }
    }
}
