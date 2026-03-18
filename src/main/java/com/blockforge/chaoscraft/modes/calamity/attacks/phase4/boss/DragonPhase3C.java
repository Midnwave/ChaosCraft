package com.blockforge.chaoscraft.modes.calamity.attacks.phase4.boss;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 4D Boss — THE VOID EMPEROR (Ender Dragon)
 * Phase 3C: Attacks #116-125
 * Final Phase 3: Enrage sequence (15% HP), pre-death escalation (5% HP),
 * death cinematic trigger (1% HP). The absolute climax.
 * Phase 3 damage range: 18-36 HP.
 * NO status effects — damage only.
 */
public final class DragonPhase3C {

    private DragonPhase3C() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new AllBladesConvergence(plugin));
        registry.register(new VoidEmperorsCrown(plugin));
        registry.register(new DwellersLastGift(plugin));
        registry.register(new ShatterpointDive(plugin));
        registry.register(new AllBladesEmperorsExplosion(plugin));
        registry.register(new EnrageSequenceAscent(plugin));
        registry.register(new EmperorDescendsImpact(plugin));
        registry.register(new EndlessBreath(plugin));
        registry.register(new VoidEmperorsWrath(plugin));
        registry.register(new FinalCommand(plugin));
    }

    // ================================================================
    // #116 — ALL BLADES CONVERGENCE — 50+ downward bolt rain from blade rings
    // ================================================================
    public static class AllBladesConvergence extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private boolean bladesFired = false;

        public AllBladesConvergence(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_all_blades_convergence", AttackType.BOSS, 4), "dragon");
            config.setDamage(20.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(250);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All blades pulse white then slow rotation
            DisplayBuilder.cyanDust(center.clone().add(0, 12, 0), 25, 10.0);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 8 && !bladesFired) {
                bladesFired = true;
                // 50 downward bolts at random arena positions
                for (int i = 0; i < 50; i++) {
                    double ox = (Math.random() - 0.5) * 28;
                    double oz = (Math.random() - 0.5) * 28;
                    Location boltStart = center.clone().add(ox, 20, oz);
                    BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltStart, Material.IRON_BLOCK);
                    bolt.scale(0.2f, 1.0f, 0.2f).glow(255, 255, 255).interpolation(1, 0);
                    bladeHandles.add(bolt);
                    spawnedEntities.add(bolt.entity());
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 2.0f, 1.4f);
            }
            // Bolts fall (8-38 ticks = 1.5 seconds)
            if (bladesFired && ticksAlive > 8 && ticksAlive < 38) {
                float fallDist = (ticksAlive - 8) * 0.7f;
                for (BlockDisplayHandle bolt : bladeHandles) {
                    Location loc = bolt.entity().getLocation();
                    bolt.entity().teleport(loc.clone().add(0, -fallDist * 0.05, 0));
                }
            }
            // Impacts (tick 38)
            if (bladesFired && ticksAlive == 38) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 30, 15.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.1f);
            }
            // Floor scatter (38-198 ticks = 8 seconds)
            if (bladesFired && ticksAlive > 38 && ticksAlive < 198 && ticksAlive % 15 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 6, 14.0);
            }
            // Blade regeneration (tick 98)
            if (bladesFired && ticksAlive == 98) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AllBladesConvergence(plugin); }
    }

    // ================================================================
    // #117 — VOID EMPEROR'S CROWN — Descending void ring, 40-second duration
    // ================================================================
    public static class VoidEmperorsCrown extends BossAttack {
        private final List<BlockDisplayHandle> crownHandles = new ArrayList<>();
        private boolean crownActive = false;
        private int crownTick = 0;

        public VoidEmperorsCrown(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_emperors_crown", AttackType.BOSS, 4), "dragon");
            config.setDamage(18.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(800);
            config.setCooldownTicks(700);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Crown ring at Y+20
            for (int i = 0; i < 16; i++) {
                double angle = (Math.PI * 2 / 16) * i;
                Location ringPt = center.clone().add(Math.cos(angle) * 10, 20, Math.sin(angle) * 10);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.PURPLE_STAINED_GLASS);
                ring.scale(2.0f, 1.0f, 2.0f).glow(120, 0, 200).interpolation(3, 0);
                crownHandles.add(ring);
                spawnedEntities.add(ring.entity());
            }
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 2.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !crownActive) {
                crownActive = true;
                crownTick = ticksAlive;
            }
            // Crown descends from Y+20 to Y+2 over 720 ticks (36 seconds)
            if (crownActive && ticksAlive - crownTick < 720) {
                float currentY = 20 - ((ticksAlive - crownTick) / 720.0f) * 18;
                for (int i = 0; i < crownHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 16) * i;
                    crownHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * 10, currentY, Math.sin(angle) * 10));
                }
                if (ticksAlive % 20 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, currentY, 0), 6, 10.0);
                    triggerImpactDamage(center.clone().add(0, currentY, 0));
                }
            }
            // Crown dismisses (tick 726 or 800)
            if (crownActive && ticksAlive - crownTick == 720) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 20, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.7f);
            }
            // Crown ambient sound loop
            if (crownActive && ticksAlive % 40 == 0 && ticksAlive - crownTick < 720) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorsCrown(plugin); }
    }

    // ================================================================
    // #118 — THE DWELLER'S LAST GIFT — Full arena brimstone ignition, 6 seconds
    // ================================================================
    public static class DwellersLastGift extends BossAttack {
        private final List<BlockDisplayHandle> giftHandles = new ArrayList<>();
        private boolean ignited = false;

        public DwellersLastGift(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_dwellers_last_gift", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(30.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dweller face projection in sky
            BlockDisplayHandle face = displayBuilder.spawnBlock(
                center.clone().add(0, 30, 0), Material.MAGMA_BLOCK);
            face.scale(8.0f, 8.0f, 2.0f).glow(255, 80, 0).interpolation(3, 0);
            giftHandles.add(face);
            spawnedEntities.add(face.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Face shatters (tick 10)
            if (ticksAlive == 10 && !ignited) {
                ignited = true;
                DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 30, 8.0);
                // Full arena floor ignition
                for (int x = -3; x <= 3; x++) {
                    for (int z = -3; z <= 3; z++) {
                        Location fireLoc = center.clone().add(x * 4, 0.05, z * 4);
                        BlockDisplayHandle fire = displayBuilder.spawnBlock(fireLoc, Material.MAGMA_BLOCK);
                        fire.scale(4.0f, 0.15f, 4.0f).glow(180, 0, 100).interpolation(1, 0);
                        giftHandles.add(fire);
                        spawnedEntities.add(fire.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 3.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 3.0f, 0.4f);
            }
            // Floor burning (10-130 ticks = 6 seconds)
            if (ignited && ticksAlive < 130) {
                if (ticksAlive % 10 == 0) {
                    triggerImpactDamage(center);
                    DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 10, 20.0);
                }
            }
            // Brimstone extinguishes (tick 130)
            if (ignited && ticksAlive == 130) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.5, 0), 30, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_DEATH, 2.5f, 0.5f);
            }
            // Scorch marks fade (130-330 ticks = 10 seconds)
            if (ignited && ticksAlive > 130 && ticksAlive < 330 && ticksAlive % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 0.2, 0), 4, 15.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DwellersLastGift(plugin); }
    }

    // ================================================================
    // #119 — SHATTERPOINT DIVE — Armor-bypass precision strike
    // ================================================================
    public static class ShatterpointDive extends BossAttack {
        private final List<BlockDisplayHandle> diveHandles = new ArrayList<>();
        private boolean diveLanded = false;

        public ShatterpointDive(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_shatterpoint_dive", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(2.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(440);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // White void lightning crackle + ascent
            BlockDisplayHandle crackle = displayBuilder.spawnBlock(
                center.clone().add(0, 35, 0), Material.WHITE_STAINED_GLASS);
            crackle.scale(2.0f, 2.0f, 2.0f).glow(255, 255, 255).interpolation(2, 0);
            diveHandles.add(crackle);
            spawnedEntities.add(crackle.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !diveLanded) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 35, 0), 10, 3.0);
                }
            }
            // Pinpoint dive (tick 10)
            else if (ticksAlive == 10 && !diveLanded) {
                diveLanded = true;
                double ox = (Math.random() - 0.5) * 16;
                double oz = (Math.random() - 0.5) * 16;
                Location targetLoc = center.clone().add(ox, 0.2, oz);
                // Needle-point trail from Y+40 to target
                BlockDisplayHandle spear = displayBuilder.spawnBlock(
                    targetLoc.clone().add(0, 20, 0), Material.WHITE_STAINED_GLASS);
                spear.scale(0.3f, 40.0f, 0.3f).glow(255, 255, 255).interpolation(1, 0);
                diveHandles.add(spear);
                spawnedEntities.add(spear.entity());
                triggerImpactDamage(targetLoc);
                DisplayBuilder.cyanDust(targetLoc, 25, 3.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 2.0f);
                DisplayBuilder.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.9f);
            }
            // Near-miss shockwave visual (tick 14)
            if (diveLanded && ticksAlive == 14) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 15, 6.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ShatterpointDive(plugin); }
    }

    // ================================================================
    // #120 — ALL BLADES: EMPEROR'S EXPLOSION — 360-degree blade fire + return
    // ================================================================
    public static class AllBladesEmperorsExplosion extends BossAttack {
        private final List<BlockDisplayHandle> bladeHandles = new ArrayList<>();
        private boolean bladesFired = false;
        private boolean bladesReturning = false;
        private int fireTick = 0;

        public AllBladesEmperorsExplosion(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_all_blades_emperors_explosion", AttackType.BOSS, 4), "dragon");
            config.setDamage(26.0);
            config.setDamageRadius(1.5);
            config.setDurationTicks(350);
            config.setCooldownTicks(800);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // All blades halt, orient outward, glow intensifies
            for (int i = 0; i < 50; i++) {
                double angle = (Math.PI * 2 / 50) * i;
                Location bladeLoc = center.clone().add(Math.cos(angle) * 3, 10, Math.sin(angle) * 3);
                BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                blade.scale(0.2f, 1.0f, 0.3f).glow(255, 255, 255).interpolation(2, 0);
                bladeHandles.add(blade);
                spawnedEntities.add(blade.entity());
            }
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Telegraph glow (0-10 ticks)
            if (ticksAlive < 10 && !bladesFired) {
                if (ticksAlive % 2 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 15, 5.0);
                }
            }
            // All blades fire outward (tick 10)
            else if (ticksAlive == 10 && !bladesFired) {
                bladesFired = true;
                fireTick = ticksAlive;
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 40, 5.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_SHOOT, 3.0f, 0.8f);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 1.0f);
            }
            // Blades expand outward (10-50 ticks = 2 seconds)
            else if (bladesFired && !bladesReturning && ticksAlive - fireTick < 40) {
                float dist = (ticksAlive - fireTick) * 0.7f;
                for (int i = 0; i < bladeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 50) * i;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * (3 + dist), 10, Math.sin(angle) * (3 + dist)));
                }
            }
            // Blades hit arena edge — arc back (tick 50)
            else if (bladesFired && !bladesReturning && ticksAlive - fireTick == 40) {
                bladesReturning = true;
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.5f);
            }
            // Return arc (50-80 ticks at 1.5x speed)
            else if (bladesReturning && ticksAlive - fireTick < 70) {
                float returnDist = 28 - (ticksAlive - fireTick - 40) * 1.05f;
                for (int i = 0; i < bladeHandles.size(); i++) {
                    double angle = (Math.PI * 2 / 50) * i + 0.06;
                    bladeHandles.get(i).entity().teleport(
                        center.clone().add(Math.cos(angle) * Math.max(3, returnDist), 10, Math.sin(angle) * Math.max(3, returnDist)));
                }
            }
            // Return impacts converge (tick 80)
            else if (bladesReturning && ticksAlive - fireTick == 70) {
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 30, 10.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.5f, 0.7f);
            }
            // Blades rebuild (80-180 ticks = 5 seconds)
            if (bladesReturning && ticksAlive - fireTick > 70 && ticksAlive - fireTick < 170 && (ticksAlive - fireTick) % 20 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 6, 5.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AllBladesEmperorsExplosion(plugin); }
    }

    // ================================================================
    // #121 — ENRAGE SEQUENCE: THE ASCENT — 15% HP threshold trigger
    // ================================================================
    public static class EnrageSequenceAscent extends BossAttack {
        private final List<BlockDisplayHandle> ascentHandles = new ArrayList<>();
        private boolean ascended = false;

        public EnrageSequenceAscent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_enrage_sequence_ascent", AttackType.BOSS, 4), "dragon");
            config.setDamage(0.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Full-body white flash
            DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 30, 8.0);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon ascends to Y+60 (10-50 ticks)
            if (ticksAlive >= 10 && ticksAlive < 50 && !ascended) {
                float ascentY = 10 + (ticksAlive - 10) * 1.25f;
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, ascentY, 0), 8, 3.0);
                }
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 2.5f, 1.6f);
            }
            // 5-second hover at Y+60 (50-150 ticks) — silence
            if (ticksAlive == 50) {
                ascended = true;
                BlockDisplayHandle hoverDragon = displayBuilder.spawnBlock(
                    center.clone().add(0, 60, 0), Material.OBSIDIAN);
                hoverDragon.scale(6.0f, 6.0f, 6.0f).glow(80, 0, 120).interpolation(3, 0);
                ascentHandles.add(hoverDragon);
                spawnedEntities.add(hoverDragon.entity());
            }
            if (ascended && ticksAlive >= 50 && ticksAlive < 150) {
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 60, 0), 8, 6.0);
                }
            }
            // Dragon descends — final sequence begins (tick 150)
            if (ascended && ticksAlive == 150) {
                DisplayBuilder.cyanDust(center.clone().add(0, 60, 0), 25, 8.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.3f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EnrageSequenceAscent(plugin); }
    }

    // ================================================================
    // #122 — THE EMPEROR DESCENDS: IMPACT STRIKE — Terminal velocity from Y+60
    // ================================================================
    public static class EmperorDescendsImpact extends BossAttack {
        private final List<BlockDisplayHandle> impactHandles = new ArrayList<>();
        private boolean impacted = false;

        public EmperorDescendsImpact(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_emperor_descends_impact", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(25.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon falls (0-30 ticks = 1.5 seconds)
            if (ticksAlive < 30 && !impacted) {
                float fallY = 60 - ticksAlive * 2.0f;
                if (ticksAlive % 3 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, fallY, 0), 12, 4.0);
                }
            }
            // Impact (tick 30)
            else if (ticksAlive == 30 && !impacted) {
                impacted = true;
                // 4 shockwave ring tiers
                double[] radii = {5, 15, 25, 35};
                for (double r : radii) {
                    for (int i = 0; i < 16; i++) {
                        double angle = (Math.PI * 2 / 16) * i;
                        Location ringPt = center.clone().add(Math.cos(angle) * r, 0.5, Math.sin(angle) * r);
                        BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, Material.CRYING_OBSIDIAN);
                        ring.scale(2.0f, 1.0f, 2.0f).glow(200, 100, 255).interpolation(1, 0);
                        impactHandles.add(ring);
                        spawnedEntities.add(ring.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 50, 25.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3.0f, 0.2f);
                DisplayBuilder.playSound(center, Sound.BLOCK_STONE_PLACE, 3.0f, 0.3f);
            }
            // Triple essence flare (tick 34)
            if (impacted && ticksAlive == 34) {
                DisplayBuilder.cyanDust(center.clone().add(0, 2, 0), 25, 20.0);
                DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 3.0f, 0.2f);
            }
            // Sustained rumble (34-114 ticks = 4 seconds)
            if (impacted && ticksAlive > 34 && ticksAlive < 114 && ticksAlive % 10 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 1, 0), 8, 20.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EmperorDescendsImpact(plugin); }
    }

    // ================================================================
    // #123 — THE ENDLESS BREATH — Continuous 360-degree rotating breath, 20s
    // ================================================================
    public static class EndlessBreath extends BossAttack {
        private final List<BlockDisplayHandle> breathHandles = new ArrayList<>();
        private boolean breathActive = false;
        private int breathTick = 0;

        public EndlessBreath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_endless_breath", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Breath charge
            BlockDisplayHandle jawCharge = displayBuilder.spawnBlock(
                center.clone().add(0, 5, 0), Material.PURPLE_STAINED_GLASS);
            jawCharge.scale(2.0f, 2.0f, 2.0f).glow(100, 0, 200).interpolation(2, 0);
            breathHandles.add(jawCharge);
            spawnedEntities.add(jawCharge.entity());
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            if (ticksAlive == 6 && !breathActive) {
                breathActive = true;
                breathTick = ticksAlive;
            }
            // Continuous rotating breath (6-406 ticks = 20 seconds)
            if (breathActive && ticksAlive - breathTick < 400) {
                int elapsed = ticksAlive - breathTick;
                // Rotation speed increases every 100 ticks
                float baseSpeed = 0.015f;
                if (elapsed > 200) baseSpeed = 0.03f;
                if (elapsed > 300) baseSpeed = 0.06f;
                float rotAngle = elapsed * baseSpeed;
                double bx = Math.cos(rotAngle) * 12;
                double bz = Math.sin(rotAngle) * 12;
                Location breathEnd = center.clone().add(bx, 2.5, bz);
                if (ticksAlive % 2 == 0) {
                    BlockDisplayHandle breathSeg = displayBuilder.spawnBlock(breathEnd, Material.PURPLE_STAINED_GLASS);
                    breathSeg.scale(6.0f, 5.0f, 1.5f).glow(80, 0, 140).interpolation(1, 0);
                    spawnedEntities.add(breathSeg.entity());
                    triggerImpactDamage(breathEnd);
                }
                // Sound every 100 ticks (acceleration cue)
                if (elapsed % 100 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.5f, 0.6f);
                }
                if (elapsed % 20 == 0) {
                    DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_SHOOT, 3.0f, 0.5f);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new EndlessBreath(plugin); }
    }

    // ================================================================
    // #124 — VOID EMPEROR'S WRATH: FULL CONVERGENCE — 5% HP, 3 simultaneous attacks
    // ================================================================
    public static class VoidEmperorsWrath extends BossAttack {
        private final List<BlockDisplayHandle> wrathHandles = new ArrayList<>();
        private boolean wrathFired = false;

        public VoidEmperorsWrath(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_void_emperors_wrath", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(15.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(6);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Dragon stops, freezes, drone builds
            BlockDisplayHandle droneGlow = displayBuilder.spawnBlock(
                center.clone().add(0, 10, 0), Material.PURPLE_STAINED_GLASS);
            droneGlow.scale(1.0f, 1.0f, 1.0f).glow(200, 100, 255).interpolation(4, 0);
            wrathHandles.add(droneGlow);
            spawnedEntities.add(droneGlow.entity());
            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 3.0f, 0.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Drone sphere grows (0-10 ticks = 0.5 seconds)
            if (ticksAlive < 10 && !wrathFired) {
                float scale = 1.0f + ticksAlive * 0.7f;
                wrathHandles.get(0).entity().setTransformation(new Transformation(
                    new Vector3f(-scale / 2, 10 - scale / 2, -scale / 2),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 1, 0)));
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 10, scale);
            }
            // ALL THREE SIMULTANEOUS (tick 10)
            else if (ticksAlive == 10 && !wrathFired) {
                wrathFired = true;
                // Layer 1: All blades fire outward (50+ blades)
                for (int i = 0; i < 50; i++) {
                    double angle = (Math.PI * 2 / 50) * i;
                    Location bladeLoc = center.clone().add(Math.cos(angle) * 15, 10, Math.sin(angle) * 15);
                    BlockDisplayHandle blade = displayBuilder.spawnBlock(bladeLoc, Material.IRON_BLOCK);
                    blade.scale(0.2f, 1.0f, 0.3f).glow(255, 255, 255).interpolation(1, 0);
                    spawnedEntities.add(blade.entity());
                }
                // Layer 2: Stream grid (25 bolts)
                for (int x = -2; x <= 2; x++) {
                    for (int z = -2; z <= 2; z++) {
                        Location boltLoc = center.clone().add(x * 4, 0.2, z * 4);
                        BlockDisplayHandle bolt = displayBuilder.spawnBlock(boltLoc, Material.GLOWSTONE);
                        bolt.scale(0.5f, 0.5f, 0.5f).glow(255, 215, 0).interpolation(1, 0);
                        spawnedEntities.add(bolt.entity());
                    }
                }
                // Layer 3: Essence rings (3 concentric)
                Material[] ringMats = {Material.PURPLE_STAINED_GLASS, Material.AMETHYST_BLOCK, Material.MAGMA_BLOCK};
                double[] ringRadii = {5, 10, 15};
                for (int r = 0; r < 3; r++) {
                    for (int i = 0; i < 12; i++) {
                        double angle = (Math.PI * 2 / 12) * i;
                        Location ringPt = center.clone().add(Math.cos(angle) * ringRadii[r], 1, Math.sin(angle) * ringRadii[r]);
                        BlockDisplayHandle ring = displayBuilder.spawnBlock(ringPt, ringMats[r]);
                        ring.scale(2.0f, 2.0f, 0.5f).glow(200, 100, 255).interpolation(1, 0);
                        spawnedEntities.add(ring.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 10, 0), 50, 20.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 3.0f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidEmperorsWrath(plugin); }
    }

    // ================================================================
    // #125 — THE FINAL COMMAND — 1% HP, cinematic death-or-victory trigger
    // ================================================================
    public static class FinalCommand extends BossAttack {
        private final List<BlockDisplayHandle> commandHandles = new ArrayList<>();
        private boolean voidSphereGrowing = false;
        private boolean exploded = false;

        public FinalCommand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dragon_final_command", AttackType.BOSS, 4), "dragon");
            config.setDamage(36.0);
            config.setDamageRadius(40.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(0);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;
            // Second 0: Silence. Everything stops.
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;

            // Dragon rises slowly (0-60 ticks = 3 seconds)
            if (ticksAlive < 60 && !voidSphereGrowing) {
                float riseY = ticksAlive * 0.33f;
                if (ticksAlive % 6 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, riseY, 0), 6, 3.0);
                }
            }
            // Second 3: Title fires, void sphere begins (tick 60)
            if (ticksAlive == 60 && !voidSphereGrowing) {
                voidSphereGrowing = true;
                BlockDisplayHandle voidSphere = displayBuilder.spawnBlock(
                    center.clone().add(0, 20, 0), Material.OBSIDIAN);
                voidSphere.scale(1.0f, 1.0f, 1.0f).glow(0, 0, 0).interpolation(4, 0);
                commandHandles.add(voidSphere);
                spawnedEntities.add(voidSphere.entity());
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 3.0f, 0.1f);
            }
            // Void sphere grows (60-160 ticks = 5 seconds)
            if (voidSphereGrowing && !exploded && ticksAlive < 160) {
                float progress = (ticksAlive - 60) / 100.0f;
                float scale = 1.0f + progress * 15.0f;
                if (!commandHandles.isEmpty()) {
                    commandHandles.get(0).entity().setTransformation(new Transformation(
                        new Vector3f(-scale / 2, 20 - scale / 2, -scale / 2),
                        new AxisAngle4f(0, 0, 1, 0),
                        new Vector3f(scale, scale, scale),
                        new AxisAngle4f(0, 0, 1, 0)));
                }
                if (ticksAlive % 10 == 0) {
                    DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 6, scale);
                }
            }
            // Second 8: THE COMMAND — void sphere explodes (tick 160)
            if (voidSphereGrowing && !exploded && ticksAlive == 160) {
                exploded = true;
                // Full arena omnidirectional void explosion
                for (int layer = 0; layer < 3; layer++) {
                    for (int i = 0; i < 24; i++) {
                        double angle = (Math.PI * 2 / 24) * i;
                        double yAngle = (layer - 1) * 0.4;
                        Location burstPt = center.clone().add(
                            Math.cos(angle) * 20, 20 + Math.sin(yAngle) * 15, Math.sin(angle) * 20);
                        BlockDisplayHandle burst = displayBuilder.spawnBlock(burstPt, Material.PURPLE_STAINED_GLASS);
                        burst.scale(4.0f, 4.0f, 4.0f).glow(100, 0, 200).interpolation(1, 0);
                        spawnedEntities.add(burst.entity());
                    }
                }
                triggerImpactDamage(center);
                DisplayBuilder.cyanDust(center.clone().add(0, 20, 0), 50, 40.0);
                DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.1f);
                DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.5f);
            }
            // Sky streams fall apart — frozen particles rain (160-200 ticks)
            if (exploded && ticksAlive > 160 && ticksAlive < 200 && ticksAlive % 4 == 0) {
                DisplayBuilder.cyanDust(center.clone().add(0, 30, 0), 15, 20.0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FinalCommand(plugin); }
    }
}
