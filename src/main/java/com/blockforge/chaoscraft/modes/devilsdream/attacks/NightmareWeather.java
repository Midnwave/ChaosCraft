package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import org.bukkit.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Nightmare Weather — hellish weather and atmospheric environmental attacks.
 * Blood storms, soul hail, brimstone winds, and infernal precipitation.
 */
public class NightmareWeather {

    private NightmareWeather() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BloodStorm(plugin));
        registry.register(new SoulHail(plugin));
        registry.register(new BrimstoneWind(plugin));
        registry.register(new NightFog(plugin));
        registry.register(new EmberShower(plugin));
        registry.register(new AshCloud(plugin));
        registry.register(new ThunderScream(plugin));
        registry.register(new HellWind(plugin));
        registry.register(new SoulBlizzard(plugin));
        registry.register(new CrimsonDew(plugin));
        registry.register(new VolcanicAsh(plugin));
        registry.register(new NightmareTornado(plugin));
        registry.register(new DreamStorm(plugin));
    }

    public static class BloodStorm extends EnvironmentalAttack {
        public BloodStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new BloodStorm(plugin); }
    }

    public static class SoulHail extends EnvironmentalAttack {
        public SoulHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_hail", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new SoulHail(plugin); }
    }

    public static class BrimstoneWind extends EnvironmentalAttack {
        public BrimstoneWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_wind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new BrimstoneWind(plugin); }
    }

    public static class NightFog extends EnvironmentalAttack {
        public NightFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("night_fog", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightFog(plugin); }
    }

    public static class EmberShower extends EnvironmentalAttack {
        public EmberShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_shower", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new EmberShower(plugin); }
    }

    public static class AshCloud extends EnvironmentalAttack {
        public AshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_cloud", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new AshCloud(plugin); }
    }

    public static class ThunderScream extends EnvironmentalAttack {
        public ThunderScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thunder_scream", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new ThunderScream(plugin); }
    }

    public static class HellWind extends EnvironmentalAttack {
        public HellWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hell_wind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new HellWind(plugin); }
    }

    public static class SoulBlizzard extends EnvironmentalAttack {
        public SoulBlizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_blizzard", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new SoulBlizzard(plugin); }
    }

    public static class CrimsonDew extends EnvironmentalAttack {
        public CrimsonDew(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_dew", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new CrimsonDew(plugin); }
    }

    public static class VolcanicAsh extends EnvironmentalAttack {
        public VolcanicAsh(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ash", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new VolcanicAsh(plugin); }
    }

    public static class NightmareTornado extends EnvironmentalAttack {
        public NightmareTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_tornado", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new NightmareTornado(plugin); }
    }

    public static class DreamStorm extends EnvironmentalAttack {
        public DreamStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
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
        @Override public AbstractAttack newInstance() { return new DreamStorm(plugin); }
    }
}
