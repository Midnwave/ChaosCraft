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

public final class ThawAndRefreeze {
    private ThawAndRefreeze() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ThawBurst(plugin));
        registry.register(new HeatThenFreeze(plugin));
        registry.register(new MeltdownRefreeze(plugin));
        registry.register(new ThermalShock(plugin));
        registry.register(new SpringFakeout(plugin));
        registry.register(new VolcanicIce(plugin));
        registry.register(new SteamExplosion(plugin));
        registry.register(new GeyserFreeze(plugin));
        registry.register(new ThawTrap(plugin));
        registry.register(new CrackAndMelt(plugin));
        registry.register(new SunBurst(plugin));
        registry.register(new FrostFireCycle(plugin));
        registry.register(new Permathaw(plugin));
    }

    public static class ThawBurst extends EnvironmentalAttack {
        private Location center;

        public ThawBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ThawBurst(plugin); }
    }

    public static class HeatThenFreeze extends EnvironmentalAttack {
        private Location center;

        public HeatThenFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("heat_then_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new HeatThenFreeze(plugin); }
    }

    public static class MeltdownRefreeze extends EnvironmentalAttack {
        private Location center;

        public MeltdownRefreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meltdown_refreeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new MeltdownRefreeze(plugin); }
    }

    public static class ThermalShock extends EnvironmentalAttack {
        private Location center;

        public ThermalShock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thermal_shock", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ThermalShock(plugin); }
    }

    public static class SpringFakeout extends EnvironmentalAttack {
        private Location center;

        public SpringFakeout(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spring_fakeout", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SpringFakeout(plugin); }
    }

    public static class VolcanicIce extends EnvironmentalAttack {
        private Location center;

        public VolcanicIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ice", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new VolcanicIce(plugin); }
    }

    public static class SteamExplosion extends EnvironmentalAttack {
        private Location center;

        public SteamExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("steam_explosion", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SteamExplosion(plugin); }
    }

    public static class GeyserFreeze extends EnvironmentalAttack {
        private Location center;

        public GeyserFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("geyser_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GeyserFreeze(plugin); }
    }

    public static class ThawTrap extends EnvironmentalAttack {
        private Location center;

        public ThawTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thaw_trap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ThawTrap(plugin); }
    }

    public static class CrackAndMelt extends EnvironmentalAttack {
        private Location center;

        public CrackAndMelt(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crack_and_melt", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrackAndMelt(plugin); }
    }

    public static class SunBurst extends EnvironmentalAttack {
        private Location center;

        public SunBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sun_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SunBurst(plugin); }
    }

    public static class FrostFireCycle extends EnvironmentalAttack {
        private Location center;

        public FrostFireCycle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_fire_cycle", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostFireCycle(plugin); }
    }

    public static class Permathaw extends EnvironmentalAttack {
        private Location center;

        public Permathaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permathaw", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new Permathaw(plugin); }
    }
}
