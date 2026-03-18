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
 * Attacks #81-90 — Stream Reversal, Combos, Territory Control
 *
 * Combo chains with 0.5s between parts. Stream weaponization peaks.
 * NO status effects — damage only.
 */
public final class CalamitasEternityD {

    private CalamitasEternityD() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ReversedStream(plugin));
        registry.register(new StackedConvergenceBeam(plugin));
        registry.register(new StreamAnchorTrap(plugin));
        registry.register(new CrimsonStreamWall(plugin));
        registry.register(new ComboRisingStormDrop(plugin));
        registry.register(new ComboGroundScoreRain(plugin));
        registry.register(new ComboHomingReleaseWall(plugin));
        registry.register(new ComboScissorsFan(plugin));
        registry.register(new ComboCorruptionPulseFan(plugin));
        registry.register(new ComboPincerBeamsRain(plugin));
    }

    // ================================================================
    // 81. REVERSED STREAM — Direction reversal mid-flow
    // ================================================================
    public static class ReversedStream extends BossAttack {
        private final List<BlockDisplayHandle> revHandles = new ArrayList<>();
        private boolean active = false;
        private int activeStart = 0;
        private boolean reversed = false;

        public ReversedStream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_reversed_stream", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(2.0);
            config.setDurationTicks(300); config.setCooldownTicks(400); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int seg = 0; seg < 10; seg++) {
                BlockDisplayHandle s = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, -10 + seg * 2), Material.RED_STAINED_GLASS);
                s.scale(2.0f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
                revHandles.add(s); spawnedEntities.add(s.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !active) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, 5.0);
            } else if (ticksAlive == 20 && !active) { active = true; activeStart = ticksAlive; }
            else if (active && !reversed && ticksAlive - activeStart < 80) {
                float drift = (ticksAlive - activeStart) * 0.2f;
                for (int i = 0; i < revHandles.size(); i++) {
                    Location cur = revHandles.get(i).entity().getLocation();
                    revHandles.get(i).entity().teleport(new Location(cur.getWorld(), cur.getX(), cur.getY(), cur.getZ() + 0.2));
                }
            } else if (active && !reversed && ticksAlive - activeStart == 80) {
                reversed = true;
                DisplayBuilder.dustParticles(center.clone().add(0, 20, 0), 15, 5.0, 255, 220, 100, 1.5f);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 0.7f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
            } else if (reversed && ticksAlive - activeStart < 160) {
                for (int i = 0; i < revHandles.size(); i++) {
                    Location cur = revHandles.get(i).entity().getLocation();
                    revHandles.get(i).entity().teleport(new Location(cur.getWorld(), cur.getX(), cur.getY(), cur.getZ() - 0.2));
                }
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ReversedStream(plugin); }
    }

    // ================================================================
    // 82. STACKED CONVERGENCE BEAM — Two streams converge, triple damage
    // ================================================================
    public static class StackedConvergenceBeam extends BossAttack {
        private boolean active = false;
        private int activeStart = 0;

        public StackedConvergenceBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_stacked_convergence_beam", AttackType.BOSS, 5), "calamitas");
            config.setDamage(28.0); config.setDamageRadius(4.0);
            config.setDurationTicks(300); config.setCooldownTicks(600); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.5f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 30 && !active) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 5), 15, 5.0);
            } else if (ticksAlive == 30 && !active) {
                active = true; activeStart = ticksAlive;
                BlockDisplayHandle beam = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 5), Material.MAGMA_BLOCK);
                beam.scale(4.0f, 25.0f, 4.0f).glow(255, 60, 0).interpolation(2, 0);
                spawnedEntities.add(beam.entity());
                DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.3f, 0.8f);
            } else if (active && ticksAlive - activeStart < 100) {
                if (ticksAlive % 4 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 10, 5), 20, 4.0);
            } else if (active && ticksAlive - activeStart == 100) {
                triggerImpactDamage(center.clone().add(0, 0.5, 5));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StackedConvergenceBeam(plugin); }
    }

    // ================================================================
    // 83. STREAM ANCHOR TRAP — Flanking streams converge on player
    // ================================================================
    public static class StreamAnchorTrap extends BossAttack {
        private final List<BlockDisplayHandle> trapHandles = new ArrayList<>();
        private boolean closing = false;
        private int closeStart = 0;

        public StreamAnchorTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_stream_anchor_trap", AttackType.BOSS, 5), "calamitas");
            config.setDamage(16.0); config.setDamageRadius(2.0);
            config.setDurationTicks(300); config.setCooldownTicks(560); config.setTicksBetweenDamage(20);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int side = 0; side < 2; side++) {
                double xOff = (side == 0) ? -3 : 3;
                for (int seg = 0; seg < 8; seg++) {
                    BlockDisplayHandle s = displayBuilder.spawnBlock(
                        center.clone().add(xOff, 20, -8 + seg * 2), Material.RED_STAINED_GLASS);
                    s.scale(1.5f, 3.0f, 2.0f).glow(180, 0, 0).interpolation(2, 0);
                    trapHandles.add(s); spawnedEntities.add(s.entity());
                }
            }
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !closing) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 10, 4.0);
            } else if (ticksAlive == 20 && !closing) { closing = true; closeStart = ticksAlive; }
            else if (closing && ticksAlive - closeStart < 80) {
                // Streams hold 4 seconds then converge
                if (ticksAlive - closeStart >= 60) {
                    float progress = (ticksAlive - closeStart - 60) / 20.0f;
                    for (int side = 0; side < 2; side++) {
                        double xStart = (side == 0) ? -3 : 3;
                        double x = xStart * (1 - progress);
                        for (int seg = 0; seg < 8; seg++) {
                            int idx = side * 8 + seg;
                            if (idx < trapHandles.size())
                                trapHandles.get(idx).entity().teleport(
                                    center.clone().add(x, 20, -8 + seg * 2));
                        }
                    }
                }
            } else if (closing && ticksAlive - closeStart == 80) {
                triggerImpactDamage(center.clone().add(0, 20, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 20, 0), 25, 4.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new StreamAnchorTrap(plugin); }
    }

    // ================================================================
    // 84. CRIMSON STREAM WALL — Vertical barrier crossing player's path
    // ================================================================
    public static class CrimsonStreamWall extends BossAttack {
        private final List<BlockDisplayHandle> wallHandles = new ArrayList<>();
        private boolean advancing = false;
        private int advStart = 0;

        public CrimsonStreamWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_crimson_stream_wall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(24.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(440); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            for (int y = 0; y < 5; y++) {
                BlockDisplayHandle panel = displayBuilder.spawnBlock(
                    center.clone().add(0, y, 8), Material.RED_STAINED_GLASS);
                panel.scale(10.0f, 1.0f, 0.3f).glow(180, 0, 0).interpolation(2, 0);
                wallHandles.add(panel); spawnedEntities.add(panel.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 1.0f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive < 20 && !advancing) {
                if (ticksAlive % 5 == 0) DisplayBuilder.crimsonDust(center.clone().add(0, 2, 8), 10, 5.0);
            } else if (ticksAlive == 20 && !advancing) { advancing = true; advStart = ticksAlive; }
            else if (advancing && ticksAlive - advStart < 120) {
                float advance = (ticksAlive - advStart) * 0.05f;
                for (int y = 0; y < 5 && y < wallHandles.size(); y++) {
                    wallHandles.get(y).entity().teleport(center.clone().add(0, y, 8 - advance));
                }
            } else if (advancing && ticksAlive - advStart == 120) {
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.8f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new CrimsonStreamWall(plugin); }
    }

    // ================================================================
    // 85-90: COMBO CHAINS — Two-part attacks with 0.5s gap
    // ================================================================

    public static class ComboRisingStormDrop extends BossAttack {
        private final List<BlockDisplayHandle> comboHandles = new ArrayList<>();
        private int phase = 0;

        public ComboRisingStormDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_rising_storm_drop", AttackType.BOSS, 5), "calamitas");
            config.setDamage(22.0); config.setDamageRadius(2.0);
            config.setDurationTicks(200); config.setCooldownTicks(440); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                for (int i = 0; i < 3; i++) {
                    BlockDisplayHandle t = displayBuilder.spawnBlock(center.clone().add((i-1)*2, 20, 0), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    comboHandles.add(t); spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 1.2f);
            } else if (phase == 1 && ticksAlive < 50) {
                float rise = (ticksAlive - 20) * 0.5f;
                for (int i = 0; i < 3 && i < comboHandles.size(); i++)
                    comboHandles.get(i).entity().teleport(center.clone().add((i-1)*2, 20 + rise, 0));
            } else if (ticksAlive == 60 && phase == 1) {
                phase = 2;
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.7f);
            } else if (phase == 2 && ticksAlive < 90) {
                float drop = (ticksAlive - 60) * 1.2f;
                for (int i = 0; i < 3 && i < comboHandles.size(); i++)
                    comboHandles.get(i).entity().teleport(center.clone().add((i-1)*2, 35 - drop, 0));
            } else if (phase == 2 && ticksAlive == 90) {
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, 0), 15, 3.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboRisingStormDrop(plugin); }
    }

    public static class ComboGroundScoreRain extends BossAttack {
        private int phase = 0;

        public ComboGroundScoreRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_ground_score_rain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(520); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 0.8f, 0.9f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                for (int seg = 0; seg < 8; seg++) {
                    BlockDisplayHandle b = displayBuilder.spawnBlock(
                        center.clone().add(0, 0.1, seg * 2), Material.ORANGE_STAINED_GLASS);
                    b.scale(2.0f, 0.5f, 2.0f).glow(255, 60, 0).interpolation(1, 0);
                    spawnedEntities.add(b.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_FALL, 1.2f, 0.8f);
            } else if (ticksAlive == 50 && phase == 1) {
                phase = 2;
                for (int i = 0; i < 10; i++) {
                    double offX = (Math.random() - 0.5) * 10;
                    double offZ = -5 + (Math.random() - 0.5) * 10;
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(offX, 30, offZ), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.1f, 0.1f, 0.8f).glow(220, 220, 255).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.9f);
            } else if (phase == 2 && ticksAlive == 80) {
                triggerImpactDamage(center.clone().add(0, 0.5, -5));
                DisplayBuilder.crimsonDust(center.clone().add(0, 0.5, -5), 15, 5.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboGroundScoreRain(plugin); }
    }

    public static class ComboHomingReleaseWall extends BossAttack {
        private int phase = 0;

        public ComboHomingReleaseWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_homing_release_wall", AttackType.BOSS, 5), "calamitas");
            config.setDamage(14.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(480); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.9f, 1.0f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2 / 4) * i;
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 3, 20, Math.sin(angle) * 3), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(200, 0, 200).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
            } else if (ticksAlive == 50 && phase == 1) {
                phase = 2;
                for (int y = 0; y < 5; y++) {
                    BlockDisplayHandle wall = displayBuilder.spawnBlock(
                        center.clone().add(0, y, -10), Material.RED_STAINED_GLASS);
                    wall.scale(10.0f, 1.0f, 0.3f).glow(180, 0, 0).interpolation(1, 0);
                    spawnedEntities.add(wall.entity());
                }
                DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.8f);
            } else if (phase == 2 && ticksAlive == 80) {
                triggerImpactDamage(center.clone().add(0, 0.5, -5));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboHomingReleaseWall(plugin); }
    }

    public static class ComboScissorsFan extends BossAttack {
        private int phase = 0;

        public ComboScissorsFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_scissors_fan", AttackType.BOSS, 5), "calamitas");
            config.setDamage(18.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(560); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                for (int beam = 0; beam < 2; beam++) {
                    for (int seg = 0; seg < 6; seg++) {
                        double x = (beam == 0 ? -12 : 12);
                        BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(x, 20, seg * 2), Material.ORANGE_STAINED_GLASS);
                        b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                        spawnedEntities.add(b.entity());
                    }
                }
            } else if (ticksAlive == 60 && phase == 1) {
                phase = 2;
                for (int i = 0; i < 12; i++) {
                    double angle = Math.toRadians(-90 + (180.0 / 11) * i);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(Math.sin(angle) * 2, 20, Math.cos(angle) * 2), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.1f, 0.1f, 0.9f).glow(220, 220, 255).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.5f, 1.0f);
            } else if (phase == 2 && ticksAlive == 90) {
                triggerImpactDamage(center.clone().add(0, 0.5, 60));
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboScissorsFan(plugin); }
    }

    public static class ComboCorruptionPulseFan extends BossAttack {
        private int phase = 0;

        public ComboCorruptionPulseFan(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_corruption_pulse_fan", AttackType.BOSS, 5), "calamitas");
            config.setDamage(26.0); config.setDamageRadius(8.0);
            config.setDurationTicks(200); config.setCooldownTicks(400); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.5f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                for (int i = 0; i < 10; i++) {
                    double angle = (Math.PI * 2 / 10) * i;
                    BlockDisplayHandle ring = displayBuilder.spawnBlock(
                        center.clone().add(Math.cos(angle) * 8, 0.1, Math.sin(angle) * 8), Material.PURPLE_STAINED_GLASS);
                    ring.scale(2.0f, 1.0f, 2.0f).glow(120, 0, 120).interpolation(1, 0);
                    spawnedEntities.add(ring.entity());
                }
                triggerImpactDamage(center.clone().add(0, 0.5, 0));
                DisplayBuilder.crimsonDust(center.clone().add(0, 1, 0), 25, 8.0);
            } else if (ticksAlive == 30 && phase == 1) {
                phase = 2;
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(45 * i);
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(Math.sin(angle), 1, Math.cos(angle)), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.12f, 0.12f, 1.0f).glow(220, 220, 255).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 1.0f);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboCorruptionPulseFan(plugin); }
    }

    public static class ComboPincerBeamsRain extends BossAttack {
        private int phase = 0;

        public ComboPincerBeamsRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("calamitas_combo_pincer_beams_rain", AttackType.BOSS, 5), "calamitas");
            config.setDamage(20.0); config.setDamageRadius(2.0);
            config.setDurationTicks(250); config.setCooldownTicks(600); config.setTicksBetweenDamage(10);
        }

        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_GUARDIAN_ATTACK, 1.0f, 0.7f); }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            if (ticksAlive == 20 && phase == 0) {
                phase = 1;
                // Two perpendicular beams
                for (int dir = 0; dir < 2; dir++) {
                    for (int seg = 0; seg < 6; seg++) {
                        double x = (dir == 0) ? seg * 3 : 0;
                        double z = (dir == 1) ? seg * 3 : 0;
                        BlockDisplayHandle b = displayBuilder.spawnBlock(
                            center.clone().add(x, 20, z), Material.ORANGE_STAINED_GLASS);
                        b.scale(2.0f, 2.0f, 2.0f).glow(255, 80, 0).interpolation(1, 0);
                        spawnedEntities.add(b.entity());
                    }
                }
            } else if (ticksAlive == 60 && phase == 1) {
                phase = 2;
                // Rain on safe quadrants
                for (int i = 0; i < 8; i++) {
                    BlockDisplayHandle t = displayBuilder.spawnBlock(
                        center.clone().add(-5 + Math.random() * 4, 30, -5 + Math.random() * 4), Material.LIGHT_GRAY_GLAZED_TERRACOTTA);
                    t.scale(0.1f, 0.1f, 0.8f).glow(220, 220, 255).interpolation(1, 0);
                    spawnedEntities.add(t.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.9f);
            } else if (phase == 2 && ticksAlive == 90) {
                triggerImpactDamage(center.clone().add(-3, 0.5, -3));
                DisplayBuilder.crimsonDust(center.clone().add(-3, 0.5, -3), 15, 4.0);
            }
        }

        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new ComboPincerBeamsRain(plugin); }
    }
}
