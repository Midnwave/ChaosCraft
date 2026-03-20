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

public final class BlizzardProjectiles {
    private BlizzardProjectiles() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceShardBarrage(plugin));
        registry.register(new HailStones(plugin));
        registry.register(new FrostDebris(plugin));
        registry.register(new IcicleRain(plugin));
        registry.register(new SnowballVolley(plugin));
        registry.register(new FrostMissile(plugin));
        registry.register(new CrystalShrapnel(plugin));
        registry.register(new BlizzardWall(plugin));
        registry.register(new IceTornado(plugin));
        registry.register(new FrostComet(plugin));
        registry.register(new GlacialBullets(plugin));
        registry.register(new IceBoomerang(plugin));
        registry.register(new FrostNova(plugin));
    }

    public static class IceShardBarrage extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceShardBarrage(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_shard_barrage", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceShardBarrage(plugin); }
    }

    public static class HailStones extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public HailStones(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hail_stones", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HailStones(plugin); }
    }

    public static class FrostDebris extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostDebris(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_debris", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostDebris(plugin); }
    }

    public static class IcicleRain extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IcicleRain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("icicle_rain", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IcicleRain(plugin); }
    }

    public static class SnowballVolley extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public SnowballVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snowball_volley", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SnowballVolley(plugin); }
    }

    public static class FrostMissile extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostMissile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_missile", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostMissile(plugin); }
    }

    public static class CrystalShrapnel extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public CrystalShrapnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystal_shrapnel", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrystalShrapnel(plugin); }
    }

    public static class BlizzardWall extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public BlizzardWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blizzard_wall", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BlizzardWall(plugin); }
    }

    public static class IceTornado extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_tornado", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceTornado(plugin); }
    }

    public static class FrostComet extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_comet", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostComet(plugin); }
    }

    public static class GlacialBullets extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public GlacialBullets(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("glacial_bullets", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new GlacialBullets(plugin); }
    }

    public static class IceBoomerang extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public IceBoomerang(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_boomerang", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new IceBoomerang(plugin); }
    }

    public static class FrostNova extends BlockDisplayAttack {
        private Location center;
        private final List<BlockDisplayHandle> all = new ArrayList<>();

        public FrostNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frost_nova", AttackType.BLOCK_DISPLAY, 1, "modes/freezingice/attacks"));
            config.setDamage(40.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(250);
        }

        @Override protected void onSpawn(Location center) { this.center = center.clone(); }
        @Override protected void onTick(int tick) { }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new FrostNova(plugin); }
    }
}
