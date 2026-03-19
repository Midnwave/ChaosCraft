package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Void Intrusions — darkness and void-based environmental attacks.
 * Shadow creep, light drain, void pockets, and abyssal effects.
 */
public class VoidIntrusions {

    private VoidIntrusions() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowCreep(plugin));
        registry.register(new LightDrain(plugin));
        registry.register(new VoidPocket(plugin));
        registry.register(new DarknessSurge(plugin));
        registry.register(new AbyssGaze(plugin));
        registry.register(new ShadowTentacle(plugin));
        registry.register(new VoidBurst(plugin));
        registry.register(new NightEncroach(plugin));
        registry.register(new ObsidianTear(plugin));
        registry.register(new ShadowWave(plugin));
        registry.register(new VoidWhisper(plugin));
        registry.register(new DarkPrison(plugin));
        registry.register(new AbyssalRift(plugin));
    }

    public static class ShadowCreep extends EnvironmentalAttack {
        public ShadowCreep(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_creep", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ShadowCreep(plugin); }
    }

    public static class LightDrain extends EnvironmentalAttack {
        public LightDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("light_drain", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new LightDrain(plugin); }
    }

    public static class VoidPocket extends EnvironmentalAttack {
        public VoidPocket(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_pocket", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new VoidPocket(plugin); }
    }

    public static class DarknessSurge extends EnvironmentalAttack {
        public DarknessSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("darkness_surge", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DarknessSurge(plugin); }
    }

    public static class AbyssGaze extends EnvironmentalAttack {
        public AbyssGaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyss_gaze", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new AbyssGaze(plugin); }
    }

    public static class ShadowTentacle extends EnvironmentalAttack {
        public ShadowTentacle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_tentacle", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ShadowTentacle(plugin); }
    }

    public static class VoidBurst extends EnvironmentalAttack {
        public VoidBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_burst", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new VoidBurst(plugin); }
    }

    public static class NightEncroach extends EnvironmentalAttack {
        public NightEncroach(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("night_encroach", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightEncroach(plugin); }
    }

    public static class ObsidianTear extends EnvironmentalAttack {
        public ObsidianTear(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_tear", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ObsidianTear(plugin); }
    }

    public static class ShadowWave extends EnvironmentalAttack {
        public ShadowWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_wave", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ShadowWave(plugin); }
    }

    public static class VoidWhisper extends EnvironmentalAttack {
        public VoidWhisper(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_whisper", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new VoidWhisper(plugin); }
    }

    public static class DarkPrison extends EnvironmentalAttack {
        public DarkPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_prison", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DarkPrison(plugin); }
    }

    public static class AbyssalRift extends EnvironmentalAttack {
        public AbyssalRift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_rift", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new AbyssalRift(plugin); }
    }
}
