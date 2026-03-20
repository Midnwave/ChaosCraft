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

public final class BlizzardWeather {
    private BlizzardWeather() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new Blizzard(plugin));
        registry.register(new IceStorm(plugin));
        registry.register(new FrostWind(plugin));
        registry.register(new WhiteoutBurst(plugin));
        registry.register(new SleetRain(plugin));
        registry.register(new ArcticGale(plugin));
        registry.register(new SnowSquall(plugin));
        registry.register(new FrostMist(plugin));
        registry.register(new CrystalShower(plugin));
        registry.register(new PolarVortex(plugin));
        registry.register(new FrostCyclone(plugin));
        registry.register(new IceNeedles(plugin));
        registry.register(new SubzeroBlast(plugin));
    }

    public static class Blizzard extends EnvironmentalAttack {
        private Location center;

        public Blizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new Blizzard(plugin); }
    }

    public static class IceStorm extends EnvironmentalAttack {
        private Location center;

        public IceStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_storm_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceStorm(plugin); }
    }

    public static class FrostWind extends EnvironmentalAttack {
        private Location center;

        public FrostWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_wind_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostWind(plugin); }
    }

    public static class WhiteoutBurst extends EnvironmentalAttack {
        private Location center;

        public WhiteoutBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whiteout_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new WhiteoutBurst(plugin); }
    }

    public static class SleetRain extends EnvironmentalAttack {
        private Location center;

        public SleetRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sleet_rain", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SleetRain(plugin); }
    }

    public static class ArcticGale extends EnvironmentalAttack {
        private Location center;

        public ArcticGale(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_gale", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ArcticGale(plugin); }
    }

    public static class SnowSquall extends EnvironmentalAttack {
        private Location center;

        public SnowSquall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_squall", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SnowSquall(plugin); }
    }

    public static class FrostMist extends EnvironmentalAttack {
        private Location center;

        public FrostMist(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_mist", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostMist(plugin); }
    }

    public static class CrystalShower extends EnvironmentalAttack {
        private Location center;

        public CrystalShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_shower", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrystalShower(plugin); }
    }

    public static class PolarVortex extends EnvironmentalAttack {
        private Location center;

        public PolarVortex(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("polar_vortex", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PolarVortex(plugin); }
    }

    public static class FrostCyclone extends EnvironmentalAttack {
        private Location center;

        public FrostCyclone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_cyclone", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostCyclone(plugin); }
    }

    public static class IceNeedles extends EnvironmentalAttack {
        private Location center;

        public IceNeedles(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_needles", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceNeedles(plugin); }
    }

    public static class SubzeroBlast extends EnvironmentalAttack {
        private Location center;

        public SubzeroBlast(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subzero_blast", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SubzeroBlast(plugin); }
    }
}
