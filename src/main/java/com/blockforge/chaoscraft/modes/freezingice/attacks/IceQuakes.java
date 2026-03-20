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

public final class IceQuakes {
    private IceQuakes() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostQuake(plugin));
        registry.register(new GlacialShift(plugin));
        registry.register(new IceCrackSpread(plugin));
        registry.register(new PermafrostSplit(plugin));
        registry.register(new AvalancheRumble(plugin));
        registry.register(new FrostTremor(plugin));
        registry.register(new GlacialCollapse(plugin));
        registry.register(new IcePlateTectonics(plugin));
        registry.register(new SeismicFrost(plugin));
        registry.register(new CryogenicBurst(plugin));
        registry.register(new FrostfaultLine(plugin));
        registry.register(new IceShatter(plugin));
        registry.register(new SubglacialRumble(plugin));
    }

    public static class FrostQuake extends EnvironmentalAttack {
        private Location center;

        public FrostQuake(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_quake", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostQuake(plugin); }
    }

    public static class GlacialShift extends EnvironmentalAttack {
        private Location center;

        public GlacialShift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_shift", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialShift(plugin); }
    }

    public static class IceCrackSpread extends EnvironmentalAttack {
        private Location center;

        public IceCrackSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_crack_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceCrackSpread(plugin); }
    }

    public static class PermafrostSplit extends EnvironmentalAttack {
        private Location center;

        public PermafrostSplit(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_split", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PermafrostSplit(plugin); }
    }

    public static class AvalancheRumble extends EnvironmentalAttack {
        private Location center;

        public AvalancheRumble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_rumble", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new AvalancheRumble(plugin); }
    }

    public static class FrostTremor extends EnvironmentalAttack {
        private Location center;

        public FrostTremor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_tremor", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostTremor(plugin); }
    }

    public static class GlacialCollapse extends EnvironmentalAttack {
        private Location center;

        public GlacialCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_collapse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialCollapse(plugin); }
    }

    public static class IcePlateTectonics extends EnvironmentalAttack {
        private Location center;

        public IcePlateTectonics(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_plate_tectonics", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IcePlateTectonics(plugin); }
    }

    public static class SeismicFrost extends EnvironmentalAttack {
        private Location center;

        public SeismicFrost(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("seismic_frost", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SeismicFrost(plugin); }
    }

    public static class CryogenicBurst extends EnvironmentalAttack {
        private Location center;

        public CryogenicBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cryogenic_burst", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CryogenicBurst(plugin); }
    }

    public static class FrostfaultLine extends EnvironmentalAttack {
        private Location center;

        public FrostfaultLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frostfault_line", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostfaultLine(plugin); }
    }

    public static class IceShatter extends EnvironmentalAttack {
        private Location center;

        public IceShatter(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shatter", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceShatter(plugin); }
    }

    public static class SubglacialRumble extends EnvironmentalAttack {
        private Location center;

        public SubglacialRumble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("subglacial_rumble", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SubglacialRumble(plugin); }
    }
}
