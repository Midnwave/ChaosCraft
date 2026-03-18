package com.blockforge.chaoscraft.modes.calamity.attacks.phase5.boss;

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
 * Supreme Calamitas — Phase 4E Boss (Boss 5, Final Boss)
 * Phase 2: "The Weight of Eternity" — HP 74% to 50%
 * Attacks #71-80 — Post-Summon Combos, Beam Sweeps, Sky Streams
 *
 * Brothers are active. Beam weaponization begins.
 * NO status effects — damage only.
 */
public final class CalamitasEternityC {

    private CalamitasEternityC() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new PostSummonTwelveFan(plugin));
        registry.register(new PostSummonSoulFireEruption(plugin));
        registry.register(new PostSummonOverheadRain(plugin));
        registry.register(new SingleSweepingBeam(plugin));
        registry.register(new DualBeamScissors(plugin));
        registry.register(new OverheadSpinningBeam(plugin));
        registry.register(new PlayerTrackingBeam(plugin));
        registry.register(new SplitBeamBurst(plugin));
        registry.register(new BrimstoneBeamGroundScore(plugin));
        registry.register(new DualStreamSummon(plugin));
    }

    // ================================================================
    // 71. POST-SUMMON COMBO: TWELVE-FAN IMMEDIATE
    // ================================================================
    public static class PostSummonTwelveFan extends BossAttack {
        private final List<BlockDisplayHandle> fanHandles = new ArrayList<>();
        private boolean fired = false;
        private int fireTick = 0;

        public PostSummonTwelveFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_post_summon_twelve_fan", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); config.setDamageRadius(2.0);
            config.setDurationTicks(150); config.setCooldownTicks(999999); config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.1f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 10 && !fired) {
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, 3.0);
            } else if (ticksAlive == 10 && !fired) {
                fired = true; fireTick = ticksAlive;
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(-75 + (150.0 / 11) * i);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.1f, 0.1f, 0.9f).glow(220, 220, 255).interpolation(1, 0);
                    fanHandles.add(t); spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            } else if (fired && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 2.0f;
                for (int i = 0; i < 12 && i < fanHandles.size(); i++) {
                    double angle = Math.toRadians(-75 + (150.0 / 11) * i);
                    fanHandles.get(i).entity().teleport(
                        center.clone().add(Math.sin(angle) * (2 + dist), 20, Math.cos(angle) * (2 + dist)));
                }
            } else if (fired && ticksAlive - fireTick == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 80));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PostSummonTwelveFan(plugin); }
    }

    // ================================================================
    // 72. POST-SUMMON COMBO: SOUL FIRE ERUPTION at arena center
    // ================================================================
    public static class PostSummonSoulFireEruption extends BossAttack {
        private boolean erupted = false;

        public PostSummonSoulFireEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_post_summon_soul_fire_eruption", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); config.setDamageRadius(10.0);
            config.setDurationTicks(200); config.setCooldownTicks(999999); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 10) DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 10, 5.0);
            else if (ticksAlive == 10 && !erupted) {
                erupted = true;
                for (int y = 0; y < 4; y++) {
                    BlockDisplayHandle col = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.ORANGE_STAINED_GLASS);
                    col.scale(10.0f, 1.0f, 10.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(col.entity());
                }
                triggerImpactDamage(center.clone().add(0, 2, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 2, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.5f, 0.6f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PostSummonSoulFireEruption(plugin); }
    }

    // ================================================================
    // 73. POST-SUMMON COMBO: OVERHEAD RAIN — 8 concentrated drops
    // ================================================================
    public static class PostSummonOverheadRain extends BossAttack {
        private final List<BlockDisplayHandle> rainHandles = new ArrayList<>();
        private boolean dropping = false;

        public PostSummonOverheadRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_post_summon_overhead_rain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); config.setDamageRadius(2.0);
            config.setDurationTicks(150); config.setCooldownTicks(999999); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int i = 0; i < 8; i++) {
                double offX = (Math.random() - 0.5) * 8;
                double offZ = (Math.random() - 0.5) * 8;
                BlockDisplayHandle shadow = displayBuilder.spawnBlock(
                    center.clone().add(offX, 30, offZ), Material.BLACK_STAINED_GLASS);
                shadow.scale(0.3f, 0.3f, 0.3f).glow(60, 0, 0).interpolation(1, 0);
                rainHandles.add(shadow); spawnedEntities.add(shadow.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 10) DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 10, 4.0);
            else if (ticksAlive == 10 && !dropping) {
                dropping = true;
            } else if (dropping && ticksAlive < 40) {
                float drop = (ticksAlive - 10) * 1.2f;
                for (int i = 0; i < 8 && i < rainHandles.size(); i++) {
                    Location cur = rainHandles.get(i).entity().getLocation();
                    double y = 30 - drop;
                    rainHandles.get(i).entity().teleport(new Location(cur.getWorld(), cur.getX(), Math.max(0.1, y), cur.getZ()));
                }
            } else if (dropping && ticksAlive == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 20, 4.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PostSummonOverheadRain(plugin); }
    }

    // ================================================================
    // 74. SINGLE SWEEPING BEAM — 90-degree sweep, 3 seconds
    // ================================================================
    public static class SingleSweepingBeam extends BossAttack {
        private final List<BlockDisplayHandle> beamHandles = new ArrayList<>();
        private boolean sweeping = false;
        private int sweepStart = 0;

        public SingleSweepingBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_single_sweeping_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(400); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle palm = displayBuilder.spawnBlock(center.clone().add(1, 20, 0), Material.MAGMA_BLOCK);
            palm.scale(0.5f, 0.5f, 0.5f).glow(255, 80, 0).interpolation(2, 0);
            beamHandles.add(palm); spawnedEntities.add(palm.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !sweeping) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(1, 20, 0), 8, 1.0);
            } else if (ticksAlive == 20 && !sweeping) {
                sweeping = true; sweepStart = ticksAlive;
                for (int seg = 0; seg < 10; seg++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(1, 20, 2 + seg * 2), Material.ORANGE_STAINED_GLASS);
                    b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                    beamHandles.add(b); spawnedEntities.add(b.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f);
            } else if (sweeping && ticksAlive - sweepStart < 60) {
                double sweepAngle = (ticksAlive - sweepStart) / 60.0 * Math.PI / 2;
                for (int seg = 1; seg <= 10 && seg < beamHandles.size(); seg++) {
                    double dist = 2 + (seg - 1) * 2;
                    beamHandles.get(seg).entity().teleport(
                        center.clone().add(1 + Math.sin(sweepAngle) * dist, 20, Math.cos(sweepAngle) * dist));
                }
                if (ticksAlive % 6 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 10), 10, 3.0);
            } else if (sweeping && ticksAlive - sweepStart == 60) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_DEATH, 0.6f, 1.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SingleSweepingBeam(plugin); }
    }

    // ================================================================
    // 75. DUAL BEAM SCISSORS — Two converging beams from opposite sides
    // ================================================================
    public static class DualBeamScissors extends BossAttack {
        private final List<BlockDisplayHandle> scissorHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStart = 0;

        public DualBeamScissors(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_dual_beam_scissors", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(500); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int arm = 0; arm < 2; arm++) {
                double xOff = (arm == 0) ? -1.5 : 1.5;
                BlockDisplayHandle hand = displayBuilder.spawnBlock(
                    center.clone().add(xOff, 20, 0), Material.MAGMA_BLOCK);
                hand.scale(0.5f, 0.5f, 0.5f).glow(255, 80, 0).interpolation(2, 0);
                scissorHandles.add(hand); spawnedEntities.add(hand.entity());
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.1f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !active) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, 2.0);
            } else if (ticksAlive == 20 && !active) {
                active = true; activeStart = ticksAlive;
                // Two beams — left and right
                for (int beam = 0; beam < 2; beam++) {
                    for (int seg = 0; seg < 8; seg++) {
                        double xStart = (beam == 0) ? -15 : 15;
                        BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(xStart, 20, seg * 2), Material.ORANGE_STAINED_GLASS);
                        b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                        scissorHandles.add(b); spawnedEntities.add(b.entity());
                    }
                }
            } else if (active && ticksAlive - activeStart < 60) {
                float progress = (ticksAlive - activeStart) / 60.0f;
                for (int beam = 0; beam < 2; beam++) {
                    for (int seg = 0; seg < 8; seg++) {
                        int idx = 2 + beam * 8 + seg;
                        if (idx < scissorHandles.size()) {
                            double xStart = (beam == 0) ? -15 : 15;
                            double x = xStart * (1 - progress);
                            scissorHandles.get(idx).entity().teleport(
                                center.clone().add(x, 20, seg * 2));
                        }
                    }
                }
            } else if (active && ticksAlive - activeStart == 60) {
                triggerImpactDamage(center.clone().add(0, 20, 8));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 8), 20, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.3f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DualBeamScissors(plugin); }
    }

    // ================================================================
    // 76. OVERHEAD SPINNING BEAM — 360-degree rotating beam from above
    // ================================================================
    public static class OverheadSpinningBeam extends BossAttack {
        private final List<BlockDisplayHandle> spinHandles = new ArrayList<>();
        private boolean spinning = false;
        private int spinStart = 0;

        public OverheadSpinningBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_overhead_spinning_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); config.setDamageRadius(2.0);
            config.setDurationTicks(350); config.setCooldownTicks(600); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle core = displayBuilder.spawnBlock(center.clone().add(0, 25, 0), Material.RED_STAINED_GLASS);
            core.scale(2.0f, 2.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
            spinHandles.add(core); spawnedEntities.add(core.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !spinning) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 10, 3.0);
            } else if (ticksAlive == 20 && !spinning) {
                spinning = true; spinStart = ticksAlive;
                for (int seg = 0; seg < 8; seg++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.5, 2 + seg * 2), Material.ORANGE_STAINED_GLASS);
                    b.scale(2.0f, 25.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                    spinHandles.add(b); spawnedEntities.add(b.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.9f, 0.65f);
            } else if (spinning && ticksAlive - spinStart < 100) {
                double angle = (ticksAlive - spinStart) / 100.0 * Math.PI * 2;
                for (int seg = 1; seg <= 8 && seg < spinHandles.size(); seg++) {
                    double dist = 2 + (seg - 1) * 2;
                    spinHandles.get(seg).entity().teleport(
                        center.clone().add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist));
                }
                if (ticksAlive % 8 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 10, 8.0);
            } else if (spinning && ticksAlive - spinStart == 100) {
                triggerImpactDamage(center.clone().add(0, 1, 0));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new OverheadSpinningBeam(plugin); }
    }

    // ================================================================
    // 77. PLAYER-TRACKING BEAM — 4 seconds tracking with 0.3s lag
    // ================================================================
    public static class PlayerTrackingBeam extends BossAttack {
        private final List<BlockDisplayHandle> trackHandles = new ArrayList<>();
        private boolean tracking = false;
        private int trackStart = 0;

        public PlayerTrackingBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_player_tracking_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(440); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle reticle = displayBuilder.spawnBlock(
                center.clone().add(0, 0.05, 5), Material.GLOWSTONE);
            reticle.scale(2.0f, 0.06f, 2.0f).glow(255, 0, 0).interpolation(2, 0);
            trackHandles.add(reticle); spawnedEntities.add(reticle.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.9f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !tracking) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 5), 8, 2.0);
            } else if (ticksAlive == 20 && !tracking) {
                tracking = true; trackStart = ticksAlive;
                for (int seg = 0; seg < 8; seg++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(0, 20, seg * 2), Material.ORANGE_STAINED_GLASS);
                    b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                    trackHandles.add(b); spawnedEntities.add(b.entity());
                }
            } else if (tracking && ticksAlive - trackStart < 80) {
                double trackAngle = Math.sin((ticksAlive - trackStart) * 0.05) * 0.5;
                for (int seg = 1; seg <= 8 && seg < trackHandles.size(); seg++) {
                    double dist = seg * 2;
                    trackHandles.get(seg).entity().teleport(
                        center.clone().add(Math.sin(trackAngle) * dist, 20, Math.cos(trackAngle) * dist));
                }
                if (ticksAlive % 8 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 8), 8, 2.0);
            } else if (tracking && ticksAlive - trackStart == 80) {
                DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_HURT, 0.6f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new PlayerTrackingBeam(plugin); }
    }

    // ================================================================
    // 78. SPLIT BEAM BURST — 8 short beams in compass directions
    // ================================================================
    public static class SplitBeamBurst extends BossAttack {
        private boolean fired = false;

        public SplitBeamBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_split_beam_burst", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); config.setDamageRadius(2.0);
            config.setDurationTicks(120); config.setCooldownTicks(360); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !fired) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 12, 3.0);
            } else if (ticksAlive == 20 && !fired) {
                fired = true;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45 * i);
                    for (int seg = 0; seg < 4; seg++) {
                        double dist = 2 + seg * 2;
                        BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(Math.cos(angle) * dist, 20, Math.sin(angle) * dist),
                            Material.ORANGE_STAINED_GLASS);
                        b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                        spawnedEntities.add(b.entity());
                    }
                }
                triggerImpactDamage(center.clone().add(0, 20, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.9f, 1.0f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.3f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SplitBeamBurst(plugin); }
    }

    // ================================================================
    // 79. BRIMSTONE BEAM GROUND SCORE — Ground-level beam line, 20 blocks
    // ================================================================
    public static class BrimstoneBeamGroundScore extends BossAttack {
        private final List<BlockDisplayHandle> scoreHandles = new ArrayList<>();
        private boolean scoring = false;
        private int scoreStart = 0;

        public BrimstoneBeamGroundScore(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_brimstone_beam_ground_score", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(420); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            BlockDisplayHandle aimPt = displayBuilder.spawnBlock(center.clone().add(0, 0.05, 3), Material.MAGMA_BLOCK);
            aimPt.scale(1.0f, 0.06f, 1.0f).glow(180, 0, 0).interpolation(2, 0);
            scoreHandles.add(aimPt); spawnedEntities.add(aimPt.entity());
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.9f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !scoring) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 3), 6, 1.0);
            } else if (ticksAlive == 20 && !scoring) {
                scoring = true; scoreStart = ticksAlive;
            } else if (scoring && ticksAlive - scoreStart < 40) {
                float progress = (ticksAlive - scoreStart) / 40.0f;
                double dist = progress * 20;
                Location scorePt = center.clone().add(0, 0.1, 3 + dist);
                BlockDisplayHandle line = displayBuilder.spawnBlock(scorePt, Material.ORANGE_STAINED_GLASS);
                line.scale(2.0f, 0.5f, 1.0f).glow(255, 60, 0).interpolation(1, 0);
                spawnedEntities.add(line.entity());
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(scorePt, 8, 1.5);
                DisplayBuilder.playSound(scorePt, Sound.BLOCK_SOUL_SAND_FALL, 0.8f, 0.8f);
            } else if (scoring && ticksAlive - scoreStart == 40) {
                triggerImpactDamage(center.clone().add(0, 0.5, 13));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneBeamGroundScore(plugin); }
    }

    // ================================================================
    // 80. DUAL STREAM SUMMON — Two parallel crimson streams, narrow gap
    // ================================================================
    public static class DualStreamSummon extends BossAttack {
        private final List<BlockDisplayHandle> streamHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStart = 0;

        public DualStreamSummon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_dual_stream_summon", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); config.setDamageRadius(2.5);
            config.setDurationTicks(300); config.setCooldownTicks(500); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.1f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !active) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 25, 0), 12, 5.0);
            } else if (ticksAlive == 20 && !active) {
                active = true; activeStart = ticksAlive;
                // Two streams 5 blocks apart
                for (int stream = 0; stream < 2; stream++) {
                    double xOff = (stream == 0) ? -2.5 : 2.5;
                    for (int seg = 0; seg < 12; seg++) {
                        BlockDisplayHandle s = displayBuilder.spawnBlock(
                            center.clone().add(xOff, 20, -12 + seg * 2), Material.RED_STAINED_GLASS);
                        s.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
                        streamHandles.add(s); spawnedEntities.add(s.entity());
                    }
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.8f);
            } else if (active && ticksAlive - activeStart < 160) {
                float drift = (ticksAlive - activeStart) * 0.15f;
                for (int i = 0; i < streamHandles.size(); i++) {
                    Location cur = streamHandles.get(i).entity().getLocation();
                    streamHandles.get(i).entity().teleport(
                        new Location(cur.getWorld(), cur.getX(), cur.getY(), cur.getZ() + 0.15));
                }
                if (ticksAlive % 10 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, drift), 10, 3.0);
            } else if (active && ticksAlive - activeStart == 160) {
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DualStreamSummon(plugin); }
    }
}
