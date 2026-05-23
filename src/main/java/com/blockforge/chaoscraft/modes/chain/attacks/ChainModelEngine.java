package com.blockforge.chaoscraft.modes.chain.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.AbstractAttack;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackConfig;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackRegistry;
import com.blockforge.chaoscraft.modes.calamity.attacks.AttackType;
import com.blockforge.chaoscraft.modes.calamity.attacks.ModelEngineAttack;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Chain Mode — MODELENGINE ATTACKS
 *
 * 25 ME VFX attacks themed around chains / industrial iron / cursed bindings
 * / spectral ghost iron. Three texture palettes:
 *   - Heavy-rust industrial (iron-black + rust orange)
 *   - Cursed-teal (iron + purple/teal emissive)
 *   - Spectral pale-blue (ghost iron + silver-blue glow)
 *
 *  Ground Eruption  (1) anchor_eruption, (2) chain_spike_array,
 *                   (3) cursed_shackle_burst, (4) spectral_chain_forest,
 *                   (5) iron_maiden_ground_trap
 *  AOE Burst        (6) chain_shockwave_ring, (7) void_chain_nova,
 *                   (8) spectral_binding_shockwave, (9) iron_crown_burst,
 *                   (10) phantom_chain_collapse
 *  Projectile       (11) flail_projectile, (12) cursed_chain_laser,
 *                   (13) ghost_chain_volley, (14) chain_comet_me,
 *                   (15) spectral_chain_tendril
 *  Summoning/Orbital(16) chain_colossus_hands, (17) cursed_prison_orbital,
 *                   (18) spectral_warden_orbital, (19) iron_wheel_orbital,
 *                   (20) void_chain_anchor_ring, (21) chain_orrery,
 *                   (22) ghost_ship_anchor_array, (23) cursed_pendulum_array,
 *                   (24) spectral_chain_gallery, (25) the_chain
 *
 * Particle palette:
 *   - industrialDust   #A06030 (iron-orange/rust)
 *   - cursedTealDust   #30D8B0 (cursed teal emissive)
 *   - spectralBlueDust #A0C0E0 (spectral pale blue)
 *   - BLOCK with IRON_BLOCK BlockData
 *   - ELECTRIC_SPARK, SOUL_FIRE_FLAME (cursed), END_ROD (spectral)
 *
 * Sounds: BLOCK_CHAIN_PLACE, BLOCK_CHAIN_BREAK, BLOCK_CHAIN_HIT,
 *   BLOCK_ANVIL_LAND, BLOCK_ANVIL_PLACE, BLOCK_NETHERITE_BLOCK_HIT,
 *   ENTITY_IRON_GOLEM_ATTACK, BLOCK_GLASS_BREAK,
 *   ENTITY_WITHER_SPAWN, ENTITY_GENERIC_EXPLODE.
 *
 * Per user rule: modelengine-scale ALWAYS matches damage-radius (or
 * impact-radius for impact-only) numerically, so default config has the
 * model visually scale to its hitbox.
 *
 * All 25 bbmodel files live in src/main/resources/models/chain/me_attacks/.
 */
public final class ChainModelEngine {
    private ChainModelEngine() {}

