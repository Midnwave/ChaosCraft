package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Hellscape Surges — infernal and volcanic environmental attacks.
 * Heat waves, ember storms, brimstone eruptions, and hellfire effects.
 */
public class HellscapeSurges {

    private HellscapeSurges() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HeatWave(plugin));
        registry.register(new EmberStorm(plugin));
        registry.register(new BrimstoneEruption(plugin));
        registry.register(new InfernalSurge(plugin));
        registry.register(new MagmaRise(plugin));
        registry.register(new HellfirePillar(plugin));
        registry.register(new CinderRain(plugin));
        registry.register(new LavaFlow(plugin));
        registry.register(new NetherBreeze(plugin));
        registry.register(new VolcanicBurst(plugin));
        registry.register(new InfernalGround(plugin));
        registry.register(new FireWhirl(plugin));
        registry.register(new HellPulse(plugin));
    }

    public static class HeatWave extends EnvironmentalAttack {
        public HeatWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_wave", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HeatWave(plugin); }
    }

    public static class EmberStorm extends EnvironmentalAttack {
        public EmberStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new EmberStorm(plugin); }
    }

    public static class BrimstoneEruption extends EnvironmentalAttack {
        public BrimstoneEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_eruption", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new BrimstoneEruption(plugin); }
    }

    public static class InfernalSurge extends EnvironmentalAttack {
        public InfernalSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_surge", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new InfernalSurge(plugin); }
    }

    public static class MagmaRise extends EnvironmentalAttack {
        public MagmaRise(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_rise", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new MagmaRise(plugin); }
    }

    public static class HellfirePillar extends EnvironmentalAttack {
        public HellfirePillar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_pillar", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HellfirePillar(plugin); }
    }

    public static class CinderRain extends EnvironmentalAttack {
        public CinderRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cinder_rain", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new CinderRain(plugin); }
    }

    public static class LavaFlow extends EnvironmentalAttack {
        public LavaFlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_flow", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new LavaFlow(plugin); }
    }

    public static class NetherBreeze extends EnvironmentalAttack {
        public NetherBreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_breeze", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NetherBreeze(plugin); }
    }

    public static class VolcanicBurst extends EnvironmentalAttack {
        public VolcanicBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_burst", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new VolcanicBurst(plugin); }
    }

    public static class InfernalGround extends EnvironmentalAttack {
        public InfernalGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_ground", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new InfernalGround(plugin); }
    }

    public static class FireWhirl extends EnvironmentalAttack {
        public FireWhirl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_whirl", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new FireWhirl(plugin); }
    }

    public static class HellPulse extends EnvironmentalAttack {
        public HellPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hell_pulse", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HellPulse(plugin); }
    }
}
