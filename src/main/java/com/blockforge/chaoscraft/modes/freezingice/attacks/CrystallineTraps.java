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

public final class CrystallineTraps {
    private CrystallineTraps() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceCage(plugin));
        registry.register(new FrostEncasement(plugin));
        registry.register(new CrystalSnare(plugin));
        registry.register(new IceMine(plugin));
        registry.register(new FrostWeb(plugin));
        registry.register(new GlacialPrison(plugin));
        registry.register(new IceFloor(plugin));
        registry.register(new FrostChain(plugin));
        registry.register(new CrystalMaze(plugin));
        registry.register(new IceTrapDoor(plugin));
        registry.register(new FrostRing(plugin));
        registry.register(new GlacialVice(plugin));
        registry.register(new IceCocoon(plugin));
    }

    public static class IceCage extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceCage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceCage(plugin); }
    }

    public static class FrostEncasement extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostEncasement(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_encasement", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostEncasement(plugin); }
    }

    public static class CrystalSnare extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalSnare(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_snare", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalSnare(plugin); }
    }

    public static class IceMine extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceMine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_mine", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceMine(plugin); }
    }

    public static class FrostWeb extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostWeb(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_web", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostWeb(plugin); }
    }

    public static class GlacialPrison extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialPrison(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_prison", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialPrison(plugin); }
    }

    public static class IceFloor extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_floor", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceFloor(plugin); }
    }

    public static class FrostChain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_chain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostChain(plugin); }
    }

    public static class CrystalMaze extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalMaze(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_maze", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalMaze(plugin); }
    }

    public static class IceTrapDoor extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceTrapDoor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_trap_door", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceTrapDoor(plugin); }
    }

    public static class FrostRing extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_ring", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostRing(plugin); }
    }

    public static class GlacialVice extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialVice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_vice", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialVice(plugin); }
    }

    public static class IceCocoon extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceCocoon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_cocoon", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceCocoon(plugin); }
    }
}
