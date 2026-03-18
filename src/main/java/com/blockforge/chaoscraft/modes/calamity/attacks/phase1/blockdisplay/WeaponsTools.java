package com.blockforge.chaoscraft.modes.calamity.attacks.phase1.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 Block Display -- GROUP 8: WEAPONS AND TOOLS
 * 10 weapon-shaped structures that float, spin, and slash.
 * Adapted from boss1-voidmaw.md attacks #71-80 with Calamity rules:
 * - NO status effects (Weakness, Wither, Slowness removed -- damage only)
 * - Always spawn straight (yaw=0, pitch=0)
 * - Calamity particle palette: purple (128,0,255), cyan (0,200,255), crimson (200,0,50)
 * - All values configurable via AttackConfig
 */
public final class WeaponsTools {

    private WeaponsTools() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new VoidBlade(plugin));
        registry.register(new ReapingScythe(plugin));
        registry.register(new DreadMaul(plugin));
        registry.register(new VoidClaw(plugin));
        registry.register(new ObsidianAnchor(plugin));
        registry.register(new ThousandSpears(plugin));
        registry.register(new SiegeBallista(plugin));
        registry.register(new RazorPendulum(plugin));
        registry.register(new AbyssalTrident(plugin));
        registry.register(new TwinVoidAxes(plugin));
    }

    // ================================================================
    // 71. VOID BLADE -- Massive obsidian greatsword with phantom slashes
    // ================================================================
    public static class VoidBlade extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private BlockDisplayHandle crossguard;
        private BlockDisplayHandle pommel;
        private BlockDisplayHandle gemstone;
        private boolean slashing = false;
        private int slashTick = 0;

        public VoidBlade(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_blade", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(14.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(100); // Damage on slash cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Blade: 9 obsidian blocks tapering upward, 2 wide at base narrowing to 1
            for (int i = 0; i < 9; i++) {
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float width = Math.max(0.5f, 1.0f - (i * 0.06f));
                h.scale(width, 1.0f, 0.3f).glow(40, 0, 80).interpolation(2, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite fuller (groove down blade center)
            for (int i = 1; i < 7; i++) {
                Location loc = center.clone().add(0, i, 0.01);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.25f, 0.9f, 0.15f).glow(20, 20, 30).interpolation(2, 0);
                bladeBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crossguard: polished blackstone extending perpendicular
            crossguard = displayBuilder.spawnBlock(center.clone().add(0, -0.5, 0), Material.POLISHED_BLACKSTONE);
            crossguard.scale(3.0f, 0.4f, 0.4f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(crossguard.entity());

            // Grip: blackstone below crossguard
            BlockDisplayHandle grip = displayBuilder.spawnBlock(center.clone().add(0, -1.5, 0), Material.BLACKSTONE);
            grip.scale(0.5f, 1.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(grip.entity());

            // Pommel: crying obsidian at base
            pommel = displayBuilder.spawnBlock(center.clone().add(0, -2.5, 0), Material.CRYING_OBSIDIAN);
            pommel.scale(0.6f, 0.5f, 0.6f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(pommel.entity());

            // Gemstone at crossguard center
            gemstone = displayBuilder.spawnBlock(center.clone().add(0, -0.3, 0.2), Material.AMETHYST_BLOCK);
            gemstone.scale(0.4f, 0.4f, 0.4f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(gemstone.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.8f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow overhead Y-axis rotation: 0.4 deg/tick = ~0.007 rad/tick
            float baseRot = ticksAlive * 0.007f;

            // Every 100 ticks: rapid 360-degree X-axis slash over 20 ticks
            int cycleTick = ticksAlive % 100;
            float slashAngle = 0;
            if (cycleTick >= 80 && cycleTick < 100) {
                int slashProgress = cycleTick - 80;
                slashAngle = (slashProgress / 20.0f) * (float)(Math.PI * 2);
                if (slashProgress == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.6f);
                }
            }

            // Apply Y rotation + slash X rotation to all blade blocks
            for (BlockDisplayHandle h : bladeBlocks) {
                if (slashAngle > 0) {
                    // During slash: combine Y rotation with X-axis spin
                    h.rotate(baseRot + slashAngle, 0.7f, 0.7f, 0);
                } else {
                    h.rotate(baseRot, 0, 1, 0);
                }
                h.interpolation(2, 0);
            }
            if (crossguard != null) {
                crossguard.rotate(baseRot, 0, 1, 0);
                crossguard.interpolation(2, 0);
            }

            // Gemstone pulse: scale 1.0 to 1.5 every 50 ticks
            if (gemstone != null) {
                float gemPulse = 0.4f + (float) Math.sin(ticksAlive * 0.1257) * 0.1f; // 50-tick cycle
                gemstone.scale(gemPulse, gemPulse, gemPulse);
                gemstone.interpolation(3, 0);
            }

            // Crit particles from blade tip
            if (ticksAlive % 2 == 0 && bladeBlocks.size() > 8) {
                Location tip = bladeBlocks.get(8).entity().getLocation().add(0, 0.5, 0);
                c.getWorld().spawnParticle(Particle.CRIT, tip, 5, 0.1, 0.2, 0.1, 0.1);
            }

            // Dark purple dust along cutting edges
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < Math.min(bladeBlocks.size(), 9); i += 2) {
                    Location edgeLoc = bladeBlocks.get(i).entity().getLocation();
                    DisplayBuilder.darkPurpleDust(edgeLoc, 2, 0.3);
                }
            }

            // Crying obsidian tears from pommel
            if (ticksAlive % 5 == 0 && pommel != null) {
                Location pommelLoc = pommel.entity().getLocation();
                c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, pommelLoc, 2, 0.1, 0.2, 0.1, 0);
            }

            // Continuous netherite hit ambient
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.4f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidBlade(plugin); }
    }

    // ================================================================
    // 72. REAPING SCYTHE -- Curved crescent blade on a long haft
    // ================================================================
    public static class ReapingScythe extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeArc = new ArrayList<>();
        private final List<BlockDisplayHandle> haftBlocks = new ArrayList<>();
        private BlockDisplayHandle counterweight;
        private final List<BlockDisplayHandle> edgeInlays = new ArrayList<>();

        public ReapingScythe(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("reaping_scythe", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(80); // Damage on swipe cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Crescent blade arc: 8 obsidian blocks in a sweeping curve
            for (int i = 0; i < 8; i++) {
                double angle = (i / 7.0) * Math.PI * 0.8; // ~144-degree arc
                double x = Math.cos(angle) * 3.5;
                double y = Math.sin(angle) * 3.5 + 6;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float taper = 0.9f - (Math.abs(i - 3.5f) * 0.08f);
                h.scale(taper, 0.4f, 0.3f).glow(60, 0, 100).interpolation(2, 0);
                bladeArc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite spine reinforcing inner curve
            for (int i = 1; i < 7; i++) {
                double angle = (i / 7.0) * Math.PI * 0.8;
                double x = Math.cos(angle) * 3.0;
                double y = Math.sin(angle) * 3.0 + 6;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.NETHERITE_BLOCK);
                h.scale(0.3f, 0.35f, 0.25f).glow(20, 20, 30).interpolation(2, 0);
                bladeArc.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian blade tips
            BlockDisplayHandle tipA = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(0) * 3.5, Math.sin(0) * 3.5 + 6, 0), Material.CRYING_OBSIDIAN);
            tipA.scale(0.5f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(tipA.entity());
            bladeArc.add(tipA);

            BlockDisplayHandle tipB = displayBuilder.spawnBlock(
                    center.clone().add(Math.cos(0.8 * Math.PI) * 3.5, Math.sin(0.8 * Math.PI) * 3.5 + 6, 0), Material.CRYING_OBSIDIAN);
            tipB.scale(0.5f, 0.5f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(tipB.entity());
            bladeArc.add(tipB);

            // Dark prismarine cutting edge inlays
            for (int i = 2; i < 6; i++) {
                double angle = (i / 7.0) * Math.PI * 0.8;
                double x = Math.cos(angle) * 3.8;
                double y = Math.sin(angle) * 3.8 + 6;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle inlay = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                inlay.scale(0.2f, 0.25f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
                edgeInlays.add(inlay);
                spawnedEntities.add(inlay.entity());
            }

            // Haft: 6 blackstone blocks with joints
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, i, 0);
                Material mat = (i == 2 || i == 4) ? Material.POLISHED_BLACKSTONE : Material.BLACKSTONE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.4f, 1.0f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                haftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Counterweight at butt end
            counterweight = displayBuilder.spawnBlock(center.clone().add(0, -0.5, 0), Material.AMETHYST_BLOCK);
            counterweight.scale(0.5f, 0.5f, 0.5f).glow(180, 80, 255).interpolation(2, 0);
            spawnedEntities.add(counterweight.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y-axis rotation: 0.5 deg/tick
            float baseRot = ticksAlive * 0.00873f;

            // Every 80 ticks: rapid downward swipe (translate 3 blocks down in 15 ticks, snap back in 5)
            int cycleTick = ticksAlive % 80;
            float swipeOffset = 0;
            if (cycleTick >= 60 && cycleTick < 75) {
                // Down stroke: 15 ticks
                swipeOffset = ((cycleTick - 60) / 15.0f) * -3.0f;
                if (cycleTick == 60) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.3f);
                    DisplayBuilder.playSound(c, Sound.ENTITY_SKELETON_HURT, 0.6f, 0.5f);
                }
            } else if (cycleTick >= 75 && cycleTick < 80) {
                // Snap back: 5 ticks
                swipeOffset = -3.0f + ((cycleTick - 75) / 5.0f) * 3.0f;
            }

            // Apply rotation and swipe offset to all parts
            for (BlockDisplayHandle h : bladeArc) {
                BlockDisplay bd = h.entity();
                Location base = bd.getLocation();
                h.rotate(baseRot, 0, 1, 0);
                h.interpolation(2, 0);
            }
            for (BlockDisplayHandle h : haftBlocks) {
                h.rotate(baseRot, 0, 1, 0);
                h.interpolation(2, 0);
            }

            // Edge inlay brightness pulse: scale 1.0 to 1.6 every 30 ticks
            float inlayPulse = 0.2f + (float) Math.sin(ticksAlive * 0.209f) * 0.05f;
            for (BlockDisplayHandle inlay : edgeInlays) {
                inlay.scale(inlayPulse, 0.25f, 0.15f);
                inlay.interpolation(3, 0);
            }

            // Sweep attack particles from blade arc
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < Math.min(bladeArc.size(), 8); i += 2) {
                    Location bladeLoc = bladeArc.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, bladeLoc, 1, 0.2, 0.2, 0.2, 0);
                }
            }

            // Cyan dust along cutting edge
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle inlay : edgeInlays) {
                    DisplayBuilder.cyanDust(inlay.entity().getLocation(), 2, 0.2);
                }
            }

            // Dripping obsidian tears from blade tips
            if (ticksAlive % 6 == 0) {
                for (int i = bladeArc.size() - 2; i < bladeArc.size(); i++) {
                    if (i >= 0) {
                        c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                                bladeArc.get(i).entity().getLocation(), 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ReapingScythe(plugin); }
    }

    // ================================================================
    // 73. DREAD MAUL -- Massive netherite warhammer with slam attacks
    // ================================================================
    public static class DreadMaul extends BlockDisplayAttack {

        private BlockDisplayHandle hammerHead;
        private BlockDisplayHandle faceLeft;
        private BlockDisplayHandle faceRight;
        private final List<BlockDisplayHandle> haftBlocks = new ArrayList<>();
        private BlockDisplayHandle neckJoint;
        private final List<BlockDisplayHandle> battleMarks = new ArrayList<>();
        private float slamBaseY = 5.0f;

        public DreadMaul(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dread_maul", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(20.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(70); // Damage on slam cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Massive hammerhead: 4x3x2 netherite block
            hammerHead = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.NETHERITE_BLOCK);
            hammerHead.scale(4.0f, 3.0f, 2.0f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(hammerHead.entity());

            // Obsidian striking face plates
            faceLeft = displayBuilder.spawnBlock(center.clone().add(-2.1, 5, 0), Material.OBSIDIAN);
            faceLeft.scale(0.3f, 2.8f, 1.8f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(faceLeft.entity());

            faceRight = displayBuilder.spawnBlock(center.clone().add(2.1, 5, 0), Material.OBSIDIAN);
            faceRight.scale(0.3f, 2.8f, 1.8f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(faceRight.entity());

            // Crying obsidian battle marks on each face
            BlockDisplayHandle markL = displayBuilder.spawnBlock(center.clone().add(-2.2, 5.5, 0), Material.CRYING_OBSIDIAN);
            markL.scale(0.2f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            battleMarks.add(markL);
            spawnedEntities.add(markL.entity());

            BlockDisplayHandle markR = displayBuilder.spawnBlock(center.clone().add(2.2, 5.5, 0), Material.CRYING_OBSIDIAN);
            markR.scale(0.2f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
            battleMarks.add(markR);
            spawnedEntities.add(markR.entity());

            // Blackstone connecting neck
            neckJoint = displayBuilder.spawnBlock(center.clone().add(0, 4, 0), Material.BLACKSTONE);
            neckJoint.scale(1.2f, 1.0f, 1.2f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(neckJoint.entity());

            // Cobbled deepslate haft
            for (int i = 0; i < 4; i++) {
                Location loc = center.clone().add(0, i, 0);
                Material mat = (i == 1 || i == 3) ? Material.BLACK_GLAZED_TERRACOTTA : Material.COBBLED_DEEPSLATE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.7f, 1.0f, 0.7f).glow(50, 50, 60).interpolation(2, 0);
                haftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Polished blackstone grip
            BlockDisplayHandle grip = displayBuilder.spawnBlock(center.clone().add(0, 1.5, 0), Material.POLISHED_BLACKSTONE);
            grip.scale(0.8f, 0.5f, 0.8f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(grip.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_FALL, 1.0f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y rotation: 0.3 deg/tick
            float baseRot = ticksAlive * 0.00524f;

            // Every 70 ticks: overhead slam - drop 5 blocks in 10 ticks, bounce 1 block overshoot, settle in 15 ticks
            int cycleTick = ticksAlive % 70;
            float slamOffset = 0;
            if (cycleTick >= 45 && cycleTick < 55) {
                // Slam down: 10 ticks
                float progress = (cycleTick - 45) / 10.0f;
                slamOffset = -progress * 5.0f;
                if (cycleTick == 45) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.5f);
                    DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_FALL, 1.0f, 0.7f);
                }
            } else if (cycleTick >= 55 && cycleTick < 70) {
                // Bounce back with overshoot
                float progress = (cycleTick - 55) / 15.0f;
                float bounce = -5.0f + progress * 6.0f;
                if (progress > 0.33f) {
                    bounce = 1.0f - ((progress - 0.33f) / 0.67f) * 1.0f;
                }
                slamOffset = bounce;
            }

            // Apply slam offset to hammer head
            if (hammerHead != null) {
                hammerHead.entity().teleport(c.clone().add(0, slamBaseY + slamOffset, 0));
                hammerHead.rotate(baseRot, 0, 1, 0);
                hammerHead.interpolation(2, 0);

                // Z-axis tremor after slam
                if (cycleTick >= 55) {
                    float tremor = (float) Math.sin(ticksAlive * 1.57) * 0.05f;
                    hammerHead.translate(-2.0f, -1.5f, -1.0f + tremor);
                }
            }

            // Move face plates with hammer
            if (faceLeft != null) {
                faceLeft.entity().teleport(c.clone().add(-2.1, slamBaseY + slamOffset, 0));
                faceLeft.rotate(baseRot, 0, 1, 0);
                faceLeft.interpolation(2, 0);
            }
            if (faceRight != null) {
                faceRight.entity().teleport(c.clone().add(2.1, slamBaseY + slamOffset, 0));
                faceRight.rotate(baseRot, 0, 1, 0);
                faceRight.interpolation(2, 0);
            }

            // Explosion particle on slam impact
            if (cycleTick == 55) {
                Location impact = c.clone().add(0, slamBaseY - 5.0, 0);
                c.getWorld().spawnParticle(Particle.EXPLOSION, impact, 3, 1.5, 0.5, 1.5, 0);
                // Purple shockwave ring
                DisplayBuilder.particleRing(impact, 3.0, Particle.DUST, 20,
                        new Particle.DustOptions(Color.fromRGB(128, 0, 255), 2.0f));
            }

            // Iron-purple dust surrounding hammerhead
            if (ticksAlive % 3 == 0 && hammerHead != null) {
                Location headLoc = hammerHead.entity().getLocation().add(0, 1.5, 0);
                DisplayBuilder.dustParticles(headLoc, 4, 1.5, 80, 70, 100, 1.0f);
            }

            // Crying obsidian tears from battle marks
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle mark : battleMarks) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            mark.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DreadMaul(plugin); }
    }

    // ================================================================
    // 74. VOID CLAW -- 5-fingered grasping claw that opens and snaps shut
    // ================================================================
    public static class VoidClaw extends BlockDisplayAttack {

        private BlockDisplayHandle palm;
        private BlockDisplayHandle palmSocket;
        private final List<List<BlockDisplayHandle>> fingers = new ArrayList<>();
        private final List<BlockDisplayHandle> clawTips = new ArrayList<>();
        private final List<BlockDisplayHandle> knuckles = new ArrayList<>();

        public VoidClaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("void_claw", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(14.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(60); // Damage on grasp cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Palm base: 3 obsidian blocks
            palm = displayBuilder.spawnBlock(center, Material.OBSIDIAN);
            palm.scale(2.5f, 1.0f, 2.5f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(palm.entity());

            // Dark prismarine glowing socket at palm center
            palmSocket = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0), Material.DARK_PRISMARINE);
            palmSocket.scale(0.8f, 0.4f, 0.8f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(palmSocket.entity());

            // 5 finger digits radiating from palm
            double[] fingerAngles = {0, Math.PI * 0.4, Math.PI * 0.8, Math.PI * 1.2, Math.PI * 1.6};
            for (int f = 0; f < 5; f++) {
                List<BlockDisplayHandle> finger = new ArrayList<>();
                double angle = fingerAngles[f];
                for (int seg = 0; seg < 3; seg++) {
                    double dist = 1.5 + seg * 1.2;
                    double x = Math.cos(angle) * dist;
                    double z = Math.sin(angle) * dist;
                    double y = 0.5 + seg * 0.4;
                    Location loc = center.clone().add(x, y, z);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    float taper = 0.6f - (seg * 0.12f);
                    h.scale(taper, 0.5f, taper).glow(50, 50, 60).interpolation(2, 0);
                    finger.add(h);
                    spawnedEntities.add(h.entity());
                }
                fingers.add(finger);

                // Polished blackstone fingertip claw
                double tipDist = 1.5 + 3 * 1.2;
                Location tipLoc = center.clone().add(
                        Math.cos(angle) * tipDist, 1.7, Math.sin(angle) * tipDist);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.POLISHED_BLACKSTONE);
                tip.scale(0.3f, 0.7f, 0.2f).glow(60, 60, 70).interpolation(2, 0);
                clawTips.add(tip);
                spawnedEntities.add(tip.entity());

                // Crying obsidian at the very tip
                Location cryLoc = tipLoc.clone().add(Math.cos(angle) * 0.3, 0.4, Math.sin(angle) * 0.3);
                BlockDisplayHandle cry = displayBuilder.spawnBlock(cryLoc, Material.CRYING_OBSIDIAN);
                cry.scale(0.2f, 0.3f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
                spawnedEntities.add(cry.entity());
                clawTips.add(cry);

                // Netherite knuckle on first 3 fingers
                if (f < 3) {
                    double knuckleDist = 1.5;
                    Location kLoc = center.clone().add(Math.cos(angle) * knuckleDist, 0.8, Math.sin(angle) * knuckleDist);
                    BlockDisplayHandle k = displayBuilder.spawnBlock(kLoc, Material.NETHERITE_BLOCK);
                    k.scale(0.35f, 0.35f, 0.35f).glow(20, 20, 30).interpolation(2, 0);
                    knuckles.add(k);
                    spawnedEntities.add(k.entity());
                }
            }

            // Cobbled deepslate wrist stubs
            BlockDisplayHandle wristA = displayBuilder.spawnBlock(center.clone().add(0, -0.5, -0.8), Material.COBBLED_DEEPSLATE);
            wristA.scale(1.0f, 0.6f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(wristA.entity());
            BlockDisplayHandle wristB = displayBuilder.spawnBlock(center.clone().add(0, -0.5, 0.8), Material.COBBLED_DEEPSLATE);
            wristB.scale(1.0f, 0.6f, 0.6f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(wristB.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_RAVAGER_ATTACK, 0.8f, 0.5f);
            DisplayBuilder.playSound(center, Sound.ENTITY_WARDEN_DIG, 0.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y rotation: 0.6 deg/tick
            float baseRot = ticksAlive * 0.01047f;

            // Every 60 ticks: fingers curl inward over 20 ticks, snap open in 5 ticks
            int cycleTick = ticksAlive % 60;
            float curlFactor = 0;
            if (cycleTick >= 35 && cycleTick < 55) {
                // Curling inward: 20 ticks
                curlFactor = (cycleTick - 35) / 20.0f;
            } else if (cycleTick >= 55 && cycleTick < 60) {
                // Snap open: 5 ticks
                curlFactor = 1.0f - ((cycleTick - 55) / 5.0f);
                if (cycleTick == 55) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_RAVAGER_ATTACK, 1.0f, 0.5f);
                }
            }

            // Apply rotation to palm
            if (palm != null) {
                palm.rotate(baseRot, 0, 1, 0);
                palm.interpolation(2, 0);
            }

            // Palm socket pulse: scale 1.0 to 1.8 every 40 ticks
            if (palmSocket != null) {
                float socketPulse = 0.8f + (float) Math.sin(ticksAlive * 0.157f) * 0.3f;
                palmSocket.scale(socketPulse, 0.4f, socketPulse);
                palmSocket.interpolation(3, 0);
            }

            // Finger curl animation: move tips toward center
            double[] fingerAngles = {0, Math.PI * 0.4, Math.PI * 0.8, Math.PI * 1.2, Math.PI * 1.6};
            for (int f = 0; f < 5 && f < fingers.size(); f++) {
                List<BlockDisplayHandle> finger = fingers.get(f);
                double angle = fingerAngles[f];
                for (int seg = 0; seg < finger.size(); seg++) {
                    double baseDist = 1.5 + seg * 1.2;
                    double curledDist = baseDist * (1.0 - curlFactor * 0.6);
                    double x = Math.cos(angle) * curledDist;
                    double z = Math.sin(angle) * curledDist;
                    double baseY = 0.5 + seg * 0.4;
                    double curledY = baseY + curlFactor * seg * 0.3;
                    Location target = c.clone().add(x, curledY, z);
                    finger.get(seg).entity().teleport(target);
                    finger.get(seg).rotate(baseRot, 0, 1, 0);
                    finger.get(seg).interpolation(2, 0);
                }
            }

            // Crit particles from each claw tip
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < clawTips.size(); i += 2) {
                    Location tipLoc = clawTips.get(i).entity().getLocation();
                    c.getWorld().spawnParticle(Particle.CRIT, tipLoc, 3, 0.1, 0.1, 0.1, 0.1);
                }
            }

            // Dark charcoal dust from palm
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 4, 1.0, 30, 10, 50, 1.0f);
            }

            // Obsidian tears from crying obsidian tips
            if (ticksAlive % 6 == 0) {
                for (int i = 1; i < clawTips.size(); i += 2) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            clawTips.get(i).entity().getLocation(), 2, 0.05, 0.15, 0.05, 0);
                }
            }

            // Warden dig ambient sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_WARDEN_DIG, 0.3f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new VoidClaw(plugin); }
    }

    // ================================================================
    // 75. OBSIDIAN ANCHOR -- Enormous swinging anchor with chain links
    // ================================================================
    public static class ObsidianAnchor extends BlockDisplayAttack {

        private BlockDisplayHandle shank;
        private BlockDisplayHandle crownBar;
        private BlockDisplayHandle stockBar;
        private final List<BlockDisplayHandle> flukes = new ArrayList<>();
        private final List<BlockDisplayHandle> chainLinks = new ArrayList<>();
        private BlockDisplayHandle depthGlow;
        private final List<BlockDisplayHandle> flukeTips = new ArrayList<>();

        public ObsidianAnchor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("obsidian_anchor", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(12.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(500);
            config.setCooldownTicks(400);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Anchor shank: 6-block vertical obsidian bar
            shank = displayBuilder.spawnBlock(center.clone().add(0, 0, 0), Material.OBSIDIAN);
            shank.scale(0.8f, 6.0f, 0.8f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(shank.entity());

            // Crown at top with ring attachment
            crownBar = displayBuilder.spawnBlock(center.clone().add(0, 6, 0), Material.OBSIDIAN);
            crownBar.scale(2.0f, 0.6f, 0.6f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(crownBar.entity());

            // Polished blackstone ring
            BlockDisplayHandle ring = displayBuilder.spawnBlock(center.clone().add(0, 6.5, 0), Material.POLISHED_BLACKSTONE);
            ring.scale(1.0f, 0.5f, 1.0f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(ring.entity());

            // Stock: horizontal crossbar at top
            stockBar = displayBuilder.spawnBlock(center.clone().add(0, 5, 0), Material.BLACKSTONE);
            stockBar.scale(3.0f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(stockBar.entity());

            // Flukes: two curving arms at bottom
            for (int side = -1; side <= 1; side += 2) {
                for (int seg = 0; seg < 3; seg++) {
                    double x = side * (0.8 + seg * 0.9);
                    double y = -seg * 0.6;
                    Location loc = center.clone().add(x, y, 0);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                    float taper = 0.7f - (seg * 0.15f);
                    h.scale(taper, 0.6f, taper).glow(40, 0, 60).interpolation(2, 0);
                    flukes.add(h);
                    spawnedEntities.add(h.entity());
                }
                // Crying obsidian fluke tip
                Location tipLoc = center.clone().add(side * 3.5, -1.8, 0);
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, Material.CRYING_OBSIDIAN);
                tip.scale(0.4f, 0.4f, 0.4f).glow(128, 0, 255).interpolation(2, 0);
                flukeTips.add(tip);
                spawnedEntities.add(tip.entity());
            }

            // Crying obsidian at ring top
            BlockDisplayHandle ringCry = displayBuilder.spawnBlock(center.clone().add(0, 7, 0), Material.CRYING_OBSIDIAN);
            ringCry.scale(0.5f, 0.4f, 0.5f).glow(128, 0, 255).interpolation(2, 0);
            flukeTips.add(ringCry);
            spawnedEntities.add(ringCry.entity());

            // Dark prismarine depth glow in shank center
            depthGlow = displayBuilder.spawnBlock(center.clone().add(0, 3, 0), Material.DARK_PRISMARINE);
            depthGlow.scale(0.5f, 0.8f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(depthGlow.entity());

            // Netherite chain links above
            for (int i = 0; i < 3; i++) {
                Location linkLoc = center.clone().add(0, 7.5 + i * 1.2, 0);
                BlockDisplayHandle link = displayBuilder.spawnBlock(linkLoc, Material.NETHERITE_BLOCK);
                link.scale(0.4f, 0.8f, 0.4f).glow(20, 20, 30).interpolation(2, 0);
                chainLinks.add(link);
                spawnedEntities.add(link.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_HIT, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing: +/-8 degrees on Z-axis over 100 ticks
            float pendulumAngle = (float) Math.sin(ticksAlive * 0.0628f) * 0.14f; // ~8 deg

            // Apply pendulum to shank and all attached parts
            if (shank != null) {
                shank.rotate(pendulumAngle, 0, 0, 1);
                shank.interpolation(3, 0);
            }
            if (crownBar != null) {
                crownBar.rotate(pendulumAngle, 0, 0, 1);
                crownBar.interpolation(3, 0);
            }
            if (stockBar != null) {
                stockBar.rotate(pendulumAngle, 0, 0, 1);
                stockBar.interpolation(3, 0);
            }

            // Chain link individual rotation: 2 deg/tick
            for (int i = 0; i < chainLinks.size(); i++) {
                float chainRot = ticksAlive * 0.0349f + i * 0.5f;
                chainLinks.get(i).rotate(chainRot, 0, 1, 0);
                chainLinks.get(i).interpolation(2, 0);
            }

            // Every 150 ticks: anchor drop (4 blocks down in 10 ticks, yank back in 20)
            int cycleTick = ticksAlive % 150;
            float dropOffset = 0;
            if (cycleTick >= 120 && cycleTick < 130) {
                dropOffset = ((cycleTick - 120) / 10.0f) * -4.0f;
                if (cycleTick == 120) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_SPLASH, 0.7f, 0.4f);
                }
            } else if (cycleTick >= 130 && cycleTick < 150) {
                dropOffset = -4.0f + ((cycleTick - 130) / 20.0f) * 4.0f;
            }

            // Apply drop to all anchor parts by teleporting shank
            if (shank != null && dropOffset != 0) {
                Location shankLoc = c.clone().add(0, dropOffset, 0);
                shank.entity().teleport(shankLoc);
            }

            // Sound on pendulum extremes
            if (Math.abs(Math.sin(ticksAlive * 0.0628f)) > 0.98f) {
                DisplayBuilder.playSound(c, Sound.BLOCK_CHAIN_HIT, 0.5f, 0.7f);
            }

            // Bubble column particles trailing upward (void-water effect)
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(0, 2, 0), 5,
                        0.5, 2.0, 0.5, 0.02);
            }

            // Abyssal blue dust surrounding anchor
            if (ticksAlive % 4 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 8, 2.0, 0, 20, 80, 1.5f);
            }

            // Obsidian tears from fluke tips
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle tip : flukeTips) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            tip.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ObsidianAnchor(plugin); }
    }

    // ================================================================
    // 76. THOUSAND SPEARS -- Radial sea-urchin of spears that thrust outward
    // ================================================================
    public static class ThousandSpears extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spearShafts = new ArrayList<>();
        private final List<BlockDisplayHandle> spearTips = new ArrayList<>();
        private final List<BlockDisplayHandle> buttCaps = new ArrayList<>();
        private BlockDisplayHandle nexus;
        private BlockDisplayHandle polarTop;
        private BlockDisplayHandle polarBottom;
        private final double[][] spearDirections = new double[8][3];
        private float thrustOffset = 0;

        public ThousandSpears(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thousand_spears", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(18.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(360);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(50); // Damage on thrust cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Netherite nexus at center
            nexus = displayBuilder.spawnBlock(center, Material.NETHERITE_BLOCK);
            nexus.scale(0.8f, 0.8f, 0.8f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(nexus.entity());

            // 8 radial spear directions: 6 cardinal/intercardinal + 2 polar (up/down)
            spearDirections[0] = new double[]{1, 0, 0};
            spearDirections[1] = new double[]{-1, 0, 0};
            spearDirections[2] = new double[]{0, 0, 1};
            spearDirections[3] = new double[]{0, 0, -1};
            spearDirections[4] = new double[]{0.707, 0, 0.707};
            spearDirections[5] = new double[]{-0.707, 0, -0.707};
            spearDirections[6] = new double[]{0, 1, 0};    // Up
            spearDirections[7] = new double[]{0, -1, 0};   // Down

            for (int s = 0; s < 8; s++) {
                double[] dir = spearDirections[s];
                // Shaft: 2 blackstone blocks
                for (int seg = 0; seg < 2; seg++) {
                    double dist = 0.8 + seg * 1.0;
                    Location loc = center.clone().add(dir[0] * dist, dir[1] * dist, dir[2] * dist);
                    BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                    h.scale(0.35f, 0.35f, 0.35f).glow(50, 50, 60).interpolation(2, 0);
                    spearShafts.add(h);
                    spawnedEntities.add(h.entity());
                }

                // Obsidian spearhead tip
                double tipDist = 3.0;
                Location tipLoc = center.clone().add(dir[0] * tipDist, dir[1] * tipDist, dir[2] * tipDist);
                Material tipMat = (s < 4) ? Material.CRYING_OBSIDIAN : Material.OBSIDIAN;
                BlockDisplayHandle tip = displayBuilder.spawnBlock(tipLoc, tipMat);
                tip.scale(0.25f, 0.25f, 0.5f).glow(s < 4 ? 128 : 60, 0, s < 4 ? 255 : 100)
                        .interpolation(2, 0);
                spearTips.add(tip);
                spawnedEntities.add(tip.entity());

                // Cobbled deepslate butt cap
                double buttDist = 0.5;
                Location buttLoc = center.clone().add(-dir[0] * buttDist, -dir[1] * buttDist, -dir[2] * buttDist);
                BlockDisplayHandle butt = displayBuilder.spawnBlock(buttLoc, Material.COBBLED_DEEPSLATE);
                butt.scale(0.25f, 0.25f, 0.25f).glow(50, 50, 60).interpolation(2, 0);
                buttCaps.add(butt);
                spawnedEntities.add(butt.entity());
            }

            // Amethyst charged tips on polar spears (indices 6, 7)
            polarTop = displayBuilder.spawnBlock(center.clone().add(0, 3.3, 0), Material.AMETHYST_BLOCK);
            polarTop.scale(0.3f, 0.4f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(polarTop.entity());

            polarBottom = displayBuilder.spawnBlock(center.clone().add(0, -3.3, 0), Material.AMETHYST_BLOCK);
            polarBottom.scale(0.3f, 0.4f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(polarBottom.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_ARROW_HIT, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_STEP, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Whole array Y-axis rotation: 0.6 deg/tick
            float arrayRot = ticksAlive * 0.01047f;

            // Nexus spins on all axes: 2 deg/tick
            if (nexus != null) {
                float nexusRot = ticksAlive * 0.0349f;
                nexus.rotate(nexusRot, 0.577f, 0.577f, 0.577f); // Normalized (1,1,1)
                nexus.interpolation(2, 0);
            }

            // Every 50 ticks: all spears thrust outward 1.5 blocks in 15 ticks, retract in 10 ticks
            int cycleTick = ticksAlive % 50;
            if (cycleTick < 15) {
                thrustOffset = (cycleTick / 15.0f) * 1.5f;
                if (cycleTick == 0) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_HIT, 1.0f, 0.8f);
                }
            } else if (cycleTick < 25) {
                thrustOffset = 1.5f - ((cycleTick - 15) / 10.0f) * 1.5f;
            } else {
                thrustOffset = 0;
            }

            // Update spear positions with rotation and thrust
            for (int s = 0; s < 8; s++) {
                double[] dir = spearDirections[s];
                // Rotate direction by arrayRot
                double cosR = Math.cos(arrayRot);
                double sinR = Math.sin(arrayRot);
                double rx = dir[0] * cosR - dir[2] * sinR;
                double rz = dir[0] * sinR + dir[2] * cosR;
                double ry = dir[1];

                // Shaft blocks
                for (int seg = 0; seg < 2; seg++) {
                    int idx = s * 2 + seg;
                    if (idx < spearShafts.size()) {
                        double dist = 0.8 + seg * 1.0 + thrustOffset;
                        Location loc = c.clone().add(rx * dist, ry * dist, rz * dist);
                        spearShafts.get(idx).entity().teleport(loc);
                    }
                }

                // Tip
                if (s < spearTips.size()) {
                    double tipDist = 3.0 + thrustOffset;
                    Location tipLoc = c.clone().add(rx * tipDist, ry * tipDist, rz * tipDist);
                    spearTips.get(s).entity().teleport(tipLoc);
                }

                // Butt cap
                if (s < buttCaps.size()) {
                    double buttDist = 0.5 - thrustOffset * 0.3;
                    Location buttLoc = c.clone().add(-rx * buttDist, -ry * buttDist, -rz * buttDist);
                    buttCaps.get(s).entity().teleport(buttLoc);
                }
            }

            // Amethyst polar tip pulse: scale 1.0 to 1.5 every 30 ticks
            float polarPulse = 0.3f + (float) Math.sin(ticksAlive * 0.209f) * 0.08f;
            if (polarTop != null) {
                polarTop.scale(polarPulse, 0.4f + polarPulse * 0.3f, polarPulse);
                polarTop.interpolation(3, 0);
            }
            if (polarBottom != null) {
                polarBottom.scale(polarPulse, 0.4f + polarPulse * 0.3f, polarPulse);
                polarBottom.interpolation(3, 0);
            }

            // Crit particles from all spear tips
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle tip : spearTips) {
                    c.getWorld().spawnParticle(Particle.CRIT, tip.entity().getLocation(), 2, 0.1, 0.1, 0.1, 0.05);
                }
            }

            // Steel dust tracing shafts
            if (ticksAlive % 5 == 0) {
                for (int i = 0; i < spearShafts.size(); i += 4) {
                    DisplayBuilder.dustParticles(spearShafts.get(i).entity().getLocation(), 2, 0.2,
                            100, 90, 120, 0.8f);
                }
            }

            // Reverse portal from nexus center
            if (ticksAlive % 2 == 0 && nexus != null) {
                c.getWorld().spawnParticle(Particle.REVERSE_PORTAL, nexus.entity().getLocation(), 5,
                        0.2, 0.2, 0.2, 0.05);
            }

            // Continuous ambient
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_STEP, 0.5f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new ThousandSpears(plugin); }
    }

    // ================================================================
    // 77. SIEGE BALLISTA -- War machine that fires bolt projectiles
    // ================================================================
    public static class SiegeBallista extends BlockDisplayAttack {

        private BlockDisplayHandle chassis;
        private final List<BlockDisplayHandle> frameBlocks = new ArrayList<>();
        private BlockDisplayHandle armLeft;
        private BlockDisplayHandle armRight;
        private BlockDisplayHandle winch;
        private BlockDisplayHandle bolt;
        private final List<BlockDisplayHandle> skeinCords = new ArrayList<>();
        private boolean boltFired = false;
        private int boltFireTick = 0;
        private float boltDistance = 0;

        public SiegeBallista(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("siege_ballista", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(480);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(120); // Damage on bolt fire
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Chassis: blackstone frame body
            chassis = displayBuilder.spawnBlock(center, Material.BLACKSTONE);
            chassis.scale(3.0f, 1.0f, 2.0f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(chassis.entity());

            // Wheels: 4 rounded blackstone at corners
            double[][] wheelPos = {{1.2, -0.5, 0.9}, {1.2, -0.5, -0.9}, {-1.2, -0.5, 0.9}, {-1.2, -0.5, -0.9}};
            for (double[] pos : wheelPos) {
                Location wLoc = center.clone().add(pos[0], pos[1], pos[2]);
                BlockDisplayHandle wheel = displayBuilder.spawnBlock(wLoc, Material.BLACKSTONE);
                wheel.scale(0.5f, 0.5f, 0.5f).glow(50, 50, 60).interpolation(2, 0);
                frameBlocks.add(wheel);
                spawnedEntities.add(wheel.entity());
            }

            // Rail trough down center
            BlockDisplayHandle trough = displayBuilder.spawnBlock(center.clone().add(0, 0.3, 0), Material.BLACKSTONE);
            trough.scale(2.5f, 0.2f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
            frameBlocks.add(trough);
            spawnedEntities.add(trough.entity());

            // Massive prod arms (2 obsidian arms extending from front)
            armLeft = displayBuilder.spawnBlock(center.clone().add(1.5, 0.5, 1.5), Material.OBSIDIAN);
            armLeft.scale(0.35f, 0.35f, 2.0f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(armLeft.entity());

            armRight = displayBuilder.spawnBlock(center.clone().add(1.5, 0.5, -1.5), Material.OBSIDIAN);
            armRight.scale(0.35f, 0.35f, 2.0f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(armRight.entity());

            // Crying obsidian at arm tips (string attachment)
            BlockDisplayHandle tipL = displayBuilder.spawnBlock(center.clone().add(1.5, 0.5, 3.5), Material.CRYING_OBSIDIAN);
            tipL.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(tipL.entity());
            BlockDisplayHandle tipR = displayBuilder.spawnBlock(center.clone().add(1.5, 0.5, -3.5), Material.CRYING_OBSIDIAN);
            tipR.scale(0.3f, 0.3f, 0.3f).glow(128, 0, 255).interpolation(2, 0);
            spawnedEntities.add(tipR.entity());

            // Netherite winch mechanism at rear
            winch = displayBuilder.spawnBlock(center.clone().add(-1.5, 0.5, 0), Material.NETHERITE_BLOCK);
            winch.scale(0.8f, 0.8f, 1.2f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(winch.entity());

            // Trigger housing
            BlockDisplayHandle trigger = displayBuilder.spawnBlock(center.clone().add(-1.5, 0.8, 0), Material.BLACK_GLAZED_TERRACOTTA);
            trigger.scale(0.3f, 0.3f, 0.3f).glow(30, 30, 40).interpolation(2, 0);
            spawnedEntities.add(trigger.entity());

            // Dark prismarine skein cords (glowing under tension)
            BlockDisplayHandle skeinL = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 1.0), Material.DARK_PRISMARINE);
            skeinL.scale(0.15f, 0.15f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
            skeinCords.add(skeinL);
            spawnedEntities.add(skeinL.entity());
            BlockDisplayHandle skeinR = displayBuilder.spawnBlock(center.clone().add(0, 0.5, -1.0), Material.DARK_PRISMARINE);
            skeinR.scale(0.15f, 0.15f, 2.0f).glow(0, 200, 255).interpolation(2, 0);
            skeinCords.add(skeinR);
            spawnedEntities.add(skeinR.entity());

            // Bolt: 3 cobbled deepslate blocks in trough
            bolt = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.COBBLED_DEEPSLATE);
            bolt.scale(2.0f, 0.3f, 0.3f).glow(60, 60, 70).interpolation(1, 0);
            spawnedEntities.add(bolt.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Very slow Y rocking: 0.15 deg/tick
            float rocking = (float) Math.sin(ticksAlive * 0.00262f) * 0.01f;
            if (chassis != null) {
                chassis.rotate(rocking, 0, 1, 0);
                chassis.interpolation(3, 0);
            }

            // Arm tension cycle: extend 0.3 blocks on X-axis and snap back every 80 ticks
            int tensionCycle = ticksAlive % 80;
            float armExtend = 0;
            if (tensionCycle >= 70 && tensionCycle < 78) {
                armExtend = ((tensionCycle - 70) / 8.0f) * 0.3f;
            } else if (tensionCycle >= 78) {
                armExtend = 0.3f - ((tensionCycle - 78) / 2.0f) * 0.3f;
                if (tensionCycle == 78) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_IRON_GOLEM_ATTACK, 0.6f, 0.6f);
                    // Sweep particles from arm tips
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(1.5, 0.5, 3.5), 3, 0.3, 0.3, 0.3, 0);
                    c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(1.5, 0.5, -3.5), 3, 0.3, 0.3, 0.3, 0);
                }
            }

            // Apply arm spread
            if (armLeft != null) {
                armLeft.entity().teleport(c.clone().add(1.5, 0.5, 1.5 + armExtend));
            }
            if (armRight != null) {
                armRight.entity().teleport(c.clone().add(1.5, 0.5, -1.5 - armExtend));
            }

            // Every 120 ticks: bolt fires forward 8 blocks in 5 ticks, vanishes, reappears at origin
            int boltCycle = ticksAlive % 120;
            if (bolt != null) {
                if (boltCycle >= 100 && boltCycle < 105) {
                    // Bolt launches forward
                    float boltProgress = (boltCycle - 100) / 5.0f;
                    boltDistance = boltProgress * 8.0f;
                    bolt.entity().teleport(c.clone().add(boltDistance, 0.5, 0));
                    bolt.scale(2.0f, 0.3f, 0.3f);

                    if (boltCycle == 100) {
                        DisplayBuilder.playSound(c, Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.5f);
                    }

                    // Firework spark trail along bolt path
                    c.getWorld().spawnParticle(Particle.FIREWORK, c.clone().add(boltDistance, 0.5, 0), 10,
                            0.2, 0.2, 0.2, 0.05);
                } else if (boltCycle >= 105 && boltCycle < 108) {
                    // Bolt vanishes (scale to 0)
                    bolt.scale(0.01f, 0.01f, 0.01f);
                    bolt.interpolation(1, 0);
                } else if (boltCycle >= 108) {
                    // Bolt reappears at origin
                    bolt.entity().teleport(c.clone().add(0, 0.5, 0));
                    bolt.scale(2.0f, 0.3f, 0.3f);
                    bolt.interpolation(2, 0);
                }
            }

            // Tension cord orange dust
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle skein : skeinCords) {
                    DisplayBuilder.dustParticles(skein.entity().getLocation(), 3, 0.5, 180, 80, 0, 0.8f);
                }
            }

            // Winch rotation
            if (winch != null) {
                float winchRot = ticksAlive * 0.02f;
                winch.rotate(winchRot, 0, 0, 1);
                winch.interpolation(2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SiegeBallista(plugin); }
    }

    // ================================================================
    // 78. RAZOR PENDULUM -- Fast swinging blade pendulum with spinning bob
    // ================================================================
    public static class RazorPendulum extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private BlockDisplayHandle bobDisc;
        private BlockDisplayHandle mountBracket;
        private BlockDisplayHandle pivotPin;
        private BlockDisplayHandle amethystEye;
        private final List<BlockDisplayHandle> razorEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> decorRings = new ArrayList<>();

        public RazorPendulum(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("razor_pendulum", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(16.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(30); // Frequent damage on swing arc
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Mounting bracket at top
            mountBracket = displayBuilder.spawnBlock(center.clone().add(0, 8, 0), Material.NETHERITE_BLOCK);
            mountBracket.scale(2.0f, 0.8f, 1.0f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(mountBracket.entity());

            // Pivot pin
            pivotPin = displayBuilder.spawnBlock(center.clone().add(0, 8.5, 0), Material.NETHERITE_BLOCK);
            pivotPin.scale(0.4f, 0.3f, 0.4f).glow(20, 20, 30).interpolation(2, 0);
            spawnedEntities.add(pivotPin.entity());

            // Dark prismarine fulcrum accent
            BlockDisplayHandle fulcrumL = displayBuilder.spawnBlock(center.clone().add(-0.8, 8, 0), Material.DARK_PRISMARINE);
            fulcrumL.scale(0.3f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(fulcrumL.entity());
            BlockDisplayHandle fulcrumR = displayBuilder.spawnBlock(center.clone().add(0.8, 8, 0), Material.DARK_PRISMARINE);
            fulcrumR.scale(0.3f, 0.5f, 0.5f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(fulcrumR.entity());

            // Pendulum shaft: 6 obsidian blocks hanging down
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, 7 - i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.5f, 1.0f, 0.4f).glow(40, 0, 60).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Heavy bob disc at bottom
            bobDisc = displayBuilder.spawnBlock(center.clone().add(0, 0.5, 0), Material.OBSIDIAN);
            bobDisc.scale(2.0f, 1.0f, 2.0f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(bobDisc.entity());

            // Polished blackstone decorative rings around bob
            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2 * i) / 3;
                Location rLoc = center.clone().add(Math.cos(angle) * 1.2, 0.5, Math.sin(angle) * 1.2);
                BlockDisplayHandle ring = displayBuilder.spawnBlock(rLoc, Material.POLISHED_BLACKSTONE);
                ring.scale(0.3f, 0.8f, 0.3f).glow(60, 60, 70).interpolation(2, 0);
                decorRings.add(ring);
                spawnedEntities.add(ring.entity());
            }

            // Crying obsidian razor edges on bob
            BlockDisplayHandle edgeA = displayBuilder.spawnBlock(center.clone().add(1.2, 0.5, 0), Material.CRYING_OBSIDIAN);
            edgeA.scale(0.3f, 0.6f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
            razorEdges.add(edgeA);
            spawnedEntities.add(edgeA.entity());

            BlockDisplayHandle edgeB = displayBuilder.spawnBlock(center.clone().add(-1.2, 0.5, 0), Material.CRYING_OBSIDIAN);
            edgeB.scale(0.3f, 0.6f, 1.5f).glow(128, 0, 255).interpolation(2, 0);
            razorEdges.add(edgeB);
            spawnedEntities.add(edgeB.entity());

            // Amethyst eye at bob center
            amethystEye = displayBuilder.spawnBlock(center.clone().add(0, 0.8, 0), Material.AMETHYST_BLOCK);
            amethystEye.scale(0.6f, 0.6f, 0.6f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(amethystEye.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.6f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Pendulum swing: +/-60 degrees on Z-axis, 60-tick period (fast and menacing)
            // Full period = 60 ticks = 3 seconds, angular velocity: 2*PI/60 = 0.1047 rad/tick
            float swingAngle = (float) Math.sin(ticksAlive * 0.1047f) * 1.047f; // +/-60 degrees

            // Calculate bob position based on pendulum physics
            float pendulumLength = 7.0f; // 7 blocks from pivot to bob
            float bobX = (float) Math.sin(swingAngle) * pendulumLength;
            float bobY = 8.0f - (float) Math.cos(swingAngle) * pendulumLength;

            // Bob Y-axis spin: 2 deg/tick
            float bobSpin = ticksAlive * 0.0349f;

            // Move shaft blocks along pendulum arc
            for (int i = 0; i < shaftBlocks.size(); i++) {
                float segDist = (i + 1.0f) / 7.0f; // 0.14 to 0.86
                float sx = (float) Math.sin(swingAngle) * segDist * pendulumLength;
                float sy = 8.0f - (float) Math.cos(swingAngle) * segDist * pendulumLength;
                shaftBlocks.get(i).entity().teleport(c.clone().add(sx, sy, 0));
                shaftBlocks.get(i).rotate(swingAngle, 0, 0, 1);
                shaftBlocks.get(i).interpolation(2, 0);
            }

            // Move bob disc
            if (bobDisc != null) {
                bobDisc.entity().teleport(c.clone().add(bobX, bobY, 0));
                // Combine swing tilt with Y spin
                bobDisc.rotate(bobSpin, 0, 1, 0);
                bobDisc.interpolation(2, 0);
            }

            // Amethyst eye pulsing: scale 0.5 to 1.3 per full swing
            if (amethystEye != null) {
                float eyeScale = 0.6f + Math.abs((float) Math.sin(ticksAlive * 0.1047f)) * 0.5f;
                amethystEye.entity().teleport(c.clone().add(bobX, bobY + 0.3, 0));
                amethystEye.scale(eyeScale, eyeScale, eyeScale);
                amethystEye.interpolation(3, 0);
            }

            // Move razor edges with bob
            if (razorEdges.size() >= 2) {
                razorEdges.get(0).entity().teleport(c.clone().add(bobX + 1.2, bobY, 0));
                razorEdges.get(1).entity().teleport(c.clone().add(bobX - 1.2, bobY, 0));
            }

            // Move decor rings with bob
            for (int i = 0; i < decorRings.size(); i++) {
                double ringAngle = (Math.PI * 2 * i) / 3 + bobSpin;
                decorRings.get(i).entity().teleport(c.clone().add(
                        bobX + Math.cos(ringAngle) * 1.2, bobY, Math.sin(ringAngle) * 1.2));
            }

            // Sound on every full swing
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(c.clone().add(bobX, bobY, 0), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
            }

            // Chain creak continuous
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(c.clone().add(0, 8, 0), Sound.BLOCK_CHAIN_PLACE, 0.3f, 0.5f);
            }

            // Sweep attack particles along swing arc
            if (ticksAlive % 2 == 0) {
                c.getWorld().spawnParticle(Particle.SWEEP_ATTACK, c.clone().add(bobX, bobY + 0.5, 0),
                        3, 0.8, 0.3, 0.8, 0);
            }

            // Pendulum gleam dust tracing arc
            if (ticksAlive % 3 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(bobX, bobY, 0), 5, 1.0,
                        200, 200, 220, 1.2f);
            }

            // Dripping obsidian tears from razor edges on each swing
            if (ticksAlive % 4 == 0) {
                for (BlockDisplayHandle edge : razorEdges) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            edge.entity().getLocation(), 2, 0.2, 0.1, 0.2, 0);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RazorPendulum(plugin); }
    }

    // ================================================================
    // 79. ABYSSAL TRIDENT -- Hovering trident with boomerang throw
    // ================================================================
    public static class AbyssalTrident extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> shaftBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tineBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> tineTips = new ArrayList<>();
        private final List<BlockDisplayHandle> barbAccents = new ArrayList<>();
        private BlockDisplayHandle crossguard;
        private BlockDisplayHandle runeInlay;
        private final List<BlockDisplayHandle> pommelGems = new ArrayList<>();
        private BlockDisplayHandle handgrip;
        private boolean thrown = false;
        private float throwDistance = 0;
        private int throwPhase = 0; // 0=idle, 1=forward, 2=pause, 3=return

        public AbyssalTrident(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_trident", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(18.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(400);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(90); // Damage on throw cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Main shaft: 6 blackstone blocks
            for (int i = 0; i < 6; i++) {
                Location loc = center.clone().add(0, i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 1.0f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                shaftBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Handgrip: widest section
            handgrip = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.POLISHED_BLACKSTONE);
            handgrip.scale(0.55f, 0.6f, 0.55f).glow(60, 60, 70).interpolation(2, 0);
            spawnedEntities.add(handgrip.entity());

            // Crossguard: extends to sides
            crossguard = displayBuilder.spawnBlock(center.clone().add(0, 5.5, 0), Material.BLACKSTONE);
            crossguard.scale(2.5f, 0.3f, 0.3f).glow(50, 50, 60).interpolation(2, 0);
            spawnedEntities.add(crossguard.entity());

            // Dark prismarine rune inlay at crossguard center
            runeInlay = displayBuilder.spawnBlock(center.clone().add(0, 5.5, 0.15), Material.DARK_PRISMARINE);
            runeInlay.scale(0.4f, 0.3f, 0.15f).glow(0, 200, 255).interpolation(2, 0);
            spawnedEntities.add(runeInlay.entity());

            // Three tines at top: center (3 tall), left and right (2 tall)
            // Center tine
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, 6 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                float taper = 0.4f - (i * 0.08f);
                h.scale(taper, 1.0f, taper).glow(40, 0, 60).interpolation(2, 0);
                tineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Left tine
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(-0.8, 6 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 1.0f, 0.3f).glow(40, 0, 60).interpolation(2, 0);
                tineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }
            // Right tine
            for (int i = 0; i < 2; i++) {
                Location loc = center.clone().add(0.8, 6 + i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.3f, 1.0f, 0.3f).glow(40, 0, 60).interpolation(2, 0);
                tineBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Netherite tine tips
            BlockDisplayHandle tipC = displayBuilder.spawnBlock(center.clone().add(0, 9, 0), Material.NETHERITE_BLOCK);
            tipC.scale(0.2f, 0.5f, 0.2f).glow(20, 20, 30).interpolation(2, 0);
            tineTips.add(tipC);
            spawnedEntities.add(tipC.entity());
            BlockDisplayHandle tipL = displayBuilder.spawnBlock(center.clone().add(-0.8, 8, 0), Material.NETHERITE_BLOCK);
            tipL.scale(0.2f, 0.4f, 0.2f).glow(20, 20, 30).interpolation(2, 0);
            tineTips.add(tipL);
            spawnedEntities.add(tipL.entity());
            BlockDisplayHandle tipR = displayBuilder.spawnBlock(center.clone().add(0.8, 8, 0), Material.NETHERITE_BLOCK);
            tipR.scale(0.2f, 0.4f, 0.2f).glow(20, 20, 30).interpolation(2, 0);
            tineTips.add(tipR);
            spawnedEntities.add(tipR.entity());

            // Crying obsidian barb accents at outer tine bases
            BlockDisplayHandle barbL = displayBuilder.spawnBlock(center.clone().add(-0.9, 6.3, 0), Material.CRYING_OBSIDIAN);
            barbL.scale(0.2f, 0.3f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
            barbAccents.add(barbL);
            spawnedEntities.add(barbL.entity());
            BlockDisplayHandle barbR = displayBuilder.spawnBlock(center.clone().add(0.9, 6.3, 0), Material.CRYING_OBSIDIAN);
            barbR.scale(0.2f, 0.3f, 0.2f).glow(128, 0, 255).interpolation(2, 0);
            barbAccents.add(barbR);
            spawnedEntities.add(barbR.entity());

            // Amethyst pommel gems (twin, orbiting shaft base)
            BlockDisplayHandle gemA = displayBuilder.spawnBlock(center.clone().add(0.3, -0.3, 0), Material.AMETHYST_BLOCK);
            gemA.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
            pommelGems.add(gemA);
            spawnedEntities.add(gemA.entity());
            BlockDisplayHandle gemB = displayBuilder.spawnBlock(center.clone().add(-0.3, -0.3, 0), Material.AMETHYST_BLOCK);
            gemB.scale(0.3f, 0.3f, 0.3f).glow(200, 100, 255).interpolation(2, 0);
            pommelGems.add(gemB);
            spawnedEntities.add(gemB.entity());

            DisplayBuilder.playSound(center, Sound.ITEM_TRIDENT_THROW, 0.8f, 0.7f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Slow Y rotation: 0.8 deg/tick
            float baseRot = ticksAlive * 0.01396f;

            // Shaft vibration: +/-0.04 blocks on X and Z at 2 ticks per oscillation
            float vibX = (float) Math.sin(ticksAlive * 1.57f) * 0.04f;
            float vibZ = (float) Math.cos(ticksAlive * 1.57f) * 0.04f;

            // Boomerang throw: every 90 ticks
            // Forward: 10 blocks in 8 ticks. Pause: 10 ticks. Return: 12 ticks.
            int throwCycle = ticksAlive % 90;
            float forwardOffset = 0;

            if (throwCycle < 8) {
                // Forward throw
                forwardOffset = (throwCycle / 8.0f) * 10.0f;
                if (throwCycle == 0) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_THROW, 1.0f, 0.7f);
                }
            } else if (throwCycle < 18) {
                // Pause at maximum extension
                forwardOffset = 10.0f;
            } else if (throwCycle < 30) {
                // Return flight
                forwardOffset = 10.0f * (1.0f - ((throwCycle - 18) / 12.0f));
                if (throwCycle == 18) {
                    DisplayBuilder.playSound(c, Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.8f);
                }
            }

            // Calculate rotated forward direction
            double fwdX = Math.sin(baseRot) * forwardOffset;
            double fwdZ = Math.cos(baseRot) * forwardOffset;

            // Move all shaft blocks
            for (int i = 0; i < shaftBlocks.size(); i++) {
                Location loc = c.clone().add(fwdX + vibX, i + vibZ, fwdZ);
                shaftBlocks.get(i).entity().teleport(loc);
                shaftBlocks.get(i).rotate(baseRot, 0, 1, 0);
                shaftBlocks.get(i).interpolation(2, 0);
            }

            // Move crossguard
            if (crossguard != null) {
                crossguard.entity().teleport(c.clone().add(fwdX, 5.5, fwdZ));
                crossguard.rotate(baseRot, 0, 1, 0);
                crossguard.interpolation(2, 0);
            }
            if (runeInlay != null) {
                runeInlay.entity().teleport(c.clone().add(fwdX, 5.5, fwdZ));
            }

            // Move tines
            double[][] tinePositions = {{0,6},{0,7},{0,8},{-0.8,6},{-0.8,7},{0.8,6},{0.8,7}};
            for (int i = 0; i < Math.min(tineBlocks.size(), tinePositions.length); i++) {
                tineBlocks.get(i).entity().teleport(c.clone().add(
                        fwdX + tinePositions[i][0], tinePositions[i][1], fwdZ));
            }

            // Move tine tips
            double[][] tipPositions = {{0,9},{-0.8,8},{0.8,8}};
            for (int i = 0; i < Math.min(tineTips.size(), tipPositions.length); i++) {
                tineTips.get(i).entity().teleport(c.clone().add(
                        fwdX + tipPositions[i][0], tipPositions[i][1], fwdZ));
            }

            // Pommel gem orbit: 3 deg/tick around shaft base
            float gemOrbit = ticksAlive * 0.0524f;
            for (int i = 0; i < pommelGems.size(); i++) {
                double gAngle = gemOrbit + i * Math.PI;
                Location gLoc = c.clone().add(
                        fwdX + Math.cos(gAngle) * 0.5, -0.3, fwdZ + Math.sin(gAngle) * 0.5);
                pommelGems.get(i).entity().teleport(gLoc);
            }

            // Void-sea bubble trail during forward throw
            if (forwardOffset > 0.5f) {
                DisplayBuilder.dustParticles(c.clone().add(fwdX * 0.5, 4, fwdZ * 0.5), 8, 1.0,
                        0, 0, 80, 1.2f);
            }

            // Trident glow dust along tines
            if (ticksAlive % 3 == 0) {
                for (BlockDisplayHandle tip : tineTips) {
                    DisplayBuilder.dustParticles(tip.entity().getLocation(), 3, 0.3, 0, 150, 200, 1.0f);
                }
            }

            // Obsidian tears from barbs
            if (ticksAlive % 5 == 0) {
                for (BlockDisplayHandle barb : barbAccents) {
                    c.getWorld().spawnParticle(Particle.FALLING_OBSIDIAN_TEAR,
                            barb.entity().getLocation(), 2, 0.1, 0.2, 0.1, 0);
                }
            }

            // Underwater ambient (very quiet)
            if (ticksAlive % 100 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP, 0.2f, 0.3f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssalTrident(plugin); }
    }

    // ================================================================
    // 80. TWIN VOID AXES -- Two counter-rotating axes that clap together
    // ================================================================
    public static class TwinVoidAxes extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> axeOneBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> axeTwoBlocks = new ArrayList<>();
        private BlockDisplayHandle axeOneHead;
        private BlockDisplayHandle axeTwoHead;
        private final List<BlockDisplayHandle> axeOneEdges = new ArrayList<>();
        private final List<BlockDisplayHandle> axeTwoEdges = new ArrayList<>();
        private BlockDisplayHandle bondingGem;

        public TwinVoidAxes(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("twin_void_axes", AttackType.BLOCK_DISPLAY, 1));
            config.setDamage(18.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(440);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(80); // Damage on clap cycles
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // === AXE ONE (tilted 45 degrees left) ===
            // Axe head: 4 obsidian blocks in broad curved blade arc
            for (int i = 0; i < 4; i++) {
                double angle = (i / 3.0) * Math.PI * 0.5; // 90-degree arc
                double x = Math.cos(angle) * 2.0 - 2.0;
                double y = Math.sin(angle) * 2.0 + 2.0;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.3f).glow(40, 0, 60).interpolation(2, 0);
                axeOneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Axe one head composite (large solid piece)
            axeOneHead = displayBuilder.spawnBlock(center.clone().add(-1.5, 2.5, 0), Material.OBSIDIAN);
            axeOneHead.scale(2.5f, 2.0f, 0.4f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(axeOneHead.entity());

            // Netherite horn/toe/poll
            BlockDisplayHandle hornOne = displayBuilder.spawnBlock(center.clone().add(-2.5, 3.5, 0), Material.NETHERITE_BLOCK);
            hornOne.scale(0.4f, 0.6f, 0.3f).glow(20, 20, 30).interpolation(2, 0);
            axeOneBlocks.add(hornOne);
            spawnedEntities.add(hornOne.entity());
            BlockDisplayHandle toeOne = displayBuilder.spawnBlock(center.clone().add(-2.5, 1.5, 0), Material.NETHERITE_BLOCK);
            toeOne.scale(0.4f, 0.6f, 0.3f).glow(20, 20, 30).interpolation(2, 0);
            axeOneBlocks.add(toeOne);
            spawnedEntities.add(toeOne.entity());
            BlockDisplayHandle pollOne = displayBuilder.spawnBlock(center.clone().add(0.5, 2.5, 0), Material.NETHERITE_BLOCK);
            pollOne.scale(0.5f, 1.5f, 0.35f).glow(20, 20, 30).interpolation(2, 0);
            axeOneBlocks.add(pollOne);
            spawnedEntities.add(pollOne.entity());

            // Haft one: 3 blackstone blocks (short handle)
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, -i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 1.0f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                axeOneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian cutting edge (axe one)
            BlockDisplayHandle edgeOne1 = displayBuilder.spawnBlock(center.clone().add(-2.8, 2.8, 0), Material.CRYING_OBSIDIAN);
            edgeOne1.scale(0.2f, 0.8f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            axeOneEdges.add(edgeOne1);
            spawnedEntities.add(edgeOne1.entity());
            BlockDisplayHandle edgeOne2 = displayBuilder.spawnBlock(center.clone().add(-2.8, 2.0, 0), Material.CRYING_OBSIDIAN);
            edgeOne2.scale(0.2f, 0.8f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            axeOneEdges.add(edgeOne2);
            spawnedEntities.add(edgeOne2.entity());

            // === AXE TWO (tilted 45 degrees right, mirrored 180) ===
            // Axe head: mirrored obsidian blade
            for (int i = 0; i < 4; i++) {
                double angle = (i / 3.0) * Math.PI * 0.5;
                double x = -(Math.cos(angle) * 2.0 - 2.0);
                double y = Math.sin(angle) * 2.0 + 2.0;
                Location loc = center.clone().add(x, y, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.OBSIDIAN);
                h.scale(0.7f, 0.7f, 0.3f).glow(40, 0, 60).interpolation(2, 0);
                axeTwoBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            axeTwoHead = displayBuilder.spawnBlock(center.clone().add(1.5, 2.5, 0), Material.OBSIDIAN);
            axeTwoHead.scale(2.5f, 2.0f, 0.4f).glow(40, 0, 60).interpolation(2, 0);
            spawnedEntities.add(axeTwoHead.entity());

            // Netherite parts for axe two
            BlockDisplayHandle hornTwo = displayBuilder.spawnBlock(center.clone().add(2.5, 3.5, 0), Material.NETHERITE_BLOCK);
            hornTwo.scale(0.4f, 0.6f, 0.3f).glow(20, 20, 30).interpolation(2, 0);
            axeTwoBlocks.add(hornTwo);
            spawnedEntities.add(hornTwo.entity());
            BlockDisplayHandle toeTwo = displayBuilder.spawnBlock(center.clone().add(2.5, 1.5, 0), Material.NETHERITE_BLOCK);
            toeTwo.scale(0.4f, 0.6f, 0.3f).glow(20, 20, 30).interpolation(2, 0);
            axeTwoBlocks.add(toeTwo);
            spawnedEntities.add(toeTwo.entity());
            BlockDisplayHandle pollTwo = displayBuilder.spawnBlock(center.clone().add(-0.5, 2.5, 0), Material.NETHERITE_BLOCK);
            pollTwo.scale(0.5f, 1.5f, 0.35f).glow(20, 20, 30).interpolation(2, 0);
            axeTwoBlocks.add(pollTwo);
            spawnedEntities.add(pollTwo.entity());

            // Haft two
            for (int i = 0; i < 3; i++) {
                Location loc = center.clone().add(0, -i, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLACKSTONE);
                h.scale(0.4f, 1.0f, 0.4f).glow(50, 50, 60).interpolation(2, 0);
                axeTwoBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Crying obsidian cutting edge (axe two)
            BlockDisplayHandle edgeTwo1 = displayBuilder.spawnBlock(center.clone().add(2.8, 2.8, 0), Material.CRYING_OBSIDIAN);
            edgeTwo1.scale(0.2f, 0.8f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            axeTwoEdges.add(edgeTwo1);
            spawnedEntities.add(edgeTwo1.entity());
            BlockDisplayHandle edgeTwo2 = displayBuilder.spawnBlock(center.clone().add(2.8, 2.0, 0), Material.CRYING_OBSIDIAN);
            edgeTwo2.scale(0.2f, 0.8f, 0.25f).glow(128, 0, 255).interpolation(2, 0);
            axeTwoEdges.add(edgeTwo2);
            spawnedEntities.add(edgeTwo2.entity());

            // Central bonding amethyst gem floating between axes
            bondingGem = displayBuilder.spawnBlock(center.clone().add(0, 2.5, 0), Material.AMETHYST_BLOCK);
            bondingGem.scale(0.5f, 0.5f, 0.5f).glow(200, 100, 255).interpolation(2, 0);
            spawnedEntities.add(bondingGem.entity());

            DisplayBuilder.playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.8f);
            DisplayBuilder.playSound(center, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.6f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Counter-rotation: Axe One at +1 deg/tick, Axe Two at -1 deg/tick
            float rotOne = ticksAlive * 0.01745f;
            float rotTwo = -ticksAlive * 0.01745f;

            // Every 80 ticks: clap together (both slide 2 blocks toward center in 10 ticks, rebound in 10 ticks)
            int clapCycle = ticksAlive % 80;
            float clapOffset = 0;
            if (clapCycle >= 60 && clapCycle < 70) {
                // Slide inward
                clapOffset = ((clapCycle - 60) / 10.0f) * 2.0f;
                if (clapCycle == 60) {
                    DisplayBuilder.playSound(c, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.8f);
                }
            } else if (clapCycle >= 70 && clapCycle < 80) {
                // Rebound outward
                clapOffset = 2.0f - ((clapCycle - 70) / 10.0f) * 2.0f;
            }

            // Apply rotation and clap to axe one (left side, tilted 45 degrees)
            float axeOneTilt = 0.785f; // 45 degrees in radians
            for (BlockDisplayHandle h : axeOneBlocks) {
                h.rotate(rotOne + axeOneTilt, 0.3f, 0.95f, 0);
                h.interpolation(2, 0);
            }
            if (axeOneHead != null) {
                // Slide axe one rightward during clap
                axeOneHead.entity().teleport(c.clone().add(-1.5 + clapOffset, 2.5, 0));
                axeOneHead.rotate(rotOne + axeOneTilt, 0.3f, 0.95f, 0);
                axeOneHead.interpolation(2, 0);
            }
            for (BlockDisplayHandle edge : axeOneEdges) {
                // Slide edges with head
                BlockDisplay bd = edge.entity();
                Location edgeLoc = bd.getLocation();
                edge.rotate(rotOne + axeOneTilt, 0.3f, 0.95f, 0);
                edge.interpolation(2, 0);
            }

            // Apply rotation and clap to axe two (right side, tilted -45 degrees)
            float axeTwoTilt = -0.785f;
            for (BlockDisplayHandle h : axeTwoBlocks) {
                h.rotate(rotTwo + axeTwoTilt, 0.3f, 0.95f, 0);
                h.interpolation(2, 0);
            }
            if (axeTwoHead != null) {
                // Slide axe two leftward during clap
                axeTwoHead.entity().teleport(c.clone().add(1.5 - clapOffset, 2.5, 0));
                axeTwoHead.rotate(rotTwo + axeTwoTilt, 0.3f, 0.95f, 0);
                axeTwoHead.interpolation(2, 0);
            }
            for (BlockDisplayHandle edge : axeTwoEdges) {
                edge.rotate(rotTwo + axeTwoTilt, 0.3f, 0.95f, 0);
                edge.interpolation(2, 0);
            }

            // Bonding gem pulse: scale 1.0 to 2.0 on each clap
            if (bondingGem != null) {
                float gemScale = 0.5f;
                if (clapCycle >= 60 && clapCycle < 70) {
                    gemScale = 0.5f + ((clapCycle - 60) / 10.0f) * 0.8f;
                } else if (clapCycle >= 70 && clapCycle < 75) {
                    gemScale = 1.3f - ((clapCycle - 70) / 5.0f) * 0.8f;
                }
                bondingGem.scale(gemScale, gemScale, gemScale);
                bondingGem.interpolation(3, 0);
            }

            // Enchant particles from bonding gem on clap
            if (clapCycle == 65) {
                c.getWorld().spawnParticle(Particle.ENCHANT, c.clone().add(0, 2.5, 0), 30,
                        1.0, 0.5, 1.0, 0.5);
            }

            // Crit particles from all 4 cutting edges
            if (ticksAlive % 2 == 0) {
                for (BlockDisplayHandle edge : axeOneEdges) {
                    c.getWorld().spawnParticle(Particle.CRIT, edge.entity().getLocation(), 4,
                            0.2, 0.2, 0.2, 0.1);
                }
                for (BlockDisplayHandle edge : axeTwoEdges) {
                    c.getWorld().spawnParticle(Particle.CRIT, edge.entity().getLocation(), 4,
                            0.2, 0.2, 0.2, 0.1);
                }
            }

            // Twin-void crimson dust tracing blade arcs
            if (ticksAlive % 4 == 0) {
                if (axeOneHead != null) {
                    DisplayBuilder.dustParticles(axeOneHead.entity().getLocation(), 4, 1.0,
                            120, 0, 40, 1.0f);
                }
                if (axeTwoHead != null) {
                    DisplayBuilder.dustParticles(axeTwoHead.entity().getLocation(), 4, 1.0,
                            120, 0, 40, 1.0f);
                }
            }

            // Continuous netherite ambient
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_NETHERITE_BLOCK_HIT, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TwinVoidAxes(plugin); }
    }
}
