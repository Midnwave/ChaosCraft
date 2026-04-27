package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Devil's Dream Mode — MODEL ENGINE VFX ATTACKS
 * 25 nightmare-themed ModelEngine 4 attacks. Each spawns a .bbmodel blueprint,
 * plays animations, and deals damage through unique mechanics.
 *
 * Color palette:
 * - Blood-red:     RGB(192, 16, 16)
 * - Bone-white:    RGB(232, 228, 220)
 * - Sickly yellow: RGB(212, 200, 32)
 * - Void purple:   RGB(96, 32, 160)
 *
 * Each model has spawn/idle/dissipate animations defined in the .bbmodel file.
 */
public final class DevilsDreamModelEngine {
    private DevilsDreamModelEngine() {}

    private static final String MODE_PATH = "modes/devilsdream/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // 1. Ground eruption
        registry.register(new FallenAngelDescent(plugin));
        registry.register(new NightmareRootSurge(plugin));
        registry.register(new DevilsSpineArray(plugin));
        // 4-8. AOE bursts
        registry.register(new FallenHaloBurst(plugin));
        registry.register(new DreamCollapseRing(plugin));
        registry.register(new DevilsSermonNova(plugin));
        registry.register(new NightmareStaticField(plugin));
        registry.register(new InfernalCrownBurst(plugin));
        // 9-14. Projectiles / Beams
        registry.register(new FallenFeatherLance(plugin));
        registry.register(new NightmareShardVolley(plugin));
        registry.register(new DevilsTongueBeam(plugin));
        registry.register(new SilverWingBlade(plugin));
        registry.register(new NightmareEyeProjectile(plugin));
        registry.register(new BloodComet(plugin));
        // 15-25. Summons / Orbital
        registry.register(new FallenAngelWingsSummon(plugin));
        registry.register(new NightmareCathedral(plugin));
        registry.register(new DevilsHaloArray(plugin));
        registry.register(new BoneThroneSummon(plugin));
        registry.register(new SilverMirrorPortal(plugin));
        registry.register(new DevilsConstellation(plugin));
        registry.register(new NightmareClock(plugin));
        registry.register(new FallenSeraphSkeleton(plugin));
        registry.register(new NightmarePlanetarium(plugin));
        registry.register(new InfernalScriptureArray(plugin));
        registry.register(new TheDreamItself(plugin));
    }

    // ================================================================
    // Shared helpers
    // ================================================================

    /** Blood-red dust — RGB(192, 16, 16). */
    private static void bloodDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 192, 16, 16, 1.4f);
    }

    /** Bone-white dust — RGB(232, 228, 220). */
    private static void boneDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 232, 228, 220, 1.2f);
    }

    /** Sickly yellow dust — RGB(212, 200, 32). */
    private static void sicklyDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 212, 200, 32, 1.3f);
    }

    /** Void purple dust — RGB(96, 32, 160). */
    private static void purpleDust(Location loc, int count, double spread) {
        DisplayBuilder.dustParticles(loc, count, spread, 96, 32, 160, 1.5f);
    }

    /** Soul flame particles. */
    private static void soulFlame(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, count, spread, spread, spread, 0.02);
    }

    /** Sculk soul particles — death-tinged. */
    private static void sculkSoul(Location loc, int count, double spread) {
        if (loc.getWorld() == null) return;
        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, count, spread, spread, spread, 0.01);
    }

    /** Damage all survival players within radius. */
    private static void damageNearby(Location center, double radius, double damage) {
        if (center.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (p.getLocation().distanceSquared(center) <= r2) {
                p.damage(damage);
                p.setNoDamageTicks(0);
            }
        }
    }

    /** Pull players toward a center point with given velocity strength. */
    private static void pullPlayersToward(Location center, double radius, double strength) {
        if (center.getWorld() == null) return;
        double r2 = radius * radius;
        for (Player p : center.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SURVIVAL) continue;
            if (p.isInvulnerable()) continue;
            if (p.getLocation().distanceSquared(center) <= r2) {
                Vector dir = center.toVector().subtract(p.getLocation().toVector()).normalize().multiply(strength);
                p.setVelocity(p.getVelocity().add(dir));
            }
        }
    }

    // Sound shorthands
    private static void caveAmb(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.AMBIENT_CAVE, v, p); }
    private static void hostileHurt(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.ENTITY_HOSTILE_HURT, v, p); }
    private static void thunderTrident(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, v, p); }
    private static void chime(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, v, p); }
    private static void portalAmb(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, v, p); }
    private static void wartPlace(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.BLOCK_WART_BLOCK_PLACE, v, p); }
    private static void anchorCharge(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, v, p); }
    private static void sculkSpread(Location loc, float v, float p) { DisplayBuilder.playSound(loc, Sound.BLOCK_SCULK_SPREAD, v, p); }

    // ================================================================
    // 1. FALLEN ANGEL DESCENT — Ground impact crater. Heavy slam.
    // ================================================================
    public static class FallenAngelDescent extends ModelEngineAttack {
        public FallenAngelDescent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fallen_angel_descent", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(240);
        }
        @Override protected String getModelId() { return "fallen_angel_descent"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.4f, 0.4f);
            thunderTrident(c, 0.8f, 0.6f);
            boneDust(c, 30, 3.0);
            purpleDust(c, 20, 2.5);
            damageNearby(c, 6.0, 16.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 6.0, 6.0); purpleDust(c, 8, 4.0); }
            if (tick % 60 == 0) caveAmb(c, 0.7f, 0.5f);
        }
        @Override public AbstractAttack newInstance() { return new FallenAngelDescent(plugin); }
    }

    // ================================================================
    // 2. NIGHTMARE ROOT SURGE — Sickly yellow root chains.
    // ================================================================
    public static class NightmareRootSurge extends ModelEngineAttack {
        public NightmareRootSurge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_root_surge", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }
        @Override protected String getModelId() { return "nightmare_root_surge"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            wartPlace(c, 1.2f, 0.7f);
            sculkSpread(c, 0.9f, 0.8f);
            sicklyDust(c, 25, 2.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 7.0, 8.0); sicklyDust(c, 6, 5.0); }
            if (tick % 40 == 0) sculkSoul(c, 8, 3.0);
        }
        @Override public AbstractAttack newInstance() { return new NightmareRootSurge(plugin); }
    }

    // ================================================================
    // 3. DEVIL'S SPINE ARRAY — Vertebrae rising in S-curve.
    // ================================================================
    public static class DevilsSpineArray extends ModelEngineAttack {
        public DevilsSpineArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_spine_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }
        @Override protected String getModelId() { return "devils_spine_array"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.3f, 0.5f);
            boneDust(c, 30, 4.0);
            purpleDust(c, 15, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 25 == 0) { damageNearby(c, 12.0, 6.0); boneDust(c, 8, 6.0); }
            if (tick % 50 == 0) caveAmb(c, 0.6f, 0.4f);
        }
        @Override public AbstractAttack newInstance() { return new DevilsSpineArray(plugin); }
    }

    // ================================================================
    // 4. FALLEN HALO BURST — Halo above caster, detonates outward.
    // ================================================================
    public static class FallenHaloBurst extends ModelEngineAttack {
        private boolean detonated = false;
        public FallenHaloBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fallen_halo_burst", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(14.0);
            config.setDamageRadius(6.5);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(200);
            config.setCooldownTicks(220);
        }
        @Override protected String getModelId() { return "fallen_halo_burst"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 1.4f, 0.8f);
            boneDust(c.clone().add(0, 8, 0), 40, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (!detonated && tick > 100) {
                thunderTrident(c, 1.5f, 0.5f);
                damageNearby(c, 8.0, 14.0);
                boneDust(c.clone().add(0, 8, 0), 60, 6.0);
                detonated = true;
            }
            if (tick % 20 == 0) chime(c.clone().add(0, 8, 0), 0.5f, 1.4f);
        }
        @Override public AbstractAttack newInstance() { return new FallenHaloBurst(plugin); }
    }

    // ================================================================
    // 5. DREAM COLLAPSE RING — Concentric rings collapsing inward.
    // ================================================================
    public static class DreamCollapseRing extends ModelEngineAttack {
        public DreamCollapseRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_collapse_ring", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(240);
            config.setCooldownTicks(220);
        }
        @Override protected String getModelId() { return "dream_collapse_ring"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            portalAmb(c, 1.3f, 0.5f);
            purpleDust(c, 40, 5.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) {
                damageNearby(c, 15.0, 5.0);
                pullPlayersToward(c, 15.0, 0.15);
                purpleDust(c, 12, 8.0);
            }
            if (tick % 40 == 0) portalAmb(c, 0.8f, 0.6f);
        }
        @Override public AbstractAttack newInstance() { return new DreamCollapseRing(plugin); }
    }

    // ================================================================
    // 6. DEVIL'S SERMON NOVA — Spherical 3D ray burst.
    // ================================================================
    public static class DevilsSermonNova extends ModelEngineAttack {
        public DevilsSermonNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_sermon_nova", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(220);
            config.setCooldownTicks(200);
        }
        @Override protected String getModelId() { return "devils_sermon_nova"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            thunderTrident(c, 1.5f, 0.4f);
            anchorCharge(c, 1.2f, 0.3f);
            bloodDust(c, 30, 3.0);
            soulFlame(c, 20, 2.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 8.0, 6.0); bloodDust(c, 8, 5.0); }
            if (tick % 40 == 0) hostileHurt(c, 0.7f, 0.6f);
        }
        @Override public AbstractAttack newInstance() { return new DevilsSermonNova(plugin); }
    }

    // ================================================================
    // 7. NIGHTMARE STATIC FIELD — Scattered chaotic shards.
    // ================================================================
    public static class NightmareStaticField extends ModelEngineAttack {
        public NightmareStaticField(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_static_field", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(14.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }
        @Override protected String getModelId() { return "nightmare_static_field"; }
        @Override protected double getModelScale() { return 3.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            sculkSpread(c, 1.2f, 0.5f);
            purpleDust(c, 40, 7.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 15 == 0) { damageNearby(c, 14.0, 4.0); sculkSoul(c, 10, 7.0); }
            if (tick % 30 == 0) caveAmb(c, 0.5f, 0.3f);
        }
        @Override public AbstractAttack newInstance() { return new NightmareStaticField(plugin); }
    }

    // ================================================================
    // 8. INFERNAL CROWN BURST — Crown at head height, detonates.
    // ================================================================
    public static class InfernalCrownBurst extends ModelEngineAttack {
        private boolean detonated = false;
        public InfernalCrownBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_crown_burst", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(11.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(220);
            config.setCooldownTicks(200);
        }
        @Override protected String getModelId() { return "infernal_crown_burst"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.3f, 0.5f);
            purpleDust(c.clone().add(0, 10, 0), 30, 3.0);
            bloodDust(c.clone().add(0, 10, 0), 20, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (!detonated && tick > 120) {
                thunderTrident(c, 1.4f, 0.5f);
                damageNearby(c, 8.0, 14.0);
                bloodDust(c, 50, 6.0);
                detonated = true;
            }
            if (tick % 20 == 0) damageNearby(c, 5.5, 5.0);
        }
        @Override public AbstractAttack newInstance() { return new InfernalCrownBurst(plugin); }
    }

    // ================================================================
    // 9. FALLEN FEATHER LANCE — Long projectile.
    // ================================================================
    public static class FallenFeatherLance extends ModelEngineAttack {
        public FallenFeatherLance(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fallen_feather_lance", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(16.0);
            config.setDamageRadius(2.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(100);
            config.setCooldownTicks(100);
        }
        @Override protected String getModelId() { return "fallen_feather_lance"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 1.0f, 0.4f);
            boneDust(c, 15, 1.5);
            purpleDust(c, 8, 1.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 15 == 0) { damageNearby(c, 2.5, 8.0); boneDust(c, 5, 2.0); }
        }
        @Override public AbstractAttack newInstance() { return new FallenFeatherLance(plugin); }
    }

    // ================================================================
    // 10. NIGHTMARE SHARD VOLLEY — Tight projectile cluster.
    // ================================================================
    public static class NightmareShardVolley extends ModelEngineAttack {
        public NightmareShardVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_shard_volley", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(80);
            config.setCooldownTicks(90);
        }
        @Override protected String getModelId() { return "nightmare_shard_volley"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 0.9f, 1.2f);
            purpleDust(c, 12, 1.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 10 == 0) { damageNearby(c, 2.0, 4.0); purpleDust(c, 4, 1.5); }
        }
        @Override public AbstractAttack newInstance() { return new NightmareShardVolley(plugin); }
    }

    // ================================================================
    // 11. DEVIL'S TONGUE BEAM — Wide flat beam, sustained channel.
    // ================================================================
    public static class DevilsTongueBeam extends ModelEngineAttack {
        public DevilsTongueBeam(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_tongue_beam", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(10);
            config.setDurationTicks(180);
            config.setCooldownTicks(180);
        }
        @Override protected String getModelId() { return "devils_tongue_beam"; }
        @Override protected double getModelScale() { return 3.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            hostileHurt(c, 1.2f, 0.6f);
            bloodDust(c, 20, 2.0);
            sicklyDust(c, 15, 2.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 10 == 0) { damageNearby(c, 3.0, 4.5); bloodDust(c, 5, 3.0); }
            if (tick % 30 == 0) hostileHurt(c, 0.7f, 0.7f);
        }
        @Override public AbstractAttack newInstance() { return new DevilsTongueBeam(plugin); }
    }

    // ================================================================
    // 12. SILVER WING BLADE — Single feather blade projectile.
    // ================================================================
    public static class SilverWingBlade extends ModelEngineAttack {
        public SilverWingBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("silver_wing_blade", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(14.0);
            config.setDamageRadius(2.0);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(100);
            config.setCooldownTicks(90);
        }
        @Override protected String getModelId() { return "silver_wing_blade"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 1.1f, 1.4f);
            boneDust(c, 15, 1.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 15 == 0) damageNearby(c, 2.0, 7.0);
        }
        @Override public AbstractAttack newInstance() { return new SilverWingBlade(plugin); }
    }

    // ================================================================
    // 13. NIGHTMARE EYE PROJECTILE — Floating eye with tendrils.
    // ================================================================
    public static class NightmareEyeProjectile extends ModelEngineAttack {
        public NightmareEyeProjectile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_eye_projectile", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(120);
            config.setCooldownTicks(110);
        }
        @Override protected String getModelId() { return "nightmare_eye_projectile"; }
        @Override protected double getModelScale() { return 5.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            sculkSoul(c, 20, 1.5);
            purpleDust(c, 15, 1.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 3.0, 5.0); sculkSoul(c, 5, 2.0); }
            if (tick % 30 == 0) caveAmb(c, 0.6f, 0.8f);
        }
        @Override public AbstractAttack newInstance() { return new NightmareEyeProjectile(plugin); }
    }

    // ================================================================
    // 14. BLOOD COMET — Heavy projectile with comet tail.
    // ================================================================
    public static class BloodComet extends ModelEngineAttack {
        public BloodComet(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_comet", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(12.0);
            config.setDamageRadius(3.5);
            config.setTicksBetweenDamage(15);
            config.setDurationTicks(130);
            config.setCooldownTicks(120);
        }
        @Override protected String getModelId() { return "blood_comet"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            hostileHurt(c, 1.3f, 0.5f);
            bloodDust(c, 25, 2.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 15 == 0) { damageNearby(c, 3.5, 6.0); bloodDust(c, 8, 2.5); }
        }
        @Override public AbstractAttack newInstance() { return new BloodComet(plugin); }
    }

    // ================================================================
    // 15. FALLEN ANGEL WINGS SUMMON — Wings flanking caster.
    // ================================================================
    public static class FallenAngelWingsSummon extends ModelEngineAttack {
        public FallenAngelWingsSummon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fallen_angel_wings_summon", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }
        @Override protected String getModelId() { return "fallen_angel_wings_summon"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 1.2f, 0.6f);
            anchorCharge(c, 1.0f, 0.5f);
            boneDust(c, 30, 4.0);
            purpleDust(c, 20, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 25 == 0) { damageNearby(c, 12.0, 4.5); boneDust(c, 8, 6.0); }
            if (tick % 60 == 0) chime(c, 0.5f, 0.5f);
        }
        @Override public AbstractAttack newInstance() { return new FallenAngelWingsSummon(plugin); }
    }

    // ================================================================
    // 16. NIGHTMARE CATHEDRAL — Massive arch around caster.
    // ================================================================
    public static class NightmareCathedral extends ModelEngineAttack {
        public NightmareCathedral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_cathedral", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(6.0);
            config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(30);
            config.setDurationTicks(360);
            config.setCooldownTicks(320);
        }
        @Override protected String getModelId() { return "nightmare_cathedral"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.4f, 0.3f);
            wartPlace(c, 1.0f, 0.5f);
            purpleDust(c, 35, 5.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 30 == 0) { damageNearby(c, 10.0, 3.5); purpleDust(c, 10, 6.0); }
            if (tick % 80 == 0) caveAmb(c, 0.5f, 0.3f);
        }
        @Override public AbstractAttack newInstance() { return new NightmareCathedral(plugin); }
    }

    // ================================================================
    // 17. DEVIL'S HALO ARRAY — 7 orbiting halos.
    // ================================================================
    public static class DevilsHaloArray extends ModelEngineAttack {
        public DevilsHaloArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_halo_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(260);
        }
        @Override protected String getModelId() { return "devils_halo_array"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            chime(c, 1.3f, 0.7f);
            boneDust(c, 30, 4.0);
            bloodDust(c, 15, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 8.0, 5.0); boneDust(c, 8, 5.0); }
            if (tick % 50 == 0) chime(c, 0.6f, 0.9f);
        }
        @Override public AbstractAttack newInstance() { return new DevilsHaloArray(plugin); }
    }

    // ================================================================
    // 18. BONE THRONE SUMMON — Throne of compacted bones.
    // ================================================================
    public static class BoneThroneSummon extends ModelEngineAttack {
        public BoneThroneSummon(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("bone_throne_summon", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(8.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }
        @Override protected String getModelId() { return "bone_throne_summon"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.3f, 0.4f);
            boneDust(c, 30, 3.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 25 == 0) { damageNearby(c, 6.0, 4.5); boneDust(c, 8, 4.0); }
            if (tick % 80 == 0) caveAmb(c, 0.5f, 0.4f);
        }
        @Override public AbstractAttack newInstance() { return new BoneThroneSummon(plugin); }
    }

    // ================================================================
    // 19. SILVER MIRROR PORTAL — Standing mirror.
    // ================================================================
    public static class SilverMirrorPortal extends ModelEngineAttack {
        public SilverMirrorPortal(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("silver_mirror_portal", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(5.5);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }
        @Override protected String getModelId() { return "silver_mirror_portal"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            portalAmb(c, 1.3f, 0.6f);
            chime(c, 0.9f, 1.0f);
            purpleDust(c, 25, 3.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 25 == 0) { damageNearby(c, 5.5, 5.0); purpleDust(c, 8, 4.0); }
            if (tick % 60 == 0) portalAmb(c, 0.6f, 0.7f);
        }
        @Override public AbstractAttack newInstance() { return new SilverMirrorPortal(plugin); }
    }

    // ================================================================
    // 20. DEVIL'S CONSTELLATION — 8 stars in irregular pattern.
    // ================================================================
    public static class DevilsConstellation extends ModelEngineAttack {
        public DevilsConstellation(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("devils_constellation", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(11.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }
        @Override protected String getModelId() { return "devils_constellation"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            thunderTrident(c, 1.0f, 0.7f);
            bloodDust(c.clone().add(0, 8, 0), 35, 6.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 12.0, 5.5); bloodDust(c.clone().add(0, 8, 0), 10, 8.0); }
            if (tick % 50 == 0) chime(c, 0.7f, 0.5f);
        }
        @Override public AbstractAttack newInstance() { return new DevilsConstellation(plugin); }
    }

    // ================================================================
    // 21. NIGHTMARE CLOCK — Time corrupted.
    // ================================================================
    public static class NightmareClock extends ModelEngineAttack {
        public NightmareClock(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_clock", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(7.0);
            config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(320);
            config.setCooldownTicks(300);
        }
        @Override protected String getModelId() { return "nightmare_clock"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.0f, 0.6f);
            boneDust(c, 25, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 6.0, 3.5); boneDust(c, 6, 4.0); }
            if (tick % 40 == 0) chime(c, 0.5f, 0.4f);
        }
        @Override public AbstractAttack newInstance() { return new NightmareClock(plugin); }
    }

    // ================================================================
    // 22. FALLEN SERAPH SKELETON — Partial divine skeleton.
    // ================================================================
    public static class FallenSeraphSkeleton extends ModelEngineAttack {
        public FallenSeraphSkeleton(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fallen_seraph_skeleton", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
        }
        @Override protected String getModelId() { return "fallen_seraph_skeleton"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            anchorCharge(c, 1.3f, 0.4f);
            chime(c, 0.8f, 0.6f);
            boneDust(c, 35, 4.0);
            purpleDust(c, 15, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 8.0, 5.0); boneDust(c, 10, 5.0); }
            if (tick % 60 == 0) caveAmb(c, 0.5f, 0.3f);
        }
        @Override public AbstractAttack newInstance() { return new FallenSeraphSkeleton(plugin); }
    }

    // ================================================================
    // 23. NIGHTMARE PLANETARIUM — Wrong-sky orrery.
    // ================================================================
    public static class NightmarePlanetarium extends ModelEngineAttack {
        public NightmarePlanetarium(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_planetarium", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(10.0);
            config.setDamageRadius(14.0);
            config.setTicksBetweenDamage(25);
            config.setDurationTicks(280);
            config.setCooldownTicks(260);
        }
        @Override protected String getModelId() { return "nightmare_planetarium"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            portalAmb(c, 1.2f, 0.5f);
            sculkSpread(c, 1.0f, 0.6f);
            purpleDust(c, 30, 6.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 25 == 0) { damageNearby(c, 14.0, 5.0); purpleDust(c, 12, 8.0); }
            if (tick % 60 == 0) portalAmb(c, 0.6f, 0.4f);
        }
        @Override public AbstractAttack newInstance() { return new NightmarePlanetarium(plugin); }
    }

    // ================================================================
    // 24. INFERNAL SCRIPTURE ARRAY — Floating pages around lectern.
    // ================================================================
    public static class InfernalScriptureArray extends ModelEngineAttack {
        public InfernalScriptureArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("infernal_scripture_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(9.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(260);
            config.setCooldownTicks(240);
        }
        @Override protected String getModelId() { return "infernal_scripture_array"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            wartPlace(c, 1.0f, 0.7f);
            purpleDust(c, 25, 4.0);
            sicklyDust(c, 15, 3.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) { damageNearby(c, 9.0, 4.5); purpleDust(c, 8, 5.0); }
            if (tick % 50 == 0) chime(c, 0.6f, 0.6f);
        }
        @Override public AbstractAttack newInstance() { return new InfernalScriptureArray(plugin); }
    }

    // ================================================================
    // 25. THE DREAM ITSELF — Finale. Massive composite. Most dangerous.
    // ================================================================
    public static class TheDreamItself extends ModelEngineAttack {
        public TheDreamItself(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_dream_itself", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDamage(20.0);
            config.setDamageRadius(16.0);
            config.setTicksBetweenDamage(20);
            config.setDurationTicks(400);
            config.setCooldownTicks(360);
        }
        @Override protected String getModelId() { return "the_dream_itself"; }
        @Override protected double getModelScale() { return 2.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            thunderTrident(c, 2.0f, 0.3f);
            anchorCharge(c, 1.6f, 0.2f);
            portalAmb(c, 1.5f, 0.4f);
            bloodDust(c, 50, 8.0);
            boneDust(c, 40, 8.0);
            sicklyDust(c, 30, 8.0);
            purpleDust(c, 60, 8.0);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null) return;
            if (tick % 20 == 0) {
                damageNearby(c, 16.0, 10.0);
                pullPlayersToward(c, 16.0, 0.10);
                bloodDust(c, 12, 10.0);
                purpleDust(c, 12, 10.0);
            }
            if (tick % 40 == 0) thunderTrident(c, 0.8f, 0.5f);
            if (tick % 60 == 0) anchorCharge(c, 0.6f, 0.3f);
        }
        @Override public AbstractAttack newInstance() { return new TheDreamItself(plugin); }
    }
}
