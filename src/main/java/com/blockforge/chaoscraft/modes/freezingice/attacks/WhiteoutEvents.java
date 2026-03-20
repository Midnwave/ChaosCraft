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

public final class WhiteoutEvents {
    private WhiteoutEvents() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhiteoutPulse(plugin));
        registry.register(new FrostHaze(plugin));
        registry.register(new SnowBlind(plugin));
        registry.register(new IceMirrorFlash(plugin));
        registry.register(new ColdDarkness(plugin));
        registry.register(new FrostStatic(plugin));
        registry.register(new ArcticNight(plugin));
        registry.register(new CrystalRefraction(plugin));
        registry.register(new BlindingSnow(plugin));
        registry.register(new FrostFlicker(plugin));
        registry.register(new GlacialGlare(plugin));
        registry.register(new IceVeilDrop(plugin));
        registry.register(new PermafrostMirage(plugin));
    }

    public static class WhiteoutPulse extends EnvironmentalAttack {
        private Location center;

        public WhiteoutPulse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whiteout_pulse", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new WhiteoutPulse(plugin); }
    }

    public static class FrostHaze extends EnvironmentalAttack {
        private Location center;

        public FrostHaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_haze", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostHaze(plugin); }
    }

    public static class SnowBlind extends EnvironmentalAttack {
        private Location center;

        public SnowBlind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_blind", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new SnowBlind(plugin); }
    }

    public static class IceMirrorFlash extends EnvironmentalAttack {
        private Location center;

        public IceMirrorFlash(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror_flash", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceMirrorFlash(plugin); }
    }

    public static class ColdDarkness extends EnvironmentalAttack {
        private Location center;

        public ColdDarkness(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cold_darkness", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ColdDarkness(plugin); }
    }

    public static class FrostStatic extends EnvironmentalAttack {
        private Location center;

        public FrostStatic(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_static", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostStatic(plugin); }
    }

    public static class ArcticNight extends EnvironmentalAttack {
        private Location center;

        public ArcticNight(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("arctic_night", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new ArcticNight(plugin); }
    }

    public static class CrystalRefraction extends EnvironmentalAttack {
        private Location center;

        public CrystalRefraction(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_refraction", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new CrystalRefraction(plugin); }
    }

    public static class BlindingSnow extends EnvironmentalAttack {
        private Location center;

        public BlindingSnow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blinding_snow", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new BlindingSnow(plugin); }
    }

    public static class FrostFlicker extends EnvironmentalAttack {
        private Location center;

        public FrostFlicker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_flicker", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new FrostFlicker(plugin); }
    }

    public static class GlacialGlare extends EnvironmentalAttack {
        private Location center;

        public GlacialGlare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_glare", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new GlacialGlare(plugin); }
    }

    public static class IceVeilDrop extends EnvironmentalAttack {
        private Location center;

        public IceVeilDrop(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_veil_drop", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new IceVeilDrop(plugin); }
    }

    public static class PermafrostMirage extends EnvironmentalAttack {
        private Location center;

        public PermafrostMirage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_mirage", AttackType.ENVIRONMENTAL, 1, "modes/freezingice/attacks"));
            config.setDamage(30.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override public AbstractAttack newInstance() { return new PermafrostMirage(plugin); }
    }
}
