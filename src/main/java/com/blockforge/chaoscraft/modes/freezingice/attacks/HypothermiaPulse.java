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

public final class HypothermiaPulse {
    private HypothermiaPulse() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SlowFreeze(plugin));
        registry.register(new FrostAccumulation(plugin));
        registry.register(new ColdSap(plugin));
        registry.register(new HypothermiaStage1(plugin));
        registry.register(new HypothermiaStage2(plugin));
        registry.register(new HypothermiaStage3(plugin));
        registry.register(new CoreTemperatureDrop(plugin));
        registry.register(new ExtremitiesFreeze(plugin));
        registry.register(new WindChillFactor(plugin));
        registry.register(new FrostShock(plugin));
        registry.register(new ColdSweat(plugin));
        registry.register(new NumbnessSpread(plugin));
        registry.register(new FinalChill(plugin));
    }

    public static class SlowFreeze extends EnvironmentalAttack {
        private Location center;

        public SlowFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("slow_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SlowFreeze(plugin); }
    }

    public static class FrostAccumulation extends EnvironmentalAttack {
        private Location center;

        public FrostAccumulation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_accumulation", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostAccumulation(plugin); }
    }

    public static class ColdSap extends EnvironmentalAttack {
        private Location center;

        public ColdSap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_sap", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ColdSap(plugin); }
    }

    public static class HypothermiaStage1 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage1(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage1", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new HypothermiaStage1(plugin); }
    }

    public static class HypothermiaStage2 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage2(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage2", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new HypothermiaStage2(plugin); }
    }

    public static class HypothermiaStage3 extends EnvironmentalAttack {
        private Location center;

        public HypothermiaStage3(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hypothermia_stage3", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new HypothermiaStage3(plugin); }
    }

    public static class CoreTemperatureDrop extends EnvironmentalAttack {
        private Location center;

        public CoreTemperatureDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("core_temp_drop", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CoreTemperatureDrop(plugin); }
    }

    public static class ExtremitiesFreeze extends EnvironmentalAttack {
        private Location center;

        public ExtremitiesFreeze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("extremities_freeze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ExtremitiesFreeze(plugin); }
    }

    public static class WindChillFactor extends EnvironmentalAttack {
        private Location center;

        public WindChillFactor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("wind_chill_factor", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new WindChillFactor(plugin); }
    }

    public static class FrostShock extends EnvironmentalAttack {
        private Location center;

        public FrostShock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_shock", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostShock(plugin); }
    }

    public static class ColdSweat extends EnvironmentalAttack {
        private Location center;

        public ColdSweat(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_sweat", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ColdSweat(plugin); }
    }

    public static class NumbnessSpread extends EnvironmentalAttack {
        private Location center;

        public NumbnessSpread(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("numbness_spread", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new NumbnessSpread(plugin); }
    }

    public static class FinalChill extends EnvironmentalAttack {
        private Location center;

        public FinalChill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("final_chill", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FinalChill(plugin); }
    }
}
