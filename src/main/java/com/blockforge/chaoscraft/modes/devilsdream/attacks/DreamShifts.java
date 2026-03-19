package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Dream Shifts — reality-warping environmental attacks.
 * Gravity changes, time stutters, spatial distortions, and perception manipulation.
 */
public class DreamShifts {

    private DreamShifts() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GravityPulse(plugin));
        registry.register(new TimeStutter(plugin));
        registry.register(new SpatialCompression(plugin));
        registry.register(new DirectionScramble(plugin));
        registry.register(new DepthDistortion(plugin));
        registry.register(new EchoStep(plugin));
        registry.register(new MirrorWorld(plugin));
        registry.register(new DreamRewind(plugin));
        registry.register(new SizeShift(plugin));
        registry.register(new ParallelSelf(plugin));
        registry.register(new PhaseFlicker(plugin));
        registry.register(new MemoryBleed(plugin));
        registry.register(new LoopBreak(plugin));
    }

    public static class GravityPulse extends EnvironmentalAttack {
        public GravityPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("gravity_pulse", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GravityPulse(plugin); }
    }

    public static class TimeStutter extends EnvironmentalAttack {
        public TimeStutter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("time_stutter", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new TimeStutter(plugin); }
    }

    public static class SpatialCompression extends EnvironmentalAttack {
        public SpatialCompression(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spatial_compression", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SpatialCompression(plugin); }
    }

    public static class DirectionScramble extends EnvironmentalAttack {
        public DirectionScramble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("direction_scramble", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DirectionScramble(plugin); }
    }

    public static class DepthDistortion extends EnvironmentalAttack {
        public DepthDistortion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("depth_distortion", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DepthDistortion(plugin); }
    }

    public static class EchoStep extends EnvironmentalAttack {
        public EchoStep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("echo_step", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EchoStep(plugin); }
    }

    public static class MirrorWorld extends EnvironmentalAttack {
        public MirrorWorld(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("mirror_world", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MirrorWorld(plugin); }
    }

    public static class DreamRewind extends EnvironmentalAttack {
        public DreamRewind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_rewind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DreamRewind(plugin); }
    }

    public static class SizeShift extends EnvironmentalAttack {
        public SizeShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("size_shift", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SizeShift(plugin); }
    }

    public static class ParallelSelf extends EnvironmentalAttack {
        public ParallelSelf(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("parallel_self", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ParallelSelf(plugin); }
    }

    public static class PhaseFlicker extends EnvironmentalAttack {
        public PhaseFlicker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phase_flicker", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new PhaseFlicker(plugin); }
    }

    public static class MemoryBleed extends EnvironmentalAttack {
        public MemoryBleed(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("memory_bleed", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new MemoryBleed(plugin); }
    }

    public static class LoopBreak extends EnvironmentalAttack {
        public LoopBreak(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("loop_break", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(200);
            config.setCooldownTicks(250);
        }
        @Override protected void onSpawn(Location center) { /* TODO */ }
        @Override protected void onTick(int ticksAlive) { /* TODO */ }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new LoopBreak(plugin); }
    }
}
