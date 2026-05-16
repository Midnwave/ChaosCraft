package com.blockforge.chaoscraft.modes.freezingice.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.ItemDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * FreezingIce Mode — ENVIRONMENTAL FX BATCH 3 (entries 21-30).
 * Ground Pattern attacks. Pure ItemDisplay + particle attacks. NO BlockDisplays.
 * Theme: runes, drifts, lattices, footprints, mirrors, blooms, ribbons,
 * meteors, storms, prisms — visually interesting from above.
 */
public final class FreezingIceEnvironmental3 {
    private FreezingIceEnvironmental3() {}

    private static final String MODE_PATH = "modes/freezingice/attacks";

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new IceRuneCircle(plugin));
        registry.register(new SnowDriftMound(plugin));
        registry.register(new CrystallineLattice(plugin));
        registry.register(new FrozenFootprintTrail(plugin));
        registry.register(new ShatteredMirrorFloor(plugin));
        registry.register(new PermafrostBloom(plugin));
        registry.register(new AuroraRibbons(plugin));
        registry.register(new IceMeteorShower(plugin));
        registry.register(new FrozenStorm(plugin));
        registry.register(new SkyShatteringPrism(plugin));
    }

    // ================================================================
    // 21. ICE RUNE CIRCLE — 12 AMETHYST_SHARD hexagram pattern on ground.
    //     Two interlocking triangles (Star of David) drawn flat on the
    //     floor. Constant damage radius 6.5, 3500hp, damage-delay 40t.
    // ================================================================
    public static class IceRuneCircle extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> runes = new ArrayList<>();
        private final double[] rAng = new double[12];
        private final double[] rR = new double[12];

        public IceRuneCircle(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_rune_circle", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(42000.0);
            config.setDamageRadius(9.75);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(3);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.0f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.2f, 0.8f);

            // Hexagram: 6 outer points (triangle 1), 6 inner points (triangle 2 inverted),
            // arranged so the two triangles interlock as a Star of David.
            // Outer hexagon vertices = 6 shards. Inner cross-points = 6 shards.
            for (int i = 0; i < 6; i++) {
                rAng[i] = Math.PI * 2 * i / 6;
                rR[i] = 5.8; // outer hexagon vertex
                Location p = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.15, Math.sin(rAng[i]) * rR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.75f, 0.75f, 0.75f).glow(180, 220, 255).interpolation(10, 0);
                runes.add(h);
                spawnedEntities.add(h.entity());
            }
            // Inner triangle cross-points (offset 30 deg, smaller radius)
            for (int i = 6; i < 12; i++) {
                rAng[i] = Math.PI * 2 * (i - 6) / 6 + Math.PI / 6;
                rR[i] = 3.2;
                Location p = c.clone().add(Math.cos(rAng[i]) * rR[i], 0.15, Math.sin(rAng[i]) * rR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 240, 255).interpolation(10, 0);
                runes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 6 AMETHYST_CLUSTER spires at each outer vertex + 4 BLUE_ICE at center cardinal cross
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 12;
                Location p = c.clone().add(Math.cos(a) * 4.5, 0.6, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.AMETHYST_CLUSTER));
                h.scale(0.55f, 0.55f, 0.55f).glow(190, 160, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 1.6, 0.2, Math.sin(a) * 1.6);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 200, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Slow rune rotation — flat on ground but spinning
            for (int i = 0; i < runes.size(); i++) {
                rAng[i] += (i < 6 ? 0.012 : -0.018);
                float bx = (float)(Math.cos(rAng[i]) * rR[i]);
                float bz = (float)(Math.sin(rAng[i]) * rR[i]);
                float scale = i < 6 ? 0.75f : 0.6f;
                runes.get(i).animateTo(
                        new Vector3f(bx - scale / 2, 0.15f, bz - scale / 2),
                        new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0),
                        new Vector3f(scale), 4);
            }

            // ENCHANT runic glow tracing the hexagram edges
            if (tick % 2 == 0) {
                // Outer triangle edges (vertices 0,2,4)
                for (int i = 0; i < 3; i++) {
                    int a = i * 2;
                    int b = (a + 2) % 6;
                    Location pa = c.clone().add(Math.cos(rAng[a]) * rR[a], 0.25, Math.sin(rAng[a]) * rR[a]);
                    Location pb = c.clone().add(Math.cos(rAng[b]) * rR[b], 0.25, Math.sin(rAng[b]) * rR[b]);
                    DisplayBuilder.particleLine(pa, pb, Particle.ENCHANT, 14, null);
                }
                // Inverted triangle edges (vertices 1,3,5)
                for (int i = 0; i < 3; i++) {
                    int a = i * 2 + 1;
                    int b = (a + 2) % 6;
                    Location pa = c.clone().add(Math.cos(rAng[a]) * rR[a], 0.25, Math.sin(rAng[a]) * rR[a]);
                    Location pb = c.clone().add(Math.cos(rAng[b]) * rR[b], 0.25, Math.sin(rAng[b]) * rR[b]);
                    DisplayBuilder.particleLine(pa, pb, Particle.ENCHANT, 14, null);
                }
            }
            // Circumscribed circle
            if (tick % 3 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 6.0, Particle.ENCHANT, 32, null);
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 3.2, Particle.SNOWFLAKE, 18, null);
            }
            // Pre-arm pulse — chime every 8 ticks during damage delay
            if (tick < 40 && tick % 8 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
                DisplayBuilder.dustParticles(c.clone().add(0, 0.3, 0), 12, 0.4, 200, 230, 255, 1.4f);
            }
            // Periodic chime after armed
            if (tick >= 40 && tick % 30 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.9f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceRuneCircle(plugin); }
    }

    // ================================================================
    // 22. SNOW DRIFT MOUND — 22 SNOWBALL dome builds (layered hemisphere)
    //     then collapses inward. Impact: radius 9.0, 4800hp.
    // ================================================================
    public static class SnowDriftMound extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> flakes = new ArrayList<>();
        private final double[] sX = new double[22];
        private final double[] sY = new double[22];
        private final double[] sZ = new double[22];
        private boolean collapsed = false;
        private static final int BUILD_TICKS = 70;
        private static final int HOLD_TICKS = 30;

        public SnowDriftMound(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("snow_drift_mound", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(57600.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(200);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(57600.0);
            config.setImpactRadius(13.5);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_PLACE, 1.2f, 0.7f);

            // Build hemisphere coordinates: 22 snowballs in a dome pattern.
            // Bottom ring (8) + middle ring (8) + upper ring (4) + apex (2)
            int idx = 0;
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                sX[idx] = Math.cos(a) * 7.0;
                sY[idx] = 0.4;
                sZ[idx] = Math.sin(a) * 7.0;
                idx++;
            }
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 8;
                sX[idx] = Math.cos(a) * 5.0;
                sY[idx] = 2.0;
                sZ[idx] = Math.sin(a) * 5.0;
                idx++;
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                sX[idx] = Math.cos(a) * 2.5;
                sY[idx] = 3.6;
                sZ[idx] = Math.sin(a) * 2.5;
                idx++;
            }
            // Apex pair
            sX[idx] = 0.0; sY[idx] = 4.6; sZ[idx] = 0.5; idx++;
            sX[idx] = 0.0; sY[idx] = 4.6; sZ[idx] = -0.5;

            // Build: spawn at floor first, then animate upward in onTick.
            for (int i = 0; i < 22; i++) {
                Location p = c.clone().add(sX[i], 0.4, sZ[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.7f, 0.7f, 0.7f).glow(240, 250, 255).interpolation(BUILD_TICKS / 8, 0);
                flakes.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 8 POWDER_SNOW_BUCKET ring around the dome's base + 6 SNOW blocks in the cracks
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 16;
                Location p = c.clone().add(Math.cos(a) * 8.0, 0.3, Math.sin(a) * 8.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POWDER_SNOW_BUCKET));
                h.scale(0.55f, 0.55f, 0.55f).glow(220, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 4.0, 1.0, Math.sin(a) * 4.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOW_BLOCK));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < BUILD_TICKS) {
                // Build phase — flakes rise into dome shape
                double t = tick / (double)BUILD_TICKS;
                for (int i = 0; i < flakes.size(); i++) {
                    float by = (float)(0.4 + (sY[i] - 0.4) * t);
                    flakes.get(i).animateTo(
                            new Vector3f((float)sX[i] - 0.35f, by, (float)sZ[i] - 0.35f),
                            new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                            new Vector3f(0.7f), 4);
                }
                // Build particles
                if (tick % 3 == 0) {
                    for (int i = 0; i < flakes.size(); i++) {
                        Location p = c.clone().add(sX[i], 0.4 + (sY[i] - 0.4) * t, sZ[i]);
                        w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.1, 0.1, 0,
                                Material.SNOW_BLOCK.createBlockData());
                    }
                }
                if (tick % 6 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_PLACE, 0.6f, 1.0f + (float)Math.random() * 0.4f);
            } else if (tick < BUILD_TICKS + HOLD_TICKS) {
                // Hold — dome sits, ominous rumble
                for (int i = 0; i < flakes.size(); i++) {
                    float bobY = (float)(sY[i] + Math.sin(tick * 0.1 + i) * 0.08);
                    flakes.get(i).animateTo(
                            new Vector3f((float)sX[i] - 0.35f, bobY, (float)sZ[i] - 0.35f),
                            new AxisAngle4f((float)(tick * 0.03 + i), 0, 1, 0),
                            new Vector3f(0.7f), 2);
                }
                if (tick % 5 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 3.0, 0), 3, 1.5, 230, 240, 255, 1.2f);
                }
                if ((tick - BUILD_TICKS) % 8 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_POWDER_SNOW_HIT, 0.7f, 0.6f);
                }
            } else if (!collapsed) {
                // Collapse — all flakes fly inward and down, then break
                collapsed = true;
                Location impact = c.clone().add(0, 0.5, 0);
                DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
                DisplayBuilder.playSound(impact, Sound.BLOCK_POWDER_SNOW_BREAK, 1.5f, 0.8f);
                DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

                for (int i = 0; i < flakes.size(); i++) {
                    flakes.get(i).animateTo(
                            new Vector3f(0 - 0.35f, 0.4f, 0 - 0.35f),
                            new AxisAngle4f((float)(tick * 0.4 + i), 0, 1, 0),
                            new Vector3f(0.7f), 8);
                }

                // Impact burst — ITEM_SNOWBALL + FALLING_DUST shower
                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                for (int i = 0; i < 60; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 9.0;
                    Location p = impact.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 2.0, Math.sin(a) * r);
                    w.spawnParticle(Particle.ITEM_SNOWBALL, p, 2, 0.2, 0.2, 0.2, 0.1);
                    w.spawnParticle(Particle.FALLING_DUST, p, 1, 0.1, 0.1, 0.1, 0,
                            Material.SNOW_BLOCK.createBlockData());
                }
                // Outer dust ring
                for (int i = 0; i < 36; i++) {
                    double a = Math.PI * 2 * i / 36;
                    Location p = impact.clone().add(Math.cos(a) * 9.0, 0.3, Math.sin(a) * 9.0);
                    DisplayBuilder.dustParticles(p, 3, 0.3, 240, 250, 255, 1.6f);
                }

                triggerImpactDamage(impact);
            } else {
                // Settled — light snow drift particles
                if (tick % 6 == 0) {
                    for (int i = 0; i < 8; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 8.0;
                        w.spawnParticle(Particle.SNOWFLAKE,
                                c.clone().add(Math.cos(a) * r, 0.5 + Math.random(), Math.sin(a) * r),
                                1, 0.1, 0.1, 0.1, 0.02);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SnowDriftMound(plugin); }
    }

    // ================================================================
    // 23. CRYSTALLINE LATTICE — 5x5 BLUE_ICE grid (25 ItemDisplays) pulses.
    //     Each cell lights up in a wave pattern. Constant radius 9.0,
    //     3200hp ticksBetween=20.
    // ================================================================
    public static class CrystallineLattice extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> cells = new ArrayList<>();
        private final int[] cellX = new int[25];
        private final int[] cellZ = new int[25];
        private static final double CELL_SPACING = 2.8;

        public CrystallineLattice(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crystalline_lattice", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(38400.0);
            config.setDamageRadius(13.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.3f, 0.9f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.0f, 1.0f);

            int idx = 0;
            for (int gx = -2; gx <= 2; gx++) {
                for (int gz = -2; gz <= 2; gz++) {
                    cellX[idx] = gx;
                    cellZ[idx] = gz;
                    Location p = c.clone().add(gx * CELL_SPACING, 0.15, gz * CELL_SPACING);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                    h.scale(0.85f, 0.15f, 0.85f).glow(140, 200, 255).interpolation(8, 0);
                    cells.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            // Decor: 8 PACKED_ICE pillars at grid corners + 4 PRISMARINE_CRYSTALS dotting cardinal edges
            int[][] corners = { {-2, -2}, {-2, 2}, {2, -2}, {2, 2}, {-2, 0}, {2, 0}, {0, -2}, {0, 2} };
            for (int[] cor : corners) {
                Location p = c.clone().add(cor[0] * CELL_SPACING, 1.0, cor[1] * CELL_SPACING);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(0.4f, 0.8f, 0.4f).glow(160, 200, 230).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 8.0, 0.5, Math.sin(a) * 8.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_CRYSTALS));
                h.scale(0.55f, 0.55f, 0.55f).glow(200, 240, 220).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Pulse wave: cells light up by Manhattan distance from origin, ticksBetween=20
            int wave = (tick / 20) % 6; // distance levels 0..4 (max Manhattan = 4)
            for (int i = 0; i < cells.size(); i++) {
                int manhattan = Math.abs(cellX[i]) + Math.abs(cellZ[i]);
                boolean pulse = manhattan == wave;
                float scaleY = pulse ? 0.6f : 0.15f;
                float by = pulse ? 0.4f : 0.15f;
                cells.get(i).animateTo(
                        new Vector3f((float)(cellX[i] * CELL_SPACING) - 0.425f, by, (float)(cellZ[i] * CELL_SPACING) - 0.425f),
                        new AxisAngle4f((float)(tick * 0.02), 0, 1, 0),
                        new Vector3f(0.85f, scaleY, 0.85f), 6);

                // END_ROD on each pulsing cell
                if (pulse && tick % 5 == 0) {
                    Location cellLoc = c.clone().add(cellX[i] * CELL_SPACING, 0.5, cellZ[i] * CELL_SPACING);
                    w.spawnParticle(Particle.END_ROD, cellLoc, 6, 0.4, 0.3, 0.4, 0.03);
                    DisplayBuilder.dustParticles(cellLoc, 4, 0.3, 160, 220, 255, 1.3f);
                }
            }

            // Staggered chime when wave advances
            if (tick % 20 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.7f + wave * 0.15f);
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.0f);
            }

            // Connecting grid lines (END_ROD trails)
            if (tick % 4 == 0) {
                for (int gx = -2; gx <= 1; gx++) {
                    Location a = c.clone().add(gx * CELL_SPACING, 0.3, -2 * CELL_SPACING);
                    Location b = c.clone().add((gx + 1) * CELL_SPACING, 0.3, -2 * CELL_SPACING);
                    DisplayBuilder.particleLine(a, b, Particle.END_ROD, 6, null);
                }
                for (int gz = -2; gz <= 1; gz++) {
                    Location a = c.clone().add(-2 * CELL_SPACING, 0.3, gz * CELL_SPACING);
                    Location b = c.clone().add(-2 * CELL_SPACING, 0.3, (gz + 1) * CELL_SPACING);
                    DisplayBuilder.particleLine(a, b, Particle.END_ROD, 6, null);
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new CrystallineLattice(plugin); }
    }

    // ================================================================
    // 24. FROZEN FOOTPRINT TRAIL — 14 ICE footprints appear sequentially
    //     wandering across the arena in a trail. Constant radius 8.0
    //     (1.5r per print), 2400hp ticksBetween=12.
    // ================================================================
    public static class FrozenFootprintTrail extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> prints = new ArrayList<>();
        private final double[] pX = new double[14];
        private final double[] pZ = new double[14];
        private final int[] pAppearTick = new int[14];
        private final boolean[] pVisible = new boolean[14];

        public FrozenFootprintTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_footprint_trail", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_SNOW_STEP, 1.0f, 0.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.6f, 1.4f);

            // Wandering path — start at random edge, curve through center
            double startAng = Math.random() * Math.PI * 2;
            double curX = Math.cos(startAng) * 6.0;
            double curZ = Math.sin(startAng) * 6.0;
            double heading = startAng + Math.PI + (Math.random() - 0.5) * 0.6;
            double stride = 0.85;

            for (int i = 0; i < 14; i++) {
                // Slight side-to-side wobble for left/right footprint pattern
                double sideOff = (i % 2 == 0 ? 0.35 : -0.35);
                double px = curX + Math.cos(heading + Math.PI / 2) * sideOff;
                double pz = curZ + Math.sin(heading + Math.PI / 2) * sideOff;
                pX[i] = px;
                pZ[i] = pz;
                pAppearTick[i] = 8 + i * 12;
                pVisible[i] = false;

                // Spawn invisible (tiny scale), grow on appear
                Location p = c.clone().add(px, 0.12, pz);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.01f, 0.01f, 0.01f).glow(180, 220, 255).interpolation(6, 0);
                prints.add(h);
                spawnedEntities.add(h.entity());

                // Advance position
                heading += (Math.random() - 0.5) * 0.4;
                curX += Math.cos(heading) * stride;
                curZ += Math.sin(heading) * stride;
            }

            // Decor: 6 SNOWBALL puffs at trailhead + 4 POWDER_SNOW_BUCKET at trail end
            for (int i = 0; i < 6; i++) {
                Location p = c.clone().add(pX[0] + (Math.random() - 0.5) * 1.0, 0.3,
                        pZ[0] + (Math.random() - 0.5) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SNOWBALL));
                h.scale(0.45f, 0.45f, 0.45f).glow(255, 255, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                Location p = c.clone().add(pX[13] + (Math.random() - 0.5) * 0.8, 0.3,
                        pZ[13] + (Math.random() - 0.5) * 0.8);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.POWDER_SNOW_BUCKET));
                h.scale(0.5f, 0.5f, 0.5f).glow(230, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < prints.size(); i++) {
                if (!pVisible[i] && tick >= pAppearTick[i]) {
                    pVisible[i] = true;
                    Location at = c.clone().add(pX[i], 0.12, pZ[i]);
                    DisplayBuilder.playSound(at, Sound.ENTITY_GENERIC_SMALL_FALL, 0.6f, 0.7f + (float)Math.random() * 0.3f);
                    w.spawnParticle(Particle.SNOWFLAKE, at.clone().add(0, 0.2, 0), 8, 0.4, 0.2, 0.4, 0.03);
                    DisplayBuilder.dustParticles(at, 6, 0.3, 200, 230, 255, 1.2f);
                    prints.get(i).animateTo(
                            new Vector3f((float)pX[i] - 0.3f, 0.12f, (float)pZ[i] - 0.3f),
                            new AxisAngle4f((float)(tick * 0.01 + i), 0, 1, 0),
                            new Vector3f(0.6f, 0.1f, 0.9f), 6);
                }
            }

            // Ambient sparkle on each visible print
            if (tick % 8 == 0) {
                for (int i = 0; i < prints.size(); i++) {
                    if (!pVisible[i]) continue;
                    Location at = c.clone().add(pX[i], 0.25, pZ[i]);
                    w.spawnParticle(Particle.SNOWFLAKE, at, 1, 0.15, 0.05, 0.15, 0.01);
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = 1.5 * 1.5;
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    for (int i = 0; i < prints.size(); i++) {
                        if (!pVisible[i]) continue;
                        Location at = c.clone().add(pX[i], 0.12, pZ[i]);
                        if (p.getLocation().distanceSquared(at) <= r2) {
                            p.damage(config.getDamage());
                            p.setNoDamageTicks(0);
                            break;
                        }
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenFootprintTrail(plugin); }
    }

    // ================================================================
    // 25. SHATTERED MIRROR FLOOR — 36 GLASS_BOTTLE flat tiles in 6x6
    //     grid, after delay shatter upward. Impact: radius 8.0, 5200hp.
    // ================================================================
    public static class ShatteredMirrorFloor extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> tiles = new ArrayList<>();
        private final double[] tX = new double[36];
        private final double[] tZ = new double[36];
        private boolean shattered = false;
        private static final int SHATTER_TICK = 80;
        private static final double TILE_SPACING = 2.2;

        public ShatteredMirrorFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("shattered_mirror_floor", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(62400.0);
            config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(220);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(62400.0);
            config.setImpactRadius(12.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 1.4f, 0.8f);
            DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_STEP, 1.0f, 0.7f);

            int idx = 0;
            for (int gx = 0; gx < 6; gx++) {
                for (int gz = 0; gz < 6; gz++) {
                    tX[idx] = (gx - 2.5) * TILE_SPACING;
                    tZ[idx] = (gz - 2.5) * TILE_SPACING;
                    Location p = c.clone().add(tX[idx], 0.1, tZ[idx]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_BOTTLE));
                    h.scale(0.95f, 0.1f, 0.95f).glow(220, 240, 255).interpolation(6, 0);
                    tiles.add(h);
                    spawnedEntities.add(h.entity());
                    idx++;
                }
            }

            // Decor: 8 GLASS_PANE shards at corners + 4 TINTED_GLASS pillars in cardinals
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 16;
                Location p = c.clone().add(Math.cos(a) * 7.5, 0.5, Math.sin(a) * 7.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLASS_PANE));
                h.scale(0.4f, 0.7f, 0.4f).glow(200, 230, 250).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 4.0, 1.2, Math.sin(a) * 4.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.TINTED_GLASS));
                h.scale(0.5f, 1.0f, 0.5f).glow(80, 100, 130).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < SHATTER_TICK) {
                // Tile sheen pulse — staggered ripple
                int waveIdx = (tick / 4) % 12;
                for (int i = 0; i < tiles.size(); i++) {
                    int dist = (int)(Math.abs(tX[i]) + Math.abs(tZ[i]));
                    if (dist == waveIdx && tick % 2 == 0) {
                        Location at = c.clone().add(tX[i], 0.2, tZ[i]);
                        DisplayBuilder.dustParticles(at, 1, 0.1, 240, 240, 255, 1.0f);
                    }
                }
                if (tick % 12 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 0.5f, 1.5f);
                if (tick == SHATTER_TICK - 10) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_HIT, 1.3f, 0.6f);
                }
            } else if (!shattered) {
                shattered = true;
                Location impact = c.clone().add(0, 0.5, 0);

                // Barrage of glass break sounds across the grid
                for (int i = 0; i < tiles.size(); i++) {
                    Location at = c.clone().add(tX[i], 0.4, tZ[i]);
                    if (i % 4 == 0) DisplayBuilder.playSound(at, Sound.BLOCK_GLASS_BREAK, 1.2f, 0.9f + (float)Math.random() * 0.4f);

                    // Animate each tile flying upward and rotating
                    tiles.get(i).animateTo(
                            new Vector3f((float)tX[i] - 0.475f, 3.5f + (float)Math.random() * 2.0f, (float)tZ[i] - 0.475f),
                            new AxisAngle4f((float)(Math.random() * Math.PI * 2), (float)Math.random(), (float)Math.random(), (float)Math.random()),
                            new Vector3f(0.5f, 0.5f, 0.5f), 16);

                    w.spawnParticle(Particle.END_ROD, at, 6, 0.4, 0.3, 0.4, 0.15);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, at, 4, 0.3, 0.3, 0.3, 0.1);
                }

                w.spawnParticle(Particle.EXPLOSION_EMITTER, impact, 1, 0, 0, 0, 0);
                for (int i = 0; i < 40; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 8.0;
                    Location p = impact.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 3.0, Math.sin(a) * r);
                    DisplayBuilder.dustParticles(p, 2, 0.3, 220, 240, 255, 1.5f);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.1);
                }

                triggerImpactDamage(impact);
            } else {
                // Aftermath — shards continue to fall, particles fade
                if (tick % 4 == 0) {
                    for (int i = 0; i < 6; i++) {
                        double a = Math.random() * Math.PI * 2;
                        double r = Math.random() * 8.0;
                        Location p = c.clone().add(Math.cos(a) * r, 0.3 + Math.random() * 2.0, Math.sin(a) * r);
                        w.spawnParticle(Particle.END_ROD, p, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                if (tick % 18 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.4f, 1.2f);
            }
        }

        @Override public AbstractAttack newInstance() { return new ShatteredMirrorFloor(plugin); }
    }

    // ================================================================
    // 26. PERMAFROST BLOOM — 18 GLOW_BERRIES "flower" opens (petals
    //     unfurl outward). 6 inner + 12 outer petals. Constant radius
    //     7.0, 2800hp ticksBetween=15.
    // ================================================================
    public static class PermafrostBloom extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> petals = new ArrayList<>();
        private final double[] pAng = new double[18];
        private final int[] pRing = new int[18]; // 0 = inner, 1 = outer
        private static final double INNER_R = 2.2;
        private static final double OUTER_R = 5.0;

        public PermafrostBloom(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("permafrost_bloom", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(33600.0);
            config.setDamageRadius(10.5);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(360);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.4f, 1.1f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AZALEA_PLACE, 1.0f, 1.4f);

            // Inner 6 petals
            for (int i = 0; i < 6; i++) {
                pAng[i] = Math.PI * 2 * i / 6;
                pRing[i] = 0;
                // Start at center, animate outward in onTick
                Location p = c.clone().add(0, 0.2, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.6f, 0.6f, 0.6f).glow(180, 255, 220).interpolation(40, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }
            // Outer 12 petals
            for (int i = 6; i < 18; i++) {
                pAng[i] = Math.PI * 2 * (i - 6) / 12 + Math.PI / 12;
                pRing[i] = 1;
                Location p = c.clone().add(0, 0.2, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_BERRIES));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 240, 200).interpolation(40, 0);
                petals.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 4 SPORE_BLOSSOM at cardinals + 6 GLOW_LICHEN around outer ring + 4 ICE pebbles
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4;
                Location p = c.clone().add(Math.cos(a) * 6.0, 0.4, Math.sin(a) * 6.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SPORE_BLOSSOM));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 180, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6 + Math.PI / 12;
                Location p = c.clone().add(Math.cos(a) * 4.5, 0.3, Math.sin(a) * 4.5);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOW_LICHEN));
                h.scale(0.5f, 0.5f, 0.5f).glow(160, 230, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 1.0, 0.3, Math.sin(a) * 1.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.4f, 0.4f, 0.4f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Bloom unfurl — petals smoothly drift outward over first 40 ticks
            double t = Math.min(1.0, tick / 40.0);
            // Smooth ease-out
            double eased = 1 - Math.pow(1 - t, 2);

            for (int i = 0; i < petals.size(); i++) {
                double targetR = (pRing[i] == 0 ? INNER_R : OUTER_R) * eased;
                pAng[i] += 0.008;
                float bx = (float)(Math.cos(pAng[i]) * targetR);
                float bz = (float)(Math.sin(pAng[i]) * targetR);
                float by = (float)(0.25 + Math.sin(tick * 0.06 + i) * 0.1);
                float scale = pRing[i] == 0 ? 0.6f : 0.5f;
                petals.get(i).animateTo(
                        new Vector3f(bx - scale / 2, by, bz - scale / 2),
                        new AxisAngle4f((float)(tick * 0.05 + i), 0, 1, 0),
                        new Vector3f(scale), 4);

                // Petal-open chime as each petal reaches its target
                if (tick > 0 && tick < 42 && (i + 1) * 2 == tick) {
                    Location at = c.clone().add(bx, by, bz);
                    DisplayBuilder.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.0f + i * 0.05f);
                }
            }

            // SPORE_BLOSSOM_AIR + GLOW per tick
            if (tick % 3 == 0) {
                for (int i = 0; i < petals.size(); i++) {
                    double targetR = (pRing[i] == 0 ? INNER_R : OUTER_R) * eased;
                    Location at = c.clone().add(Math.cos(pAng[i]) * targetR, 0.4, Math.sin(pAng[i]) * targetR);
                    w.spawnParticle(Particle.SPORE_BLOSSOM_AIR, at, 1, 0.2, 0.1, 0.2, 0.02);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.GLOW, at, 1, 0.1, 0.1, 0.1, 0.01);
                }
            }
            // Center sparkle once fully bloomed
            if (eased >= 1.0 && tick % 8 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 4, 0.3, 200, 255, 220, 1.2f);
                w.spawnParticle(Particle.GLOW, c.clone().add(0, 0.5, 0), 3, 0.4, 0.2, 0.4, 0.05);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new PermafrostBloom(plugin); }
    }

    // ================================================================
    // 27. AURORA RIBBONS — 30 PRISMARINE_SHARD ribbons at Y+10-14,
    //     swaying like northern lights. Constant radius 10.0, 2400hp
    //     ticksBetween=14.
    // ================================================================
    public static class AuroraRibbons extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> shards = new ArrayList<>();
        private final double[] rAng = new double[30];
        private final double[] rBaseY = new double[30];
        private final double[] rR = new double[30];
        private final double[] rPhase = new double[30];

        public AuroraRibbons(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("aurora_ribbons", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(28800.0);
            config.setDamageRadius(15.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(0);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 0.5f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 0.6f);

            // 30 ribbons — 3 ribbon arcs of 10 shards each
            for (int arc = 0; arc < 3; arc++) {
                double arcStart = Math.PI * 2 * arc / 3;
                for (int j = 0; j < 10; j++) {
                    int i = arc * 10 + j;
                    rAng[i] = arcStart + (j / 10.0) * (Math.PI / 2); // each arc spans 90 deg
                    rBaseY[i] = 10.0 + (j / 9.0) * 4.0; // Y+10 to Y+14
                    rR[i] = 8.0 + Math.sin(j * 0.4) * 1.0;
                    rPhase[i] = Math.random() * Math.PI * 2;
                    Location p = c.clone().add(Math.cos(rAng[i]) * rR[i], rBaseY[i], Math.sin(rAng[i]) * rR[i]);
                    ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_SHARD));
                    int colorR = 120 + arc * 30;
                    int colorG = 220 - arc * 20;
                    int colorB = 240;
                    h.scale(0.7f, 0.25f, 0.7f).glow(colorR, colorG, colorB).interpolation(8, 0);
                    shards.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            // Decor: 6 PRISMARINE_CRYSTALS at low Y suggesting reflective pools + 4 GLOWSTONE_DUST hovering
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 5.0, 0.3, Math.sin(a) * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PRISMARINE_CRYSTALS));
                h.scale(0.5f, 0.1f, 0.5f).glow(160, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 7.0, 8.0 + Math.random() * 4.0, Math.sin(a) * 7.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.GLOWSTONE_DUST));
                h.scale(0.55f, 0.55f, 0.55f).glow(255, 240, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Sway each ribbon — sinusoidal Y bobbing + angular drift
            for (int i = 0; i < shards.size(); i++) {
                rAng[i] += 0.006;
                double sway = Math.sin(tick * 0.05 + rPhase[i]) * 1.2;
                float bx = (float)(Math.cos(rAng[i]) * rR[i]);
                float bz = (float)(Math.sin(rAng[i]) * rR[i]);
                float by = (float)(rBaseY[i] + sway);
                shards.get(i).animateTo(
                        new Vector3f(bx - 0.35f, by, bz - 0.35f),
                        new AxisAngle4f((float)(tick * 0.04 + i), 0, 1, 0.3f),
                        new Vector3f(0.7f, 0.25f, 0.7f), 8);
            }

            // END_ROD + DUST_PILLAR trailing each shard
            if (tick % 2 == 0) {
                for (int i = 0; i < shards.size(); i += 2) {
                    double sway = Math.sin(tick * 0.05 + rPhase[i]) * 1.2;
                    Location at = c.clone().add(Math.cos(rAng[i]) * rR[i], rBaseY[i] + sway, Math.sin(rAng[i]) * rR[i]);
                    w.spawnParticle(Particle.END_ROD, at, 1, 0.2, 0.2, 0.2, 0.02);
                    if (Math.random() < 0.35) w.spawnParticle(Particle.DUST_PILLAR, at, 1, 0.3, 0.3, 0.3, 0,
                            Material.PACKED_ICE.createBlockData());
                }
            }
            // Vertical light pillars descending from ribbons
            if (tick % 8 == 0) {
                for (int i = 0; i < 6; i++) {
                    int s = i * 5;
                    double sway = Math.sin(tick * 0.05 + rPhase[s]) * 1.2;
                    Location top = c.clone().add(Math.cos(rAng[s]) * rR[s], rBaseY[s] + sway, Math.sin(rAng[s]) * rR[s]);
                    Location btm = c.clone().add(Math.cos(rAng[s]) * rR[s], 0.5, Math.sin(rAng[s]) * rR[s]);
                    DisplayBuilder.particleLine(top, btm, Particle.END_ROD, 10, null);
                }
            }
            // Continuous low chime
            if (tick % 22 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.7f);
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new AuroraRibbons(plugin); }
    }

    // ================================================================
    // 28. ICE METEOR SHOWER — 6 PACKED_ICE meteors stagger-fall from sky
    //     Each impact: radius 4.0, 4500hp.
    // ================================================================
    public static class IceMeteorShower extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> meteors = new ArrayList<>();
        private final double[] mAng = new double[6];
        private final double[] mR = new double[6];
        private final double[] mY = new double[6];
        private final int[] mStartTick = new int[6];
        private final boolean[] mImpacted = new boolean[6];

        public IceMeteorShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ice_meteor_shower", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(54000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(300);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(54000.0);
            config.setImpactRadius(6.0);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_AMBIENT, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.0f, 0.6f);

            for (int i = 0; i < 6; i++) {
                mAng[i] = Math.PI * 2 * i / 6 + (Math.random() - 0.5) * 0.6;
                mR[i] = 2.0 + Math.random() * 6.0;
                mY[i] = 22.0 + Math.random() * 4.0;
                mStartTick[i] = i * 16;
                mImpacted[i] = false;
                Location p = c.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.PACKED_ICE));
                h.scale(1.0f, 1.0f, 1.0f).glow(150, 200, 255).interpolation(2, 0);
                meteors.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 8 BLUE_ICE chunks high in sky + 6 ICE shards as foreshadow markers on ground
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + Math.PI / 16;
                Location p = c.clone().add(Math.cos(a) * 9.0, 18.0 + Math.random() * 4.0, Math.sin(a) * 9.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.BLUE_ICE));
                h.scale(0.6f, 0.6f, 0.6f).glow(170, 220, 255).interpolation(60, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * (mR[i % 6]), 0.2, Math.sin(a) * (mR[i % 6]));
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.ICE));
                h.scale(0.5f, 0.1f, 0.5f).glow(180, 230, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            for (int i = 0; i < meteors.size(); i++) {
                if (tick < mStartTick[i] || mImpacted[i]) continue;
                mY[i] -= 0.65;
                Location pos = c.clone().add(Math.cos(mAng[i]) * mR[i], mY[i], Math.sin(mAng[i]) * mR[i]);
                meteors.get(i).animateTo(
                        new Vector3f((float)(Math.cos(mAng[i]) * mR[i]) - 0.5f, (float)mY[i], (float)(Math.sin(mAng[i]) * mR[i]) - 0.5f),
                        new AxisAngle4f((float)(tick * 0.4 + i), 1, 0.5f, 0.3f),
                        new Vector3f(1.0f), 2);

                // Trail — SOUL_FIRE_FLAME + SNOWFLAKE
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, pos, 4, 0.2, 0.2, 0.2, 0.03);
                w.spawnParticle(Particle.SNOWFLAKE, pos, 3, 0.3, 0.3, 0.3, 0.05);
                DisplayBuilder.dustParticles(pos, 2, 0.2, 180, 220, 255, 1.2f);

                // ITEM_TRIDENT_RIPTIDE whoosh while falling
                if (tick % 6 == 0) {
                    DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.5f, 0.7f);
                }

                if (mY[i] < 0.6) {
                    Location impact = c.clone().add(Math.cos(mAng[i]) * mR[i], 0.4, Math.sin(mAng[i]) * mR[i]);
                    DisplayBuilder.playSound(impact, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.6f, 0.8f);
                    DisplayBuilder.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.7f);
                    DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.3f, 0.6f);

                    w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, impact, 24, 0.6, 0.4, 0.6, 0.15);
                    w.spawnParticle(Particle.SNOWFLAKE, impact, 30, 0.6, 0.4, 0.6, 0.2);
                    DisplayBuilder.dustParticles(impact, 22, 0.5, 180, 220, 255, 1.5f);

                    triggerImpactDamage(impact);
                    mImpacted[i] = true;
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new IceMeteorShower(plugin); }
    }

    // ================================================================
    // 29. FROZEN STORM — rotating cloud + 6 NETHER_STAR lightning origins
    //     swirling above. Constant radius 12.0, 2200hp ticksBetween=10.
    // ================================================================
    public static class FrozenStorm extends EnvironmentalAttack {
        private final List<ItemDisplayHandle> stars = new ArrayList<>();
        private final List<ItemDisplayHandle> cloudBits = new ArrayList<>();
        private final double[] sAng = new double[6];
        private final double[] sR = new double[6];
        private final double[] sY = new double[6];
        private final double[] cbAng = new double[12];
        private final double[] cbR = new double[12];
        private final double[] cbY = new double[12];

        public FrozenStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_storm", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(26400.0);
            config.setDamageRadius(18.0);
            config.setTicksBetweenDamage(4);
            config.setDamageDelayTicks(1);
            config.setDurationTicks(400);
            config.setCooldownTicks(60);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, 1.4f);
            DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 1.2f, 0.6f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.0f);

            // 6 NETHER_STAR lightning origins — orbiting at Y+9
            for (int i = 0; i < 6; i++) {
                sAng[i] = Math.PI * 2 * i / 6;
                sR[i] = 7.0;
                sY[i] = 9.0 + Math.sin(i) * 1.2;
                Location p = c.clone().add(Math.cos(sAng[i]) * sR[i], sY[i], Math.sin(sAng[i]) * sR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.NETHER_STAR));
                h.scale(0.6f, 0.6f, 0.6f).glow(220, 240, 255).interpolation(4, 0);
                stars.add(h);
                spawnedEntities.add(h.entity());
            }

            // 12 WHITE_WOOL cloud bits at varying Y to suggest a rotating cloud mass
            for (int i = 0; i < 12; i++) {
                cbAng[i] = Math.PI * 2 * i / 12;
                cbR[i] = 4.0 + (i % 3) * 1.5;
                cbY[i] = 7.5 + Math.sin(i * 0.7) * 1.5;
                Location p = c.clone().add(Math.cos(cbAng[i]) * cbR[i], cbY[i], Math.sin(cbAng[i]) * cbR[i]);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.WHITE_WOOL));
                h.scale(1.0f, 0.7f, 1.0f).glow(220, 230, 245).interpolation(4, 0);
                cloudBits.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 6 LIGHTNING_ROD spike markers on ground beneath stars + 4 SOUL_LANTERN at ground edge
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location p = c.clone().add(Math.cos(a) * 7.0, 0.5, Math.sin(a) * 7.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.LIGHTNING_ROD));
                h.scale(0.3f, 1.2f, 0.3f).glow(200, 220, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location p = c.clone().add(Math.cos(a) * 11.0, 0.6, Math.sin(a) * 11.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(p, new ItemStack(Material.SOUL_LANTERN));
                h.scale(0.55f, 0.55f, 0.55f).glow(180, 220, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            // Rotate stars
            for (int i = 0; i < stars.size(); i++) {
                sAng[i] += 0.04;
                float by = (float)(sY[i] + Math.sin(tick * 0.06 + i) * 0.5);
                float bx = (float)(Math.cos(sAng[i]) * sR[i]);
                float bz = (float)(Math.sin(sAng[i]) * sR[i]);
                stars.get(i).animateTo(
                        new Vector3f(bx - 0.3f, by, bz - 0.3f),
                        new AxisAngle4f((float)(tick * 0.2 + i), 0, 1, 0),
                        new Vector3f(0.6f), 4);
            }

            // Rotate cloud bits (slower, opposite direction)
            for (int i = 0; i < cloudBits.size(); i++) {
                cbAng[i] -= 0.025;
                float by = (float)(cbY[i] + Math.sin(tick * 0.04 + i) * 0.4);
                float bx = (float)(Math.cos(cbAng[i]) * cbR[i]);
                float bz = (float)(Math.sin(cbAng[i]) * cbR[i]);
                cloudBits.get(i).animateTo(
                        new Vector3f(bx - 0.5f, by, bz - 0.35f),
                        new AxisAngle4f((float)(tick * 0.08 + i), 0, 1, 0),
                        new Vector3f(1.0f, 0.7f, 1.0f), 4);
            }

            // Lightning strikes from star down to ground every 18 ticks (random star)
            if (tick % 18 == 0 && tick >= 15) {
                int s = (int)(Math.random() * 6);
                Location top = c.clone().add(Math.cos(sAng[s]) * sR[s], sY[s], Math.sin(sAng[s]) * sR[s]);
                Location btm = c.clone().add(Math.cos(sAng[s]) * sR[s], 0.5, Math.sin(sAng[s]) * sR[s]);
                DisplayBuilder.particleLine(top, btm, Particle.ELECTRIC_SPARK, 24, null);
                DisplayBuilder.particleLine(top, btm, Particle.END_ROD, 12, null);
                w.spawnParticle(Particle.FLASH, btm, 1, 0, 0, 0, 0);
                DisplayBuilder.playSound(btm, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.4f);
                DisplayBuilder.playSound(btm, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 1.4f);
                DisplayBuilder.dustParticles(btm, 12, 0.5, 200, 230, 255, 1.5f);
            }

            // Continuous CLOUD + ELECTRIC_SPARK swirl
            if (tick % 2 == 0) {
                for (int i = 0; i < 12; i++) {
                    double a = (i / 12.0) * Math.PI * 2 + tick * 0.03;
                    double r = 5.0 + Math.sin(tick * 0.04 + i) * 1.5;
                    Location p = c.clone().add(Math.cos(a) * r, 7.5 + Math.sin(i * 0.5) * 1.5, Math.sin(a) * r);
                    w.spawnParticle(Particle.CLOUD, p, 1, 0.2, 0.2, 0.2, 0.02);
                    if (Math.random() < 0.3) w.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
            // Ground rumble dust beneath the storm
            if (tick % 6 == 0) {
                for (int i = 0; i < 6; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 10.0;
                    DisplayBuilder.dustParticles(c.clone().add(Math.cos(a) * r, 0.3, Math.sin(a) * r),
                            1, 0.1, 180, 210, 240, 1.2f);
                }
            }

            if (tick >= config.getDamageDelayTicks() && tick % config.getTicksBetweenDamage() == 0) {
                double r2 = config.getDamageRadius() * config.getDamageRadius();
                for (Player p : w.getPlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || p.isInvulnerable()) continue;
                    if (p.getLocation().distanceSquared(c) <= r2) {
                        p.damage(config.getDamage());
                        p.setNoDamageTicks(0);
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new FrozenStorm(plugin); }
    }

    // ================================================================
    // 30. SKY SHATTERING PRISM — 1 DIAMOND prism hovers at Y+10, then
    //     shatters into 20 fragments raining down. Per-fragment impact:
    //     radius 2.5, 3000hp.
    // ================================================================
    public static class SkyShatteringPrism extends EnvironmentalAttack {
        private ItemDisplayHandle prism;
        private final List<ItemDisplayHandle> fragments = new ArrayList<>();
        private final double[] fAng = new double[20];
        private final double[] fR = new double[20];
        private final double[] fY = new double[20];
        private final double[] fFallSpeed = new double[20];
        private final boolean[] fImpacted = new boolean[20];
        private boolean shattered = false;
        private static final int SHATTER_TICK = 60;
        private double prismY = 10.0;

        public SkyShatteringPrism(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sky_shattering_prism", AttackType.ENVIRONMENTAL, 1, MODE_PATH));
            config.setDamage(36000.0);
            config.setDamageRadius(9.0);
            config.setTicksBetweenDamage(4);
            config.setDurationTicks(280);
            config.setCooldownTicks(60);
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(36000.0);
            config.setImpactRadius(3.75);
        }

        @Override protected void onSpawn(Location c) {
            c.setYaw(0); c.setPitch(0);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.6f);
            DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1.4f, 1.2f);
            DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.8f, 1.6f);

            // The prism — single DIAMOND scaled large at Y+10
            Location p = c.clone().add(0, prismY, 0);
            prism = displayBuilder.spawnItem(p, new ItemStack(Material.DIAMOND));
            prism.scale(2.2f, 2.2f, 2.2f).glow(180, 240, 255).interpolation(8, 0);
            spawnedEntities.add(prism.entity());

            // Pre-create the 20 fragments but keep them tiny/hidden inside the prism
            for (int i = 0; i < 20; i++) {
                fAng[i] = Math.PI * 2 * i / 20 + (Math.random() - 0.5) * 0.3;
                fR[i] = 0.0;
                fY[i] = prismY;
                fFallSpeed[i] = 0.0;
                fImpacted[i] = false;
                Location fp = c.clone().add(0, prismY, 0);
                ItemDisplayHandle h = displayBuilder.spawnItem(fp, new ItemStack(Material.DIAMOND));
                h.scale(0.01f, 0.01f, 0.01f).glow(200, 240, 255).interpolation(4, 0);
                fragments.add(h);
                spawnedEntities.add(h.entity());
            }

            // Decor: 8 PRISMARINE_SHARD ring under prism + 6 AMETHYST_SHARD orbiting prism + 4 GLOWSTONE_DUST on ground
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                Location pp = c.clone().add(Math.cos(a) * 3.0, 0.3, Math.sin(a) * 3.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.PRISMARINE_SHARD));
                h.scale(0.55f, 0.1f, 0.55f).glow(180, 230, 240).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                Location pp = c.clone().add(Math.cos(a) * 2.0, prismY + Math.sin(a) * 1.5, Math.sin(a) * 2.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.AMETHYST_SHARD));
                h.scale(0.5f, 0.5f, 0.5f).glow(200, 240, 255).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
            for (int i = 0; i < 4; i++) {
                double a = Math.PI * 2 * i / 4 + Math.PI / 4;
                Location pp = c.clone().add(Math.cos(a) * 5.0, 0.4, Math.sin(a) * 5.0);
                ItemDisplayHandle h = displayBuilder.spawnItem(pp, new ItemStack(Material.GLOWSTONE_DUST));
                h.scale(0.5f, 0.5f, 0.5f).glow(255, 240, 180).interpolation(40, 0);
                spawnedEntities.add(h.entity());
            }
        }

        @Override protected void onTick(int tick) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return;
            World w = c.getWorld();

            if (tick < SHATTER_TICK) {
                // Prism hovers and rotates, gathering charge
                float by = (float)(prismY + Math.sin(tick * 0.1) * 0.4);
                prism.animateTo(
                        new Vector3f(-1.1f, by, -1.1f),
                        new AxisAngle4f((float)(tick * 0.1), 0.3f, 1, 0.3f),
                        new Vector3f(2.2f), 4);

                // Charge particles spiraling into the prism
                if (tick % 2 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double a = tick * 0.2 + i * Math.PI / 2;
                        double r = 4.0 - (tick / (double)SHATTER_TICK) * 3.5;
                        Location pp = c.clone().add(Math.cos(a) * r, prismY + Math.sin(tick * 0.1) * 0.4, Math.sin(a) * r);
                        w.spawnParticle(Particle.END_ROD, pp, 1, 0.1, 0.1, 0.1, 0.05);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, pp, 1, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                // Wind-up chime
                if (tick % 12 == 0) {
                    DisplayBuilder.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.0f + (tick / (float)SHATTER_TICK) * 1.0f);
                }
            } else if (!shattered) {
                shattered = true;
                Location prismLoc = c.clone().add(0, prismY, 0);

                // Hide prism (scale to zero)
                prism.animateTo(
                        new Vector3f(0, (float)prismY, 0),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(0.01f), 2);

                DisplayBuilder.playSound(prismLoc, Sound.BLOCK_GLASS_BREAK, 1.6f, 1.2f);
                DisplayBuilder.playSound(prismLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.4f, 1.4f);
                DisplayBuilder.playSound(prismLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.5f, 1.0f);
                DisplayBuilder.playSound(prismLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                w.spawnParticle(Particle.EXPLOSION_EMITTER, prismLoc, 1, 0, 0, 0, 0);
                w.spawnParticle(Particle.FLASH, prismLoc, 1, 0, 0, 0, 0);
                for (int i = 0; i < 30; i++) {
                    double a = Math.random() * Math.PI * 2;
                    double r = Math.random() * 3.0;
                    double yy = prismY + (Math.random() - 0.5) * 3.0;
                    Location pp = c.clone().add(Math.cos(a) * r, yy, Math.sin(a) * r);
                    w.spawnParticle(Particle.END_ROD, pp, 2, 0.2, 0.2, 0.2, 0.2);
                    w.spawnParticle(Particle.ELECTRIC_SPARK, pp, 2, 0.2, 0.2, 0.2, 0.15);
                }

                // Launch all 20 fragments — outward angles, varying fall speeds
                for (int i = 0; i < fragments.size(); i++) {
                    fR[i] = 0.5;
                    fY[i] = prismY;
                    fFallSpeed[i] = 0.4 + Math.random() * 0.3;
                }
            } else {
                // Fragments rain down
                for (int i = 0; i < fragments.size(); i++) {
                    if (fImpacted[i]) continue;
                    fR[i] += 0.18;
                    fY[i] -= fFallSpeed[i];

                    Location pos = c.clone().add(Math.cos(fAng[i]) * fR[i], fY[i], Math.sin(fAng[i]) * fR[i]);
                    fragments.get(i).animateTo(
                            new Vector3f((float)(Math.cos(fAng[i]) * fR[i]) - 0.25f, (float)fY[i], (float)(Math.sin(fAng[i]) * fR[i]) - 0.25f),
                            new AxisAngle4f((float)(tick * 0.5 + i), 0.4f, 1, 0.4f),
                            new Vector3f(0.5f), 2);

                    // Sparkle trail
                    w.spawnParticle(Particle.END_ROD, pos, 1, 0.1, 0.1, 0.1, 0.02);
                    if (Math.random() < 0.4) w.spawnParticle(Particle.ELECTRIC_SPARK, pos, 1, 0.1, 0.1, 0.1, 0.05);
                    DisplayBuilder.dustParticles(pos, 1, 0.05, 200, 240, 255, 1.0f);

                    // Whoosh while falling
                    if (tick % 8 == 0 && Math.random() < 0.3) {
                        DisplayBuilder.playSound(pos, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.4f, 1.4f);
                    }

                    if (fY[i] < 0.5) {
                        Location impact = c.clone().add(Math.cos(fAng[i]) * fR[i], 0.4, Math.sin(fAng[i]) * fR[i]);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_GLASS_BREAK, 1.1f, 1.3f);
                        DisplayBuilder.playSound(impact, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.4f);

                        w.spawnParticle(Particle.EXPLOSION, impact, 1, 0, 0, 0, 0);
                        w.spawnParticle(Particle.END_ROD, impact, 10, 0.4, 0.3, 0.4, 0.2);
                        w.spawnParticle(Particle.ELECTRIC_SPARK, impact, 8, 0.3, 0.3, 0.3, 0.15);
                        DisplayBuilder.dustParticles(impact, 8, 0.3, 220, 240, 255, 1.4f);

                        triggerImpactDamage(impact);
                        fImpacted[i] = true;
                    }
                }
            }
        }

        @Override public AbstractAttack newInstance() { return new SkyShatteringPrism(plugin); }
    }
}
