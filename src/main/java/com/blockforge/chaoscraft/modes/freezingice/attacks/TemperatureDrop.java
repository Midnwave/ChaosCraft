package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class TemperatureDrop {
    private TemperatureDrop() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ArcticChill(plugin));
        registry.register(new PermafrostPulse(plugin));
        registry.register(new FrostBite(plugin));
        registry.register(new HypothermiaWave(plugin));
        registry.register(new DeepFreeze(plugin));
        registry.register(new ColdSnap(plugin));
        registry.register(new ShiveringFit(plugin));
        registry.register(new BreathCloud(plugin));
        registry.register(new BodyHeatDrain(plugin));
        registry.register(new WindChill(plugin));
        registry.register(new FrostCreepEnv(plugin));
        registry.register(new CoreFreeze(plugin));
        registry.register(new ThawFakeout(plugin));
    }

    public static class ArcticChill extends EnvironmentalAttack {
        private Location center;

        public ArcticChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ArcticChill(plugin); }
    }

    public static class PermafrostPulse extends EnvironmentalAttack {
        private Location center;

        public PermafrostPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_pulse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PermafrostPulse(plugin); }
    }

    public static class FrostBite extends EnvironmentalAttack {
        private Location center;

        public FrostBite(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_bite_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostBite(plugin); }
    }

    public static class HypothermiaWave extends EnvironmentalAttack {
        private Location center;

        public HypothermiaWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_wave", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new HypothermiaWave(plugin); }
    }

    public static class DeepFreeze extends EnvironmentalAttack {
        private Location center;

        public DeepFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new DeepFreeze(plugin); }
    }

    public static class ColdSnap extends EnvironmentalAttack {
        private Location center;

        public ColdSnap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_snap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ColdSnap(plugin); }
    }

    public static class ShiveringFit extends EnvironmentalAttack {
        private Location center;

        public ShiveringFit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shivering_fit", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ShiveringFit(plugin); }
    }

    public static class BreathCloud extends EnvironmentalAttack {
        private Location center;

        public BreathCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("breath_cloud", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new BreathCloud(plugin); }
    }

    public static class BodyHeatDrain extends EnvironmentalAttack {
        private Location center;

        public BodyHeatDrain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("body_heat_drain", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new BodyHeatDrain(plugin); }
    }

    public static class WindChill extends EnvironmentalAttack {
        private Location center;

        public WindChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new WindChill(plugin); }
    }

    public static class FrostCreepEnv extends EnvironmentalAttack {
        private Location center;

        public FrostCreepEnv(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_creep_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostCreepEnv(plugin); }
    }

    public static class CoreFreeze extends EnvironmentalAttack {
        private Location center;

        public CoreFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("core_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CoreFreeze(plugin); }
    }

    public static class ThawFakeout extends EnvironmentalAttack {
        private Location center;

        public ThawFakeout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_fakeout", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ThawFakeout(plugin); }
    }
}
