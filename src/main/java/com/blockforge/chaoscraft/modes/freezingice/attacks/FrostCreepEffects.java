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

public final class FrostCreepEffects {
    private FrostCreepEffects() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new GroundFreeze(plugin));
        registry.register(new PowderSnowTrap(plugin));
        registry.register(new BlackIce(plugin));
        registry.register(new FrostRime(plugin));
        registry.register(new IceSlick(plugin));
        registry.register(new PermafrostSpread(plugin));
        registry.register(new SnowDrift(plugin));
        registry.register(new FreezingFog(plugin));
        registry.register(new CrystalGrowth(plugin));
        registry.register(new FrostLine(plugin));
        registry.register(new IcePatches(plugin));
        registry.register(new GlacialAdvance(plugin));
        registry.register(new FlashFreeze(plugin));
    }

    public static class GroundFreeze extends EnvironmentalAttack {
        private Location center;

        public GroundFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ground_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GroundFreeze(plugin); }
    }

    public static class PowderSnowTrap extends EnvironmentalAttack {
        private Location center;

        public PowderSnowTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("powder_snow_trap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PowderSnowTrap(plugin); }
    }

    public static class BlackIce extends EnvironmentalAttack {
        private Location center;

        public BlackIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("black_ice", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new BlackIce(plugin); }
    }

    public static class FrostRime extends EnvironmentalAttack {
        private Location center;

        public FrostRime(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_rime", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostRime(plugin); }
    }

    public static class IceSlick extends EnvironmentalAttack {
        private Location center;

        public IceSlick(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_slick", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceSlick(plugin); }
    }

    public static class PermafrostSpread extends EnvironmentalAttack {
        private Location center;

        public PermafrostSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PermafrostSpread(plugin); }
    }

    public static class SnowDrift extends EnvironmentalAttack {
        private Location center;

        public SnowDrift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_drift", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SnowDrift(plugin); }
    }

    public static class FreezingFog extends EnvironmentalAttack {
        private Location center;

        public FreezingFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("freezing_fog", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FreezingFog(plugin); }
    }

    public static class CrystalGrowth extends EnvironmentalAttack {
        private Location center;

        public CrystalGrowth(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_growth", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrystalGrowth(plugin); }
    }

    public static class FrostLine extends EnvironmentalAttack {
        private Location center;

        public FrostLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_line", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostLine(plugin); }
    }

    public static class IcePatches extends EnvironmentalAttack {
        private Location center;

        public IcePatches(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_patches", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IcePatches(plugin); }
    }

    public static class GlacialAdvance extends EnvironmentalAttack {
        private Location center;

        public GlacialAdvance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_advance", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialAdvance(plugin); }
    }

    public static class FlashFreeze extends EnvironmentalAttack {
        private Location center;

        public FlashFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flash_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FlashFreeze(plugin); }
    }
}