    private static final String MODE_PATH = "modes/chain/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        // Ground Eruption
        registry.register(new AnchorEruption(plugin));
        registry.register(new ChainSpikeArray(plugin));
        registry.register(new CursedShackleBurst(plugin));
        registry.register(new SpectralChainForest(plugin));
        registry.register(new IronMaidenGroundTrap(plugin));
        // AOE Burst / Shockwaves
        registry.register(new ChainShockwaveRing(plugin));
        registry.register(new VoidChainNova(plugin));
        registry.register(new SpectralBindingShockwave(plugin));
        registry.register(new IronCrownBurst(plugin));
        registry.register(new PhantomChainCollapse(plugin));
        // Projectile
        registry.register(new FlailProjectile(plugin));
        registry.register(new CursedChainLaser(plugin));
        registry.register(new GhostChainVolley(plugin));
        registry.register(new ChainCometMe(plugin));
        registry.register(new SpectralChainTendril(plugin));
        // Summoning / Orbital
        registry.register(new ChainColossusHands(plugin));
        registry.register(new CursedPrisonOrbital(plugin));
        registry.register(new SpectralWardenOrbital(plugin));
        registry.register(new IronWheelOrbital(plugin));
        registry.register(new VoidChainAnchorRing(plugin));
        registry.register(new ChainOrrery(plugin));
        registry.register(new GhostShipAnchorArray(plugin));
        registry.register(new CursedPendulumArray(plugin));
        registry.register(new SpectralChainGallery(plugin));
        registry.register(new TheChain(plugin));
    }

    // ============================================================
    // Shared chain-palette particle helpers.
    // ============================================================

    /** Iron-orange/rust dust — industrial chain palette. */
    private static void industrialDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xA0, 0x60, 0x30), 1.5f));
    }

    /** Cursed teal dust — for cursed-variant attacks. */
    private static void cursedTealDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0x30, 0xD8, 0xB0), 1.5f));
    }

    /** Spectral pale-blue dust — for spectral/ghost-variant attacks. */
    private static void spectralBlueDust(Location c, int count, double spread) {
        if (c == null || c.getWorld() == null) return;
        c.getWorld().spawnParticle(Particle.DUST, c, count, spread, spread, spread, 0,
                new Particle.DustOptions(Color.fromRGB(0xA0, 0xC0, 0xE0), 1.5f));
    }

    // ============================================================
    // 1. ANCHOR ERUPTION — heavy anchor slam, impact-only
    // ============================================================
    public static class AnchorEruption extends ModelEngineAttack {
        public AnchorEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("anchor_eruption", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Ground eruption — heavy anchor slam (sidestep wide radius)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(480.0); config.setImpactRadius(11.0);
            config.setDurationTicks(140); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(11.0));
        }
        @Override protected String getModelId() { return "anchor_eruption"; }
        @Override protected double getModelScale() { return 11.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.5f);
            industrialDust(c.clone().add(0, 1, 0), 60, 4);
            c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.5, 0), 30, 3, 1, 3,
                    Material.IRON_BLOCK.createBlockData());
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 22 && tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1, 0), 8, 3, 1, 3, 0.04);
            }
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.5f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1, 0), 80, 6, 2, 6,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c.clone().add(0, 1, 0), 80, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new AnchorEruption(plugin); }
    }

    // ============================================================
    // 2. CHAIN SPIKE ARRAY — spike field eruption, constant damage
    // ============================================================
    public static class ChainSpikeArray extends ModelEngineAttack {
        public ChainSpikeArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_spike_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Spike field eruption (find safe cell)");
            config.setDamage(320.0); config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(25);
            config.setDurationTicks(160); config.setCooldownTicks(220);
            config.setModelengineScale(String.valueOf(9.0));
        }
        @Override protected String getModelId() { return "chain_spike_array"; }
        @Override protected double getModelScale() { return 9.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.3f, 0.5f);
            industrialDust(c.clone().add(0, 0.5, 0), 50, 5);
            c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 0.5, 0), 25, 4, 1, 4,
                    Material.IRON_BLOCK.createBlockData());
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c.clone().add(0, 1, 0), 10, 7, 1.5, 7, 0.05);
            }
            if (tick % 12 == 0) {
                industrialDust(c.clone().add(0, 1, 0), 25, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new ChainSpikeArray(plugin); }
    }

    // ============================================================
    // 3. CURSED SHACKLE BURST — cursed rune circle, impact-only
    // ============================================================
    public static class CursedShackleBurst extends ModelEngineAttack {
        public CursedShackleBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_shackle_burst", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Cursed eruption (escape rune circle)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(380.0); config.setImpactRadius(8.5);
            config.setDurationTicks(120); config.setCooldownTicks(220);
            config.setModelengineScale(String.valueOf(8.5));
        }
        @Override protected String getModelId() { return "cursed_shackle_burst"; }
        @Override protected double getModelScale() { return 8.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.2f, 0.6f);
            cursedTealDust(c.clone().add(0, 0.6, 0), 60, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 20 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 0.5, 0), 8, 4, 0.3, 4, 0.03);
            }
            if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1, 0), 90, 5, 2, 5, 0.08);
                cursedTealDust(c.clone().add(0, 1, 0), 80, 5);
            }
        }
        @Override public AbstractAttack newInstance() { return new CursedShackleBurst(plugin); }
    }

    // ============================================================
    // 4. SPECTRAL CHAIN FOREST — hanging spectral chains, constant
    // ============================================================
    public static class SpectralChainForest extends ModelEngineAttack {
        public SpectralChainForest(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_chain_forest", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Hanging chain forest (thread between chains)");
            config.setDamage(280.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(6); config.setDamageDelayTicks(25);
            config.setDurationTicks(200); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(10.0));
        }
        @Override protected String getModelId() { return "spectral_chain_forest"; }
        @Override protected double getModelScale() { return 10.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.3f, 0.6f);
            spectralBlueDust(c.clone().add(0, 3, 0), 70, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 8 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 3, 0), 14, 8, 2.5, 8, 0.03);
            }
            if (tick % 14 == 0) {
                spectralBlueDust(c.clone().add(0, 3, 0), 30, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new SpectralChainForest(plugin); }
    }

    // ============================================================
    // 5. IRON MAIDEN GROUND TRAP — closing trap, impact-only
    // ============================================================
    public static class IronMaidenGroundTrap extends ModelEngineAttack {
        public IronMaidenGroundTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_maiden_ground_trap", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Closing iron maiden trap (escape before lock)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(500.0); config.setImpactRadius(6.0);
            config.setDurationTicks(140); config.setCooldownTicks(280);
            config.setModelengineScale(String.valueOf(6.0));
        }
        @Override protected String getModelId() { return "iron_maiden_ground_trap"; }
        @Override protected double getModelScale() { return 6.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.5f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.5f);
            industrialDust(c.clone().add(0, 1, 0), 55, 3);
            c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1, 0), 25, 2.5, 1, 2.5,
                    Material.IRON_BLOCK.createBlockData());
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Closing warning particles
            if (tick > 6 && tick < 28 && tick % 3 == 0) {
                double r = 5.0 - (tick * 0.15);
                if (r > 0.5) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.PI * 2 * i / 8;
                        Location p = c.clone().add(Math.cos(a) * r, 0.8, Math.sin(a) * r);
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.1, 0.1, 0.1, 0.04);
                    }
                }
            }
            if (tick == 28) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.8f, 0.3f);
                DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1, 0), 80, 3.5, 2, 3.5,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c.clone().add(0, 1, 0), 80, 4);
            }
        }
        @Override public AbstractAttack newInstance() { return new IronMaidenGroundTrap(plugin); }
    }

    // ============================================================
    // 6. CHAIN SHOCKWAVE RING — rattling shockwave ring
    // ============================================================
    public static class ChainShockwaveRing extends ModelEngineAttack {
        public ChainShockwaveRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_shockwave_ring", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Rattling shockwave ring (timing dodge)");
            config.setDamage(320.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(12);
            config.setDurationTicks(110); config.setCooldownTicks(200);
            config.setModelengineScale(String.valueOf(10.0));
        }
        @Override protected String getModelId() { return "chain_shockwave_ring"; }
        @Override protected double getModelScale() { return 10.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.3f, 0.6f);
            industrialDust(c.clone().add(0, 1, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                double r = 2 + tick * 0.15;
                for (int i = 0; i < 24; i++) {
                    double a = Math.PI * 2 * i / 24;
                    Location p = c.clone().add(Math.cos(a) * r, 1, Math.sin(a) * r);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick % 12 == 0) {
                industrialDust(c.clone().add(0, 1, 0), 28, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new ChainShockwaveRing(plugin); }
    }

    // ============================================================
    // 7. VOID CHAIN NOVA — expanding void nova, impact-only
    // ============================================================
    public static class VoidChainNova extends ModelEngineAttack {
        public VoidChainNova(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_chain_nova", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Void nova (sprint out of expanding ring)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(460.0); config.setImpactRadius(11.0);
            config.setDurationTicks(130); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(11.0));
        }
        @Override protected String getModelId() { return "void_chain_nova"; }
        @Override protected double getModelScale() { return 11.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.5f);
            cursedTealDust(c.clone().add(0, 1, 0), 60, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 24 && tick % 2 == 0) {
                double r = (tick * 0.4);
                for (int i = 0; i < 16; i++) {
                    double a = Math.PI * 2 * i / 16;
                    Location p = c.clone().add(Math.cos(a) * r, 1, Math.sin(a) * r);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.1, 0.1, 0.1, 0.02);
                }
            }
            if (tick == 24) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1, 0), 100, 7, 2, 7, 0.1);
                cursedTealDust(c.clone().add(0, 1, 0), 90, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new VoidChainNova(plugin); }
    }

    // ============================================================
    // 8. SPECTRAL BINDING SHOCKWAVE — spectral binding ring
    // ============================================================
    public static class SpectralBindingShockwave extends ModelEngineAttack {
        public SpectralBindingShockwave(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_binding_shockwave", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Spectral binding ring (escape before fully bound)");
            config.setDamage(260.0); config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(7); config.setDamageDelayTicks(20);
            config.setDurationTicks(160); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(10.5));
        }
        @Override protected String getModelId() { return "spectral_binding_shockwave"; }
        @Override protected double getModelScale() { return 10.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.6f);
            spectralBlueDust(c.clone().add(0, 1, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.1;
                for (int i = 0; i < 12; i++) {
                    double a = phase + Math.PI * 2 * i / 12;
                    Location p = c.clone().add(Math.cos(a) * 9, 1.2, Math.sin(a) * 9);
                    c.getWorld().spawnParticle(Particle.END_ROD, p, 3, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 14 == 0) {
                spectralBlueDust(c.clone().add(0, 1.5, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new SpectralBindingShockwave(plugin); }
    }

    // ============================================================
    // 9. IRON CROWN BURST — crown burst with radial spikes
    // ============================================================
    public static class IronCrownBurst extends ModelEngineAttack {
        public IronCrownBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_crown_burst", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Crown burst (sidestep wide radial spikes)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(440.0); config.setImpactRadius(9.5);
            config.setDurationTicks(120); config.setCooldownTicks(220);
            config.setModelengineScale(String.valueOf(9.5));
        }
        @Override protected String getModelId() { return "iron_crown_burst"; }
        @Override protected double getModelScale() { return 9.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.7f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, 0.5f);
            industrialDust(c.clone().add(0, 2, 0), 55, 4);
            c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 2, 0), 25, 3, 1, 3,
                    Material.IRON_BLOCK.createBlockData());
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 22 && tick % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    Location arm = c.clone().add(Math.cos(a) * 5, 1.5, Math.sin(a) * 5);
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, arm, 4, 0.3, 0.3, 0.3, 0.03);
                }
            }
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.4f, 0.5f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1.5, 0), 80, 6, 2, 6,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c.clone().add(0, 1.5, 0), 80, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new IronCrownBurst(plugin); }
    }

    // ============================================================
    // 10. PHANTOM CHAIN COLLAPSE — inward collapse, constant damage
    // ============================================================
    public static class PhantomChainCollapse extends ModelEngineAttack {
        public PhantomChainCollapse(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("phantom_chain_collapse", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Phantom inward collapse (sprint outward)");
            config.setDamage(300.0); config.setDamageRadius(11.0);
            config.setTicksBetweenDamage(6); config.setDamageDelayTicks(18);
            config.setDurationTicks(150); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(11.0));
        }
        @Override protected String getModelId() { return "phantom_chain_collapse"; }
        @Override protected double getModelScale() { return 11.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.6f);
            spectralBlueDust(c.clone().add(0, 2, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                double r = Math.max(1.5, 10 - tick * 0.07);
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12;
                    Location p = c.clone().add(Math.cos(a) * r, 1.5, Math.sin(a) * r);
                    c.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 12 == 0) {
                spectralBlueDust(c.clone().add(0, 1.5, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new PhantomChainCollapse(plugin); }
    }

    // ============================================================
    // 11. FLAIL PROJECTILE — read arc + sidestep
    // ============================================================
    public static class FlailProjectile extends ModelEngineAttack {
        public FlailProjectile(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("flail_projectile", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Flail projectile (read arc + sidestep)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(380.0); config.setImpactRadius(5.0);
            config.setDurationTicks(100); config.setCooldownTicks(180);
            config.setModelengineScale(String.valueOf(5.0));
        }
        @Override protected String getModelId() { return "flail_projectile"; }
        @Override protected double getModelScale() { return 5.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.2f, 0.6f);
            industrialDust(c, 35, 2.5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 2 && tick < 18 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 6, 0.5, 0.5, 0.5, 0.04);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 4, 0.4, 0.4, 0.4,
                        Material.IRON_BLOCK.createBlockData());
            }
            if (tick == 18) {
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.6f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 60, 3, 1.5, 3,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c, 50, 4);
            }
        }
        @Override public AbstractAttack newInstance() { return new FlailProjectile(plugin); }
    }

    // ============================================================
    // 12. CURSED CHAIN LASER — narrow beam, sprint perpendicular
    // ============================================================
    public static class CursedChainLaser extends ModelEngineAttack {
        public CursedChainLaser(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_chain_laser", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Beam laser (sprint perpendicular)");
            config.setDamage(360.0); config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(4); config.setDamageDelayTicks(15);
            config.setDurationTicks(100); config.setCooldownTicks(200);
            config.setModelengineScale(String.valueOf(4.0));
        }
        @Override protected String getModelId() { return "cursed_chain_laser"; }
        @Override protected double getModelScale() { return 4.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 1.4f, 0.6f);
            cursedTealDust(c.clone().add(0, 1.5, 0), 50, 3);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, c.clone().add(0, 1.5, 0), 10, 3, 1, 3, 0.04);
            }
            if (tick % 8 == 0) {
                cursedTealDust(c.clone().add(0, 1.5, 0), 18, 3);
            }
        }
        @Override public AbstractAttack newInstance() { return new CursedChainLaser(plugin); }
    }

    // ============================================================
    // 13. GHOST CHAIN VOLLEY — 13-link spread, find gap
    // ============================================================
    public static class GhostChainVolley extends ModelEngineAttack {
        public GhostChainVolley(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_chain_volley", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("13-volley spread (find gap between 13)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(200.0); config.setImpactRadius(3.0);
            config.setDurationTicks(120); config.setCooldownTicks(200);
            config.setModelengineScale(String.valueOf(3.0));
        }
        @Override protected String getModelId() { return "ghost_chain_volley"; }
        @Override protected double getModelScale() { return 3.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.2f, 0.7f);
            spectralBlueDust(c, 40, 3);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 22 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c, 10, 2, 1, 2, 0.04);
            }
            if (tick == 22) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.5f, 0.6f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1, 0), 70, 4, 2, 4, 0.08);
                spectralBlueDust(c.clone().add(0, 1, 0), 60, 4);
            }
        }
        @Override public AbstractAttack newInstance() { return new GhostChainVolley(plugin); }
    }

    // ============================================================
    // 14. CHAIN COMET ME — comet impact, clear large radius
    // ============================================================
    public static class ChainCometMe extends ModelEngineAttack {
        public ChainCometMe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_comet_me", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Comet impact (clear large radius)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(500.0); config.setImpactRadius(9.0);
            config.setDurationTicks(130); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(9.0));
        }
        @Override protected String getModelId() { return "chain_comet_me"; }
        @Override protected double getModelScale() { return 9.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.5f);
            industrialDust(c, 60, 4);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 28 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, c, 10, 0.5, 0.5, 0.5, 0.05);
                c.getWorld().spawnParticle(Particle.BLOCK, c, 6, 0.5, 0.5, 0.5,
                        Material.IRON_BLOCK.createBlockData());
            }
            if (tick == 28) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.9f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.7f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1, 0), 110, 5, 3, 5,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c.clone().add(0, 1, 0), 90, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new ChainCometMe(plugin); }
    }

    // ============================================================
    // 15. SPECTRAL CHAIN TENDRIL — reaching tendril claw
    // ============================================================
    public static class SpectralChainTendril extends ModelEngineAttack {
        public SpectralChainTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_chain_tendril", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Reaching tendril (sidestep claw grab)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(360.0); config.setImpactRadius(5.0);
            config.setDurationTicks(110); config.setCooldownTicks(200);
            config.setModelengineScale(String.valueOf(5.0));
        }
        @Override protected String getModelId() { return "spectral_chain_tendril"; }
        @Override protected double getModelScale() { return 5.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.7f);
            spectralBlueDust(c.clone().add(0, 1.5, 0), 45, 3);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 4 && tick < 20 && tick % 2 == 0) {
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0), 8, 1.5, 0.5, 1.5, 0.03);
            }
            if (tick == 20) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.6f, 0.5f);
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.6f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.END_ROD, c.clone().add(0, 1.5, 0), 70, 3, 1.5, 3, 0.1);
                spectralBlueDust(c.clone().add(0, 1.5, 0), 60, 4);
            }
        }
        @Override public AbstractAttack newInstance() { return new SpectralChainTendril(plugin); }
    }

    // ============================================================
    // 16. CHAIN COLOSSUS HANDS — colossus slam, read shadow
    // ============================================================
    public static class ChainColossusHands extends ModelEngineAttack {
        public ChainColossusHands(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_colossus_hands", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Colossus slam (read shadow, sprint clear)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(480.0); config.setImpactRadius(8.5);
            config.setDurationTicks(160); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(8.5));
        }
        @Override protected String getModelId() { return "chain_colossus_hands"; }
        @Override protected double getModelScale() { return 8.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
            industrialDust(c.clone().add(0, 4, 0), 70, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick > 6 && tick < 30 && tick % 3 == 0) {
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 4 - tick * 0.1, 0), 12, 3, 0.5, 3,
                        Material.IRON_BLOCK.createBlockData());
            }
            if (tick == 30) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.8f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.4f);
                triggerImpactDamage(c);
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 1, 0), 90, 5, 2, 5,
                        Material.IRON_BLOCK.createBlockData());
                industrialDust(c.clone().add(0, 1, 0), 80, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new ChainColossusHands(plugin); }
    }

    // ============================================================
    // 17. CURSED PRISON ORBITAL — cursed prison enclosure
    // ============================================================
    public static class CursedPrisonOrbital extends ModelEngineAttack {
        public CursedPrisonOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_prison_orbital", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Cursed prison enclosure (escape before lock)");
            config.setDamage(340.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(25);
            config.setDurationTicks(200); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(8.0));
        }
        @Override protected String getModelId() { return "cursed_prison_orbital"; }
        @Override protected double getModelScale() { return 8.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.7f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.5f);
            cursedTealDust(c.clone().add(0, 2.5, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.08;
                for (int i = 0; i < 6; i++) {
                    double a = phase + Math.PI * 2 * i / 6;
                    Location p = c.clone().add(Math.cos(a) * 7, 2.5, Math.sin(a) * 7);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick % 14 == 0) {
                cursedTealDust(c.clone().add(0, 2, 0), 30, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new CursedPrisonOrbital(plugin); }
    }

    // ============================================================
    // 18. SPECTRAL WARDEN ORBITAL — spectral warden patrol
    // ============================================================
    public static class SpectralWardenOrbital extends ModelEngineAttack {
        public SpectralWardenOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_warden_orbital", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Spectral warden orbit (read patrol path)");
            config.setDamage(280.0); config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(6); config.setDamageDelayTicks(25);
            config.setDurationTicks(200); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(9.0));
        }
        @Override protected String getModelId() { return "spectral_warden_orbital"; }
        @Override protected double getModelScale() { return 9.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.6f);
            spectralBlueDust(c.clone().add(0, 3, 0), 60, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                double phase = tick * 0.12;
                Location pos = c.clone().add(Math.cos(phase) * 8, 3 + Math.sin(phase * 1.7) * 0.6, Math.sin(phase) * 8);
                c.getWorld().spawnParticle(Particle.END_ROD, pos, 6, 0.4, 0.4, 0.4, 0.03);
                spectralBlueDust(pos, 8, 0.5);
            }
            if (tick % 14 == 0) {
                spectralBlueDust(c.clone().add(0, 3, 0), 25, 8);
            }
        }
        @Override public AbstractAttack newInstance() { return new SpectralWardenOrbital(plugin); }
    }

    // ============================================================
    // 19. IRON WHEEL ORBITAL — breaking wheel orbit
    // ============================================================
    public static class IronWheelOrbital extends ModelEngineAttack {
        public IronWheelOrbital(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_wheel_orbital", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Breaking wheel orbit (timing dodge between wheel)");
            config.setDamage(360.0); config.setDamageRadius(7.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(20);
            config.setDurationTicks(180); config.setCooldownTicks(240);
            config.setModelengineScale(String.valueOf(7.0));
        }
        @Override protected String getModelId() { return "iron_wheel_orbital"; }
        @Override protected double getModelScale() { return 7.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, 0.6f);
            industrialDust(c.clone().add(0, 2.5, 0), 50, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 5 == 0) {
                double phase = tick * 0.18;
                for (int i = 0; i < 8; i++) {
                    double a = phase + Math.PI * 2 * i / 8;
                    Location p = c.clone().add(Math.cos(a) * 6, 2.5, Math.sin(a) * 6);
                    c.getWorld().spawnParticle(Particle.BLOCK, p, 3, 0.3, 0.3, 0.3,
                            Material.IRON_BLOCK.createBlockData());
                    c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.2, 0.2, 0.2, 0.02);
                }
            }
            if (tick % 12 == 0) {
                industrialDust(c.clone().add(0, 2, 0), 28, 6);
            }
        }
        @Override public AbstractAttack newInstance() { return new IronWheelOrbital(plugin); }
    }

    // ============================================================
    // 20. VOID CHAIN ANCHOR RING — mooring ring before anchor drops
    // ============================================================
    public static class VoidChainAnchorRing extends ModelEngineAttack {
        public VoidChainAnchorRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_chain_anchor_ring", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Mooring ring (escape ring before anchor drops)");
            config.setDamage(300.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(25);
            config.setDurationTicks(200); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(10.0));
        }
        @Override protected String getModelId() { return "void_chain_anchor_ring"; }
        @Override protected double getModelScale() { return 10.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.5f);
            cursedTealDust(c.clone().add(0, 1, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.06;
                for (int i = 0; i < 8; i++) {
                    double a = phase + Math.PI * 2 * i / 8;
                    Location p = c.clone().add(Math.cos(a) * 9, 1.2, Math.sin(a) * 9);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick % 14 == 0) {
                cursedTealDust(c.clone().add(0, 1.5, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new VoidChainAnchorRing(plugin); }
    }

    // ============================================================
    // 21. CHAIN ORRERY — multi-ring orbital pattern
    // ============================================================
    public static class ChainOrrery extends ModelEngineAttack {
        public ChainOrrery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_orrery", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Orbital orrery (read multi-ring pattern)");
            config.setDamage(320.0); config.setDamageRadius(9.5);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(25);
            config.setDurationTicks(220); config.setCooldownTicks(280);
            config.setModelengineScale(String.valueOf(9.5));
        }
        @Override protected String getModelId() { return "chain_orrery"; }
        @Override protected double getModelScale() { return 9.5; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.5f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.5f);
            industrialDust(c.clone().add(0, 3, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            if (tick % 6 == 0) {
                double phase = tick * 0.06;
                for (int ring = 0; ring < 3; ring++) {
                    double r = 4 + ring * 2.5;
                    double offset = phase * (1 + ring * 0.35);
                    for (int i = 0; i < 5; i++) {
                        double a = offset + Math.PI * 2 * i / 5;
                        Location p = c.clone().add(Math.cos(a) * r, 2.5 + ring * 0.5, Math.sin(a) * r);
                        c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 3, 0.2, 0.2, 0.2, 0.02);
                        c.getWorld().spawnParticle(Particle.BLOCK, p, 2, 0.2, 0.2, 0.2,
                                Material.IRON_BLOCK.createBlockData());
                    }
                }
            }
            if (tick % 16 == 0) {
                industrialDust(c.clone().add(0, 3, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new ChainOrrery(plugin); }
    }

    // ============================================================
    // 22. GHOST SHIP ANCHOR ARRAY — 5-anchor slam, find safe
    // ============================================================
    public static class GhostShipAnchorArray extends ModelEngineAttack {
        public GhostShipAnchorArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ghost_ship_anchor_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Ghost ship 5-anchor slam (find safe between anchors)");
            config.setDamage(0.0); config.setDamageRadius(0.0);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(460.0); config.setImpactRadius(8.0);
            config.setDurationTicks(160); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(8.0));
        }
        @Override protected String getModelId() { return "ghost_ship_anchor_array"; }
        @Override protected double getModelScale() { return 8.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.4f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.5f);
            spectralBlueDust(c.clone().add(0, 4, 0), 70, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Five descending anchors at ring positions
            if (tick > 6 && tick < 28 && tick % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double a = Math.PI * 2 * i / 5;
                    Location p = c.clone().add(Math.cos(a) * 6, 4 - tick * 0.13, Math.sin(a) * 6);
                    c.getWorld().spawnParticle(Particle.END_ROD, p, 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick == 28) {
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.7f, 0.3f);
                DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.6f, 0.4f);
                triggerImpactDamage(c);
                for (int i = 0; i < 5; i++) {
                    double a = Math.PI * 2 * i / 5;
                    Location p = c.clone().add(Math.cos(a) * 6, 1, Math.sin(a) * 6);
                    c.getWorld().spawnParticle(Particle.END_ROD, p, 20, 2, 1, 2, 0.08);
                    spectralBlueDust(p, 20, 2);
                }
            }
        }
        @Override public AbstractAttack newInstance() { return new GhostShipAnchorArray(plugin); }
    }

    // ============================================================
    // 23. CURSED PENDULUM ARRAY — 4-pendulum array
    // ============================================================
    public static class CursedPendulumArray extends ModelEngineAttack {
        public CursedPendulumArray(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("cursed_pendulum_array", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("4-pendulum array (read 4 arcs, time between swings)");
            config.setDamage(380.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(20);
            config.setDurationTicks(200); config.setCooldownTicks(260);
            config.setModelengineScale(String.valueOf(8.0));
        }
        @Override protected String getModelId() { return "cursed_pendulum_array"; }
        @Override protected double getModelScale() { return 8.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.4f, 0.5f);
            cursedTealDust(c.clone().add(0, 3.5, 0), 60, 5);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // 4 swinging pendulums
            if (tick % 4 == 0) {
                double swing = Math.sin(tick * 0.12) * 5;
                for (int i = 0; i < 4; i++) {
                    double a = Math.PI * 2 * i / 4;
                    double dx = Math.cos(a) * swing;
                    double dz = Math.sin(a) * swing;
                    Location p = c.clone().add(dx, 2, dz);
                    c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick % 14 == 0) {
                cursedTealDust(c.clone().add(0, 2, 0), 28, 7);
            }
        }
        @Override public AbstractAttack newInstance() { return new CursedPendulumArray(plugin); }
    }

    // ============================================================
    // 24. SPECTRAL CHAIN GALLERY — gallery enclosure, which is real
    // ============================================================
    public static class SpectralChainGallery extends ModelEngineAttack {
        public SpectralChainGallery(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spectral_chain_gallery", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Gallery enclosure (read which frame is real)");
            config.setDamage(240.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(6); config.setDamageDelayTicks(30);
            config.setDurationTicks(220); config.setCooldownTicks(280);
            config.setModelengineScale(String.valueOf(10.0));
        }
        @Override protected String getModelId() { return "spectral_chain_gallery"; }
        @Override protected double getModelScale() { return 10.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_GLOW_SQUID_AMBIENT, 1.5f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.3f, 0.7f);
            spectralBlueDust(c.clone().add(0, 3, 0), 70, 6);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Gallery frames around perimeter
            if (tick % 7 == 0) {
                double phase = tick * 0.04;
                for (int i = 0; i < 10; i++) {
                    double a = phase + Math.PI * 2 * i / 10;
                    Location p = c.clone().add(Math.cos(a) * 9, 2.5, Math.sin(a) * 9);
                    c.getWorld().spawnParticle(Particle.END_ROD, p, 4, 0.3, 0.3, 0.3, 0.02);
                }
            }
            if (tick % 16 == 0) {
                spectralBlueDust(c.clone().add(0, 2.5, 0), 30, 9);
            }
        }
        @Override public AbstractAttack newInstance() { return new SpectralChainGallery(plugin); }
    }

    // ============================================================
    // 25. THE CHAIN — grand finale signature, multi-phase, arena-wide
    // ============================================================
    public static class TheChain extends ModelEngineAttack {
        public TheChain(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("the_chain", AttackType.MODEL_ENGINE, 1, MODE_PATH));
            config.setDesignType("Grand finale signature (multi-phase, read each phase)");
            config.setDamage(420.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(5); config.setDamageDelayTicks(30);
            config.setDurationTicks(260); config.setCooldownTicks(360);
            config.setModelengineScale(String.valueOf(12.0));
        }
        @Override protected String getModelId() { return "the_chain"; }
        @Override protected double getModelScale() { return 12.0; }
        @Override protected void onSpawn(Location c) {
            spawnModel(c);
            DisplayBuilder.playSound(c, Sound.ENTITY_WITHER_SPAWN, 1.2f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_PLACE, 1.6f, 0.4f);
            DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.3f, 0.4f);
            DisplayBuilder.playSound(c, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.4f);
            industrialDust(c.clone().add(0, 4, 0), 90, 8);
            cursedTealDust(c.clone().add(0, 4, 0), 60, 8);
            spectralBlueDust(c.clone().add(0, 4, 0), 60, 8);
        }
        @Override protected void onTick(int tick) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;
            // Phase pulses across duration
            int phase = tick / 60; // 0..4 across 260 ticks
            // Multi-ring rotation
            if (tick % 5 == 0) {
                double rot = tick * 0.07;
                for (int ring = 0; ring < 3; ring++) {
                    double r = 5 + ring * 3;
                    int rays = 8 + ring * 2;
                    for (int i = 0; i < rays; i++) {
                        double a = rot + Math.PI * 2 * i / rays;
                        Location p = c.clone().add(Math.cos(a) * r, 1.5 + ring * 0.6, Math.sin(a) * r);
                        // Cycle palette per phase
                        if (phase % 3 == 0) {
                            c.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.1, 0.1, 0.1, 0.02);
                        } else if (phase % 3 == 1) {
                            c.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.1, 0.1, 0.1, 0.02);
                        } else {
                            c.getWorld().spawnParticle(Particle.END_ROD, p, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
            }
            // Phase transition sounds + bursts
            if (tick == 60 || tick == 120 || tick == 180 || tick == 240) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.5f, 0.4f);
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_BREAK, 1.4f, 0.5f);
                if (phase % 3 == 0) {
                    industrialDust(c.clone().add(0, 2, 0), 60, 11);
                } else if (phase % 3 == 1) {
                    cursedTealDust(c.clone().add(0, 2, 0), 60, 11);
                } else {
                    spectralBlueDust(c.clone().add(0, 2, 0), 60, 11);
                }
                c.getWorld().spawnParticle(Particle.BLOCK, c.clone().add(0, 2, 0), 80, 10, 3, 10,
                        Material.IRON_BLOCK.createBlockData());
            }
        }
        @Override public AbstractAttack newInstance() { return new TheChain(plugin); }
    }
}
