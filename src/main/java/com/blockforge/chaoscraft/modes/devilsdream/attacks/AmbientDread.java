package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Ambient Dread — atmospheric horror environmental attacks.
 * Whispers, heartbeats, cold presences, and creeping insanity effects.
 */
public class AmbientDread {

    private AmbientDread() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhisperZone(plugin));
        registry.register(new HeartbeatPulse(plugin));
        registry.register(new NightmareEcho(plugin));
        registry.register(new ColdPresence(plugin));
        registry.register(new WatchingFeeling(plugin));
        registry.register(new DreadAura(plugin));
        registry.register(new ShadowFollower(plugin));
        registry.register(new InsanityTick(plugin));
        registry.register(new NightmareBreath(plugin));
        registry.register(new FadingReality(plugin));
        registry.register(new DreamDecay(plugin));
        registry.register(new PhantomTouch(plugin));
        registry.register(new DarkResonance(plugin));
    }

    public static class WhisperZone extends EnvironmentalAttack {
        public WhisperZone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whisper_zone", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new WhisperZone(plugin); }
    }

    public static class HeartbeatPulse extends EnvironmentalAttack {
        public HeartbeatPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heartbeat_pulse", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HeartbeatPulse(plugin); }
    }

    public static class NightmareEcho extends EnvironmentalAttack {
        public NightmareEcho(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_echo", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightmareEcho(plugin); }
    }

    public static class ColdPresence extends EnvironmentalAttack {
        public ColdPresence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_presence", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ColdPresence(plugin); }
    }

    public static class WatchingFeeling extends EnvironmentalAttack {
        public WatchingFeeling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("watching_feeling", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new WatchingFeeling(plugin); }
    }

    public static class DreadAura extends EnvironmentalAttack {
        public DreadAura(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dread_aura", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DreadAura(plugin); }
    }

    public static class ShadowFollower extends EnvironmentalAttack {
        public ShadowFollower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shadow_follower", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ShadowFollower(plugin); }
    }

    public static class InsanityTick extends EnvironmentalAttack {
        public InsanityTick(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("insanity_tick", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new InsanityTick(plugin); }
    }

    public static class NightmareBreath extends EnvironmentalAttack {
        public NightmareBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_breath", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightmareBreath(plugin); }
    }

    public static class FadingReality extends EnvironmentalAttack {
        public FadingReality(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fading_reality", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new FadingReality(plugin); }
    }

    public static class DreamDecay extends EnvironmentalAttack {
        public DreamDecay(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_decay", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DreamDecay(plugin); }
    }

    public static class PhantomTouch extends EnvironmentalAttack {
        public PhantomTouch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_touch", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new PhantomTouch(plugin); }
    }

    public static class DarkResonance extends EnvironmentalAttack {
        public DarkResonance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_resonance", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DarkResonance(plugin); }
    }
}
