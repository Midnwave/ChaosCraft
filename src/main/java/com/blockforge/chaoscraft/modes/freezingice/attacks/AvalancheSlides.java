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

public final class AvalancheSlides {
    private AvalancheSlides() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SnowBoulder(plugin));
        registry.register(new IceAvalanche(plugin));
        registry.register(new GlacierSlide(plugin));
        registry.register(new FrostTsunami(plugin));
        registry.register(new IceBallBarrage(plugin));
        registry.register(new SnowSlide(plugin));
        registry.register(new FrostRoller(plugin));
        registry.register(new CrystalCascade(plugin));
        registry.register(new IceRam(plugin));
        registry.register(new FrostStampede(plugin));
        registry.register(new AvalancheFunnel(plugin));
        registry.register(new GlacialSurge(plugin));
        registry.register(new IceDebrisFlow(plugin));
    }

    public static class SnowBoulder extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public SnowBoulder(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_boulder", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SnowBoulder(plugin); }
    }

    public static class IceAvalanche extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceAvalanche(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_avalanche", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceAvalanche(plugin); }
    }

    public static class GlacierSlide extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacierSlide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacier_slide", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacierSlide(plugin); }
    }

    public static class FrostTsunami extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostTsunami(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_tsunami", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostTsunami(plugin); }
    }

    public static class IceBallBarrage extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceBallBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_ball_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceBallBarrage(plugin); }
    }

    public static class SnowSlide extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public SnowSlide(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_slide", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SnowSlide(plugin); }
    }

    public static class FrostRoller extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostRoller(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_roller", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostRoller(plugin); }
    }

    public static class CrystalCascade extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalCascade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_cascade", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalCascade(plugin); }
    }

    public static class IceRam extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceRam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_ram", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceRam(plugin); }
    }

    public static class FrostStampede extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostStampede(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_stampede", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostStampede(plugin); }
    }

    public static class AvalancheFunnel extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public AvalancheFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("avalanche_funnel", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new AvalancheFunnel(plugin); }
    }

    public static class GlacialSurge extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_surge", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialSurge(plugin); }
    }

    public static class IceDebrisFlow extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceDebrisFlow(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_debris_flow", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceDebrisFlow(plugin); }
    }
}
