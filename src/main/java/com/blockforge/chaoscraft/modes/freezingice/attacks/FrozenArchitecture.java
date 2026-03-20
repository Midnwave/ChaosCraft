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

public final class FrozenArchitecture {
    private FrozenArchitecture() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceCastle(plugin));
        registry.register(new FrozenBridge(plugin));
        registry.register(new CrystallineTower(plugin));
        registry.register(new IceThrone(plugin));
        registry.register(new FrostGate(plugin));
        registry.register(new GlacialAltar(plugin));
        registry.register(new IceObelisk(plugin));
        registry.register(new FrozenFountain(plugin));
        registry.register(new CrystalChandelier(plugin));
        registry.register(new IceStatue(plugin));
        registry.register(new FrostCathedral(plugin));
        registry.register(new GlacialPyramid(plugin));
        registry.register(new IceMirrorHall(plugin));
    }

    public static class IceCastle extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceCastle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_castle", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceCastle(plugin); }
    }

    public static class FrozenBridge extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrozenBridge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_bridge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrozenBridge(plugin); }
    }

    public static class CrystallineTower extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystallineTower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_tower", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystallineTower(plugin); }
    }

    public static class IceThrone extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceThrone(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_throne", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceThrone(plugin); }
    }

    public static class FrostGate extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostGate(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_gate", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostGate(plugin); }
    }

    public static class GlacialAltar extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialAltar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_altar", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialAltar(plugin); }
    }

    public static class IceObelisk extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceObelisk(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_obelisk", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceObelisk(plugin); }
    }

    public static class FrozenFountain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrozenFountain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_fountain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrozenFountain(plugin); }
    }

    public static class CrystalChandelier extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalChandelier(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_chandelier", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalChandelier(plugin); }
    }

    public static class IceStatue extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceStatue(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_statue", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceStatue(plugin); }
    }

    public static class FrostCathedral extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_cathedral", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostCathedral(plugin); }
    }

    public static class GlacialPyramid extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialPyramid(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_pyramid", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialPyramid(plugin); }
    }

    public static class IceMirrorHall extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceMirrorHall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mirror_hall", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceMirrorHall(plugin); }
    }
}
