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

public final class FrozenMobEffects {
    private FrozenMobEffects() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new FrostSwarm(plugin));
        registry.register(new IceWraithPass(plugin));
        registry.register(new CrystalBeastRoar(plugin));
        registry.register(new FrostBiteAttack(plugin));
        registry.register(new GlacialCharge(plugin));
        registry.register(new IceSpiderWeb(plugin));
        registry.register(new FrostHowl(plugin));
        registry.register(new CrystallineScream(plugin));
        registry.register(new IceBreath(plugin));
        registry.register(new PermafrostStomp(plugin));
        registry.register(new FrostHunterDash(plugin));
        registry.register(new GlacialRoarWave(plugin));
        registry.register(new IceStalkerAmbush(plugin));
    }

    public static class FrostSwarm extends EnvironmentalAttack {
        private Location center;

        public FrostSwarm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_swarm_env", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostSwarm(plugin); }
    }

    public static class IceWraithPass extends EnvironmentalAttack {
        private Location center;

        public IceWraithPass(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_wraith_pass", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceWraithPass(plugin); }
    }

    public static class CrystalBeastRoar extends EnvironmentalAttack {
        private Location center;

        public CrystalBeastRoar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_beast_roar", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrystalBeastRoar(plugin); }
    }

    public static class FrostBiteAttack extends EnvironmentalAttack {
        private Location center;

        public FrostBiteAttack(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_bite_attack", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostBiteAttack(plugin); }
    }

    public static class GlacialCharge extends EnvironmentalAttack {
        private Location center;

        public GlacialCharge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_charge", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialCharge(plugin); }
    }

    public static class IceSpiderWeb extends EnvironmentalAttack {
        private Location center;

        public IceSpiderWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_spider_web", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceSpiderWeb(plugin); }
    }

    public static class FrostHowl extends EnvironmentalAttack {
        private Location center;

        public FrostHowl(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_howl", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostHowl(plugin); }
    }

    public static class CrystallineScream extends EnvironmentalAttack {
        private Location center;

        public CrystallineScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_scream", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrystallineScream(plugin); }
    }

    public static class IceBreath extends EnvironmentalAttack {
        private Location center;

        public IceBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_breath", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceBreath(plugin); }
    }

    public static class PermafrostStomp extends EnvironmentalAttack {
        private Location center;

        public PermafrostStomp(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_stomp", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PermafrostStomp(plugin); }
    }

    public static class FrostHunterDash extends EnvironmentalAttack {
        private Location center;

        public FrostHunterDash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_hunter_dash", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostHunterDash(plugin); }
    }

    public static class GlacialRoarWave extends EnvironmentalAttack {
        private Location center;

        public GlacialRoarWave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_roar_wave", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialRoarWave(plugin); }
    }

    public static class IceStalkerAmbush extends EnvironmentalAttack {
        private Location center;

        public IceStalkerAmbush(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_stalker_ambush", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceStalkerAmbush(plugin); }
    }
}
