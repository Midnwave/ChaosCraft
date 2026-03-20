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

public final class FrostWeaponry {
    private FrostWeaponry() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IcicleVolley(plugin));
        registry.register(new FrostHammer(plugin));
        registry.register(new IceLance(plugin));
        registry.register(new GlacialSword(plugin));
        registry.register(new FrostAxe(plugin));
        registry.register(new IcicleGatling(plugin));
        registry.register(new FrostScythe(plugin));
        registry.register(new CrystalMace(plugin));
        registry.register(new IceShuriken(plugin));
        registry.register(new FrostTrident(plugin));
        registry.register(new GlacialFlail(plugin));
        registry.register(new IceJavelinRain(plugin));
        registry.register(new FrostWhip(plugin));
    }

    public static class IcicleVolley extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IcicleVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_volley", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IcicleVolley(plugin); }
    }

    public static class FrostHammer extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostHammer(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_hammer", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostHammer(plugin); }
    }

    public static class IceLance extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_lance", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceLance(plugin); }
    }

    public static class GlacialSword extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialSword(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_sword", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialSword(plugin); }
    }

    public static class FrostAxe extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostAxe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_axe", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostAxe(plugin); }
    }

    public static class IcicleGatling extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IcicleGatling(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_gatling", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IcicleGatling(plugin); }
    }

    public static class FrostScythe extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_scythe", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostScythe(plugin); }
    }

    public static class CrystalMace extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalMace(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_mace", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalMace(plugin); }
    }

    public static class IceShuriken extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceShuriken(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shuriken", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceShuriken(plugin); }
    }

    public static class FrostTrident extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_trident", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostTrident(plugin); }
    }

    public static class GlacialFlail extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialFlail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_flail", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialFlail(plugin); }
    }

    public static class IceJavelinRain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceJavelinRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_javelin_rain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceJavelinRain(plugin); }
    }

    public static class FrostWhip extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostWhip(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_whip", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostWhip(plugin); }
    }
}
