package com.blockforge.chaoscraft.modes.bluemoon.attacks.blockdisplay;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Blue Moon Block Display — TIDAL FORMATIONS
 * 13 structures themed around tidal waves, water, and ocean forces.
 * Blue Moon particle palette: pale blue (180, 210, 255), frost cyan (150, 230, 255)
 * Sounds: BLOCK_WATER_AMBIENT, ENTITY_DROWNED_AMBIENT, AMBIENT_UNDERWATER_LOOP,
 *         BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT
 */
public final class TidalFormations {

    private TidalFormations() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new TidalWaveWall(plugin));
        registry.register(new WhirlpoolFunnel(plugin));
        registry.register(new RiptideColumn(plugin));
        registry.register(new UndertowHands(plugin));
        registry.register(new TsunamiArch(plugin));
        registry.register(new TidePoolTrap(plugin));
        registry.register(new SeaFoamSpiral(plugin));
        registry.register(new CoralSpikeBurst(plugin));
        registry.register(new DeepCurrent(plugin));
        registry.register(new GeyserEruption(plugin));
        registry.register(new FrozenBreaker(plugin));
        registry.register(new AbyssalTendril(plugin));
        registry.register(new MaelstromEye(plugin));
    }

    // ================================================================
    // 1. TIDAL WAVE WALL — Curved wave crest of BLUE_ICE + PRISMARINE
    //    Moves forward 0.3 blocks/tick. Knockback on contact.
    // ================================================================
    public static class TidalWaveWall extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private double forwardOffset = 0;
        private double dirX, dirZ;

        public TidalWaveWall(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tidal_wave_wall", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Direction: random horizontal
            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // Build curved wave crest: 18 blocks in an arc
            for (int i = 0; i < 18; i++) {
                // Horizontal spread along perpendicular axis
                double spread = (i - 8.5) * 0.6;
                double perpX = -dirZ * spread;
                double perpZ = dirX * spread;
                // Curved crest height: parabolic
                double heightCurve = 3.0 - 0.04 * (i - 8.5) * (i - 8.5);
                // Slight forward curl at top
                double curl = heightCurve * 0.3;

                Location loc = center.clone().add(perpX + dirX * curl, heightCurve, perpZ + dirZ * curl);
                Material mat = (i % 3 == 0) ? Material.PRISMARINE : Material.BLUE_ICE;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                float scaleY = 0.8f + (float)(heightCurve / 3.0) * 0.6f;
                h.scale(0.7f, scaleY, 0.7f).glow(150, 230, 255).interpolation(3, 0);
                // Tilt forward to look like cresting wave
                h.rotate((float)(heightCurve * 0.15), (float)-dirZ, 0, (float)dirX);
                waveBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.5f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.8f, 0.8f);
            DisplayBuilder.dustParticles(center, 40, 2.0, 180, 210, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Move wave forward
            forwardOffset += 0.3;

            for (int i = 0; i < waveBlocks.size(); i++) {
                double spread = (i - 8.5) * 0.6;
                double perpX = -dirZ * spread;
                double perpZ = dirX * spread;
                double heightCurve = 3.0 - 0.04 * (i - 8.5) * (i - 8.5);
                double curl = heightCurve * 0.3;

                Location target = c.clone().add(
                        perpX + dirX * (curl + forwardOffset),
                        heightCurve,
                        perpZ + dirZ * (curl + forwardOffset)
                );
                waveBlocks.get(i).entity().teleport(target);
                waveBlocks.get(i).interpolation(2, 0);
            }

            // Update damage center to wave front
            setCenter(c.clone().add(dirX * forwardOffset, 0, dirZ * forwardOffset));

            // Knockback nearby players
            if (ticksAlive % 5 == 0) {
                Location waveCenter = getCenter();
                for (Player player : c.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(waveCenter) <= 25) { // 5 radius
                        Vector kb = new Vector(dirX * 0.8, 0.3, dirZ * 0.8);
                        player.setVelocity(player.getVelocity().add(kb));
                    }
                }
            }

            // Spray particles along crest
            if (ticksAlive % 3 == 0) {
                Location spray = getCenter().clone().add(0, 3, 0);
                DisplayBuilder.dustParticles(spray, 8, 3.0, 180, 210, 255, 1.2f);
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, spray, 6, 2.0, 0.5, 2.0, 0);
            }

            // Ambient water sound
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(getCenter(), Sound.BLOCK_WATER_AMBIENT, 0.6f, 0.7f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TidalWaveWall(plugin); }
    }

    // ================================================================
    // 2. WHIRLPOOL FUNNEL — PRISMARINE spiraling downward cone
    //    Rotates 4 deg/tick. Pulls nearby players toward center.
    // ================================================================
    public static class WhirlpoolFunnel extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> funnelBlocks = new ArrayList<>();
        private float rotationAngle = 0;

        public WhirlpoolFunnel(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("whirlpool_funnel", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Spiral cone: 16 PRISMARINE blocks descending in tightening spiral
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double radius = 3.5 * (1.0 - t * 0.7); // 3.5 → 1.05 radius
                double angle = t * Math.PI * 4; // 2 full rotations down
                double y = -t * 4.0; // descend 4 blocks
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location loc = center.clone().add(x, y + 2, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                float scale = 0.9f - (float)t * 0.4f;
                h.scale(scale, 0.6f, scale).glow(150, 230, 255).interpolation(3, 0);
                funnelBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_DROWNED_AMBIENT, 1.0f, 0.4f);
            DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.2f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate entire funnel 4 degrees/tick
            rotationAngle += (float) Math.toRadians(4);

            for (int i = 0; i < funnelBlocks.size(); i++) {
                double t = i / 15.0;
                double radius = 3.5 * (1.0 - t * 0.7);
                double baseAngle = t * Math.PI * 4;
                double angle = baseAngle + rotationAngle;
                double y = -t * 4.0;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Location target = c.clone().add(x, y + 2, z);
                funnelBlocks.get(i).entity().teleport(target);
                funnelBlocks.get(i).rotate(rotationAngle * 0.5f, 0, 1, 0);
                funnelBlocks.get(i).interpolation(2, 0);
            }

            // Pull nearby players toward center
            if (ticksAlive % 4 == 0) {
                for (Player player : c.getWorld().getPlayers()) {
                    double dist = player.getLocation().distance(c);
                    if (dist <= 6.0 && dist > 0.5) {
                        Vector pull = c.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.15);
                        pull.setY(-0.05); // Slight downward pull
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
            }

            // Water vortex particles
            if (ticksAlive % 3 == 0) {
                double pAngle = rotationAngle * 2;
                Location pLoc = c.clone().add(Math.cos(pAngle) * 2, 0, Math.sin(pAngle) * 2);
                DisplayBuilder.dustParticles(pLoc, 5, 0.5, 150, 230, 255, 1.0f);
                c.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(0, -1, 0), 4, 1.0, 0.5, 1.0, 0);
            }

            // Ambient sound
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new WhirlpoolFunnel(plugin); }
    }

    // ================================================================
    // 3. RIPTIDE COLUMN — 12 BLUE_ICE stacked vertical, spinning rapidly
    //    Drip particles. Moves toward player 0.15 blocks/tick.
    // ================================================================
    public static class RiptideColumn extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> columnBlocks = new ArrayList<>();
        private float spinAngle = 0;

        public RiptideColumn(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("riptide_column", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(250);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
            config.setTracksPlayer(true);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Vertical column: 12 BLUE_ICE blocks stacked with slight offsets
            for (int i = 0; i < 12; i++) {
                double wobbleX = Math.sin(i * 0.8) * 0.3;
                double wobbleZ = Math.cos(i * 0.8) * 0.3;
                Location loc = center.clone().add(wobbleX, i * 0.8, wobbleZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float taper = 1.0f - Math.abs(i - 5.5f) * 0.08f; // Wider in middle
                h.scale(taper, 0.8f, taper).glow(180, 210, 255).interpolation(3, 0);
                columnBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.2f);
            DisplayBuilder.playSound(center, Sound.ENTITY_DROWNED_AMBIENT, 0.8f, 0.8f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spin rapidly (15 deg/tick)
            spinAngle += (float) Math.toRadians(15);

            // Move toward target player
            Player target = getTargetPlayer();
            if (target != null && target.isOnline()) {
                Location playerLoc = target.getLocation();
                Vector dir = playerLoc.toVector().subtract(c.toVector());
                if (dir.lengthSquared() > 0.1) {
                    dir.normalize().multiply(0.15);
                    dir.setY(0);
                    setCenter(c.add(dir));
                }
            }

            Location base = getCenter();
            for (int i = 0; i < columnBlocks.size(); i++) {
                double phase = spinAngle + i * 0.5;
                double wobbleX = Math.sin(phase) * 0.4;
                double wobbleZ = Math.cos(phase) * 0.4;
                Location target2 = base.clone().add(wobbleX, i * 0.8, wobbleZ);
                columnBlocks.get(i).entity().teleport(target2);
                columnBlocks.get(i).rotate(spinAngle, 0, 1, 0);
                columnBlocks.get(i).interpolation(2, 0);
            }

            // Drip particles falling from column
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 3; i++) {
                    int idx = (int)(Math.random() * columnBlocks.size());
                    Location drip = columnBlocks.get(idx).entity().getLocation();
                    base.getWorld().spawnParticle(Particle.DRIPPING_WATER, drip, 2, 0.3, 0.2, 0.3, 0);
                }
                DisplayBuilder.dustParticles(base.clone().add(0, 5, 0), 4, 1.0, 150, 230, 255, 1.0f);
            }

            // Splash sound
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(base, Sound.BLOCK_WATER_AMBIENT, 0.5f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new RiptideColumn(plugin); }
    }

    // ================================================================
    // 4. UNDERTOW HANDS — Two DARK_PRISMARINE grabbing hands from ground
    //    Close over 40 ticks. Impact when clap.
    // ================================================================
    public static class UndertowHands extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> leftHand = new ArrayList<>();
        private final List<BlockDisplayHandle> rightHand = new ArrayList<>();
        private boolean impactDone = false;

        public UndertowHands(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("undertow_hands", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(14.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(120);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Left hand: 7 blocks forming curved fingers reaching from left
            for (int i = 0; i < 7; i++) {
                double curve = Math.sin(i / 6.0 * Math.PI) * 1.5;
                Location loc = center.clone().add(-3.5 + i * 0.2, curve, -0.3 + i * 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                float s = 0.7f + (float)(curve / 1.5) * 0.3f;
                h.scale(s, 0.6f, s).glow(150, 230, 255).interpolation(3, 0);
                leftHand.add(h);
                spawnedEntities.add(h.entity());
            }

            // Right hand: 7 blocks from right side
            for (int i = 0; i < 7; i++) {
                double curve = Math.sin(i / 6.0 * Math.PI) * 1.5;
                Location loc = center.clone().add(3.5 - i * 0.2, curve, 0.3 - i * 0.1);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                float s = 0.7f + (float)(curve / 1.5) * 0.3f;
                h.scale(s, 0.6f, s).glow(150, 230, 255).interpolation(3, 0);
                rightHand.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_DROWNED_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.dustParticles(center, 25, 2.0, 180, 210, 255, 1.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            if (ticksAlive <= 40) {
                // Hands close inward over 40 ticks
                double closeProgress = ticksAlive / 40.0;
                double offset = 3.5 * (1.0 - closeProgress);

                for (int i = 0; i < leftHand.size(); i++) {
                    double curve = Math.sin(i / 6.0 * Math.PI) * 1.5;
                    double rise = closeProgress * 2.0;
                    Location target = c.clone().add(-offset + i * 0.2 * (1.0 - closeProgress * 0.5),
                            curve + rise, -0.3 + i * 0.1);
                    leftHand.get(i).entity().teleport(target);
                    leftHand.get(i).interpolation(2, 0);
                }
                for (int i = 0; i < rightHand.size(); i++) {
                    double curve = Math.sin(i / 6.0 * Math.PI) * 1.5;
                    double rise = closeProgress * 2.0;
                    Location target = c.clone().add(offset - i * 0.2 * (1.0 - closeProgress * 0.5),
                            curve + rise, 0.3 - i * 0.1);
                    rightHand.get(i).entity().teleport(target);
                    rightHand.get(i).interpolation(2, 0);
                }

                // Rising particles
                if (ticksAlive % 4 == 0) {
                    DisplayBuilder.dustParticles(c.clone().add(0, 1, 0), 6, 1.5, 150, 230, 255, 1.0f);
                }
            }

            // Impact at tick 40 — the clap
            if (ticksAlive == 40 && !impactDone) {
                impactDone = true;
                triggerImpactDamage(c.clone().add(0, 1.5, 0));
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 1.5f, 0.3f);
                DisplayBuilder.dustParticles(c.clone().add(0, 1.5, 0), 50, 2.0, 180, 210, 255, 2.0f);
                c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, 2, 0), 40, 1.5, 1.0, 1.5, 0);
            }

            // Post-clap: tremble
            if (ticksAlive > 40) {
                float tremble = (float) Math.sin(ticksAlive * 2.0) * 0.1f;
                for (BlockDisplayHandle h : leftHand) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, tremble, 0));
                }
                for (BlockDisplayHandle h : rightHand) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, -tremble, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new UndertowHands(plugin); }
    }

    // ================================================================
    // 5. TSUNAMI ARCH — PACKED_ICE arch/wave that crashes forward
    //    Impact on collapse.
    // ================================================================
    public static class TsunamiArch extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> archBlocks = new ArrayList<>();
        private boolean impactDone = false;
        private double dirX, dirZ;

        public TsunamiArch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tsunami_arch", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(12.0);
            config.setImpactRadius(5.0);
            config.setDurationTicks(140);
            config.setCooldownTicks(380);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // Arch: 16 PACKED_ICE blocks forming a rising arch
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                double archHeight = Math.sin(t * Math.PI) * 6.0; // Arch up to 6 blocks
                double forward = t * 5.0; // Span 5 blocks forward
                Location loc = center.clone().add(dirX * forward, archHeight, dirZ * forward);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                float scaleX = 1.2f;
                float scaleY = 0.8f + (float)(archHeight / 6.0) * 0.6f;
                h.scale(scaleX, scaleY, 1.0f).glow(180, 210, 255).interpolation(3, 0);
                // Tilt to follow arch curve
                float tiltAngle = (float)(Math.cos(t * Math.PI) * 0.5);
                h.rotate(tiltAngle, (float)-dirZ, 0, (float)dirX);
                archBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.5f, 0.4f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Arch rises and holds (ticks 1-60)
            if (ticksAlive <= 60) {
                float riseProgress = Math.min(1.0f, ticksAlive / 30.0f);
                for (int i = 0; i < archBlocks.size(); i++) {
                    double t = i / 15.0;
                    double archHeight = Math.sin(t * Math.PI) * 6.0 * riseProgress;
                    double forward = t * 5.0;
                    Location target = c.clone().add(dirX * forward, archHeight, dirZ * forward);
                    archBlocks.get(i).entity().teleport(target);
                    archBlocks.get(i).interpolation(2, 0);
                }

                // Spray at apex
                if (ticksAlive % 4 == 0 && ticksAlive > 20) {
                    Location apex = c.clone().add(dirX * 2.5, 6, dirZ * 2.5);
                    DisplayBuilder.dustParticles(apex, 8, 1.0, 150, 230, 255, 1.2f);
                    c.getWorld().spawnParticle(Particle.DRIPPING_WATER, apex, 5, 1.0, 0.5, 1.0, 0);
                }
            }

            // Phase 2: Collapse forward (ticks 61-80)
            if (ticksAlive > 60 && ticksAlive <= 80) {
                double collapseProgress = (ticksAlive - 60) / 20.0;
                for (int i = 0; i < archBlocks.size(); i++) {
                    double t = i / 15.0;
                    double originalHeight = Math.sin(t * Math.PI) * 6.0;
                    double crashHeight = originalHeight * (1.0 - collapseProgress);
                    double forward = t * 5.0 + collapseProgress * 3.0;
                    Location target = c.clone().add(dirX * forward, crashHeight, dirZ * forward);
                    archBlocks.get(i).entity().teleport(target);
                    // Tilt forward as it crashes
                    float crashTilt = (float)(collapseProgress * -1.2);
                    archBlocks.get(i).rotate(crashTilt, (float)-dirZ, 0, (float)dirX);
                    archBlocks.get(i).interpolation(2, 0);
                }
            }

            // Impact at tick 80
            if (ticksAlive == 80 && !impactDone) {
                impactDone = true;
                Location impactLoc = c.clone().add(dirX * 6, 0, dirZ * 6);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.3f);
                DisplayBuilder.dustParticles(impactLoc, 60, 3.0, 180, 210, 255, 2.0f);
                impactLoc.getWorld().spawnParticle(Particle.SPLASH, impactLoc.clone().add(0, 1, 0), 80, 3.0, 1.0, 3.0, 0);
            }

            // Post-impact: scatter blocks with tremble
            if (ticksAlive > 80) {
                float fade = (float) Math.sin(ticksAlive * 1.5) * 0.08f;
                for (BlockDisplayHandle h : archBlocks) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(0, fade, 0));
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TsunamiArch(plugin); }
    }

    // ================================================================
    // 6. TIDE POOL TRAP — 10 PRISMARINE shallow bowl
    //    Drip particle fill effect. Increasing damage while inside.
    // ================================================================
    public static class TidePoolTrap extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bowlBlocks = new ArrayList<>();
        private int ticksPlayerInside = 0;

        public TidePoolTrap(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tide_pool_trap", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(4.0);
            config.setDamageRadius(3.0);
            config.setDurationTicks(300);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(30);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Shallow bowl: 10 PRISMARINE blocks in a ring, slightly tilted inward
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 2.5;
                double z = Math.sin(angle) * 2.5;
                Location loc = center.clone().add(x, 0, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(1.0f, 0.4f, 1.0f).glow(150, 230, 255).interpolation(3, 0);
                // Tilt inward toward center
                float tiltAngle = 0.3f;
                h.rotate(tiltAngle, (float)Math.sin(angle), 0, (float)-Math.cos(angle));
                bowlBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Center floor block
            BlockDisplayHandle floor = displayBuilder.spawnBlock(center.clone().add(0, -0.3, 0), Material.PRISMARINE);
            floor.scale(2.5f, 0.2f, 2.5f).glow(180, 210, 255).interpolation(3, 0);
            bowlBlocks.add(floor);
            spawnedEntities.add(floor.entity());

            // Inner detail block
            BlockDisplayHandle inner = displayBuilder.spawnBlock(center.clone().add(0, -0.1, 0), Material.DARK_PRISMARINE);
            inner.scale(1.5f, 0.15f, 1.5f).glow(100, 180, 220).interpolation(3, 0);
            bowlBlocks.add(inner);
            spawnedEntities.add(inner.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.8f, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Drip fill effect: water particles increasing density over time
            double fillLevel = Math.min(1.0, ticksAlive / 200.0);
            if (ticksAlive % 3 == 0) {
                int particleCount = 2 + (int)(fillLevel * 8);
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, c.clone().add(0, 0.5 + fillLevel, 0),
                        particleCount, 1.5, 0.3, 1.5, 0);
                DisplayBuilder.dustParticles(c.clone().add(0, fillLevel * 0.5, 0),
                        4, 2.0, 180, 210, 255, 0.8f + (float)fillLevel);
            }

            // Scale up damage based on fill level (simulated by increasing damage over time)
            // The base damage is 4, but we modulate the bowl glow intensity
            if (ticksAlive % 20 == 0) {
                int glowIntensity = 150 + (int)(fillLevel * 105);
                for (int i = 0; i < Math.min(bowlBlocks.size(), 10); i++) {
                    bowlBlocks.get(i).glow(glowIntensity, 230, 255);
                    bowlBlocks.get(i).interpolation(10, 0);
                }
            }

            // Bubble column at center
            if (ticksAlive % 5 == 0 && fillLevel > 0.3) {
                c.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(0, 0.2, 0),
                        3, 0.5, 0.1, 0.5, 0);
            }

            // Bowl edge ripple animation
            if (ticksAlive % 10 == 0) {
                float ripple = (float) Math.sin(ticksAlive * 0.2) * 0.05f;
                for (int i = 0; i < Math.min(bowlBlocks.size(), 10); i++) {
                    BlockDisplay bd = bowlBlocks.get(i).entity();
                    bd.teleport(bd.getLocation().add(0, ripple, 0));
                }
            }

            // Ambient sound
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new TidePoolTrap(plugin); }
    }

    // ================================================================
    // 7. SEA FOAM SPIRAL — WHITE_CONCRETE + LIGHT_BLUE_STAINED_GLASS
    //    Flat spiral spinning outward, expanding 2→5 radius.
    // ================================================================
    public static class SeaFoamSpiral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();
        private float spinAngle = 0;

        public SeaFoamSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("sea_foam_spiral", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(5.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Flat spiral: 15 blocks in Archimedean spiral pattern
            for (int i = 0; i < 15; i++) {
                double t = i / 14.0;
                double radius = 2.0; // Start at radius 2
                double angle = t * Math.PI * 3; // 1.5 full rotations
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0.2, z);
                Material mat = (i % 2 == 0) ? Material.WHITE_CONCRETE : Material.LIGHT_BLUE_STAINED_GLASS;
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, mat);
                h.scale(0.6f, 0.2f, 0.6f).glow(180, 210, 255).interpolation(3, 0);
                spiralBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.8f, 1.4f);
            DisplayBuilder.dustParticles(center, 20, 2.0, 180, 210, 255, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spin 6 deg/tick
            spinAngle += (float) Math.toRadians(6);

            // Expand from radius 2 to 5 over duration
            double expandProgress = Math.min(1.0, ticksAlive / 200.0);
            double currentRadius = 2.0 + expandProgress * 3.0;

            // Update damage radius to match expansion
            // (damage is handled by base class using config radius of 5)

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double t = i / 14.0;
                double blockRadius = currentRadius * (0.3 + t * 0.7); // Inner to outer
                double baseAngle = t * Math.PI * 3;
                double angle = baseAngle + spinAngle;
                double x = Math.cos(angle) * blockRadius;
                double z = Math.sin(angle) * blockRadius;
                Location target = c.clone().add(x, 0.2, z);
                spiralBlocks.get(i).entity().teleport(target);
                spiralBlocks.get(i).rotate(spinAngle, 0, 1, 0);
                spiralBlocks.get(i).interpolation(2, 0);
            }

            // Foam trail particles
            if (ticksAlive % 2 == 0) {
                double pAngle = spinAngle * 1.5;
                Location pLoc = c.clone().add(Math.cos(pAngle) * currentRadius, 0.3, Math.sin(pAngle) * currentRadius);
                DisplayBuilder.dustParticles(pLoc, 4, 0.5, 230, 240, 255, 0.8f);
                c.getWorld().spawnParticle(Particle.SPLASH, pLoc, 3, 0.3, 0.1, 0.3, 0);
            }

            // Expanding ring particles at edge
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), currentRadius, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.8f));
            }

            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 0.4f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new SeaFoamSpiral(plugin); }
    }

    // ================================================================
    // 8. CORAL SPIKE BURST — PRISMARINE + CALCITE erupting sequentially
    //    One spike per 3 ticks. Impact per spike.
    // ================================================================
    public static class CoralSpikeBurst extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spikeBlocks = new ArrayList<>();
        private final double[][] spikePositions = new double[12][];
        private int spikesSpawned = 0;

        public CoralSpikeBurst(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("coral_spike_burst", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(6.0);
            config.setImpactRadius(2.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(300);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-calculate spike positions in outward spiral
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12 + Math.random() * 0.3;
                double radius = 1.0 + i * 0.35;
                spikePositions[i] = new double[]{
                        Math.cos(angle) * radius,
                        Math.sin(angle) * radius
                };
            }

            // Ground warning: ring of particles
            DisplayBuilder.particleRing(center, 4.0, Particle.DUST, 20,
                    new Particle.DustOptions(Color.fromRGB(150, 230, 255), 1.0f));
            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 0.8f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Spawn one spike every 3 ticks
            if (ticksAlive % 3 == 0 && spikesSpawned < 12) {
                int idx = spikesSpawned;
                double x = spikePositions[idx][0];
                double z = spikePositions[idx][1];
                Location spikeLoc = c.clone().add(x, 0, z);

                // Each spike: 2 blocks tall (base + tip)
                Material baseMat = (idx % 2 == 0) ? Material.PRISMARINE : Material.CALCITE;
                Material tipMat = (idx % 2 == 0) ? Material.CALCITE : Material.PRISMARINE;

                BlockDisplayHandle base = displayBuilder.spawnBlock(spikeLoc.clone().add(0, -1, 0), baseMat);
                base.scale(0.5f, 1.2f, 0.5f).glow(150, 230, 255).interpolation(3, 0);
                spikeBlocks.add(base);
                spawnedEntities.add(base.entity());

                BlockDisplayHandle tip = displayBuilder.spawnBlock(spikeLoc.clone().add(0, 0.2, 0), tipMat);
                tip.scale(0.3f, 0.8f, 0.3f).glow(180, 210, 255).interpolation(3, 0);
                spikeBlocks.add(tip);
                spawnedEntities.add(tip.entity());

                // Animate eruption upward
                // (blocks start underground, move to surface)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (base.entity().isValid()) {
                        base.entity().teleport(spikeLoc);
                        base.interpolation(3, 0);
                    }
                    if (tip.entity().isValid()) {
                        tip.entity().teleport(spikeLoc.clone().add(0, 1.2, 0));
                        tip.interpolation(3, 0);
                    }
                }, 1L);

                // Impact damage per spike
                triggerImpactDamage(spikeLoc);

                // Eruption effects
                DisplayBuilder.playSound(spikeLoc, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 0.7f, 0.8f);
                DisplayBuilder.dustParticles(spikeLoc, 8, 0.5, 150, 230, 255, 1.0f);
                c.getWorld().spawnParticle(Particle.SPLASH, spikeLoc.clone().add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0);

                spikesSpawned++;
            }

            // Existing spikes shimmer
            if (ticksAlive % 8 == 0) {
                for (int i = 0; i < spikeBlocks.size(); i += 2) {
                    if (i < spikeBlocks.size()) {
                        float shimmer = (float) Math.sin(ticksAlive * 0.3 + i) * 0.05f;
                        BlockDisplay bd = spikeBlocks.get(i).entity();
                        if (bd.isValid()) {
                            bd.teleport(bd.getLocation().add(0, shimmer, 0));
                        }
                    }
                }
            }

            // Ambient drip
            if (ticksAlive % 10 == 0 && !spikeBlocks.isEmpty()) {
                int idx = (int)(Math.random() * spikeBlocks.size());
                Location drip = spikeBlocks.get(idx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, drip.clone().add(0, 1, 0), 3, 0.2, 0.1, 0.2, 0);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new CoralSpikeBurst(plugin); }
    }

    // ================================================================
    // 9. DEEP CURRENT — DARK_PRISMARINE horizontal line sweeping ground
    //    Knockback + damage.
    // ================================================================
    public static class DeepCurrent extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> currentBlocks = new ArrayList<>();
        private double sweepAngle;
        private double sweepSpeed;

        public DeepCurrent(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("deep_current", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(7.0);
            config.setDamageRadius(5.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            sweepAngle = Math.random() * Math.PI * 2;
            sweepSpeed = 0.04; // Radians per tick (~2.3 deg/tick)

            // Horizontal line: 14 DARK_PRISMARINE blocks in a bar
            for (int i = 0; i < 14; i++) {
                double offset = (i - 6.5) * 0.8; // Spread along the line
                double x = Math.cos(sweepAngle) * offset;
                double z = Math.sin(sweepAngle) * offset;
                Location loc = center.clone().add(x, 0.3, z);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                float scaleX = 0.8f;
                float scaleZ = 0.8f;
                h.scale(scaleX, 0.3f, scaleZ).glow(100, 180, 220).interpolation(3, 0);
                currentBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 1.0f, 0.7f);
            DisplayBuilder.dustParticles(center, 30, 3.0, 150, 230, 255, 1.0f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Sweep the line across the ground (rotate around center)
            sweepAngle += sweepSpeed;

            for (int i = 0; i < currentBlocks.size(); i++) {
                double offset = (i - 6.5) * 0.8;
                double x = Math.cos(sweepAngle) * offset;
                double z = Math.sin(sweepAngle) * offset;
                Location target = c.clone().add(x, 0.3, z);
                currentBlocks.get(i).entity().teleport(target);
                currentBlocks.get(i).rotate((float) sweepAngle, 0, 1, 0);
                currentBlocks.get(i).interpolation(2, 0);
            }

            // Knockback players in sweep path
            if (ticksAlive % 4 == 0) {
                double sweepDirX = -Math.sin(sweepAngle);
                double sweepDirZ = Math.cos(sweepAngle);
                for (Player player : c.getWorld().getPlayers()) {
                    if (player.getLocation().distanceSquared(c) <= 36) { // 6 block check
                        // Check if player is near the sweep line
                        double px = player.getLocation().getX() - c.getX();
                        double pz = player.getLocation().getZ() - c.getZ();
                        double lineX = Math.cos(sweepAngle);
                        double lineZ = Math.sin(sweepAngle);
                        double distToLine = Math.abs(px * (-lineZ) + pz * lineX);
                        if (distToLine < 1.5) {
                            Vector kb = new Vector(sweepDirX * 0.6, 0.2, sweepDirZ * 0.6);
                            player.setVelocity(player.getVelocity().add(kb));
                        }
                    }
                }
            }

            // Current trail particles
            if (ticksAlive % 3 == 0) {
                double edgeX = Math.cos(sweepAngle) * 5;
                double edgeZ = Math.sin(sweepAngle) * 5;
                Location edge = c.clone().add(edgeX, 0.5, edgeZ);
                DisplayBuilder.dustParticles(edge, 6, 0.8, 150, 230, 255, 1.0f);
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, edge, 3, 0.5, 0.2, 0.5, 0);
            }

            // Ground foam
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.2, 0), 3.0, Particle.DUST, 10,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.6f));
            }

            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP, 0.5f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new DeepCurrent(plugin); }
    }

    // ================================================================
    // 10. GEYSER ERUPTION — PACKED_ICE launched upward then scatter
    //     Impact where they land.
    // ================================================================
    public static class GeyserEruption extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> geyserBlocks = new ArrayList<>();
        private final double[][] landingOffsets = new double[10][];
        private final boolean[] landed = new boolean[10];
        private boolean erupted = false;

        public GeyserEruption(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("geyser_eruption", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(10.0);
            config.setImpactRadius(3.0);
            config.setDurationTicks(160);
            config.setCooldownTicks(350);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Pre-calculate random landing positions
            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * Math.PI * 2;
                double dist = 2.0 + Math.random() * 4.0;
                landingOffsets[i] = new double[]{Math.cos(angle) * dist, Math.sin(angle) * dist};
                landed[i] = false;
            }

            // Spawn 10 PACKED_ICE blocks at center, stacked underground
            for (int i = 0; i < 10; i++) {
                Location loc = center.clone().add(0, -2 + i * 0.3, 0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PACKED_ICE);
                float s = 0.6f + (float)(Math.random() * 0.3);
                h.scale(s, s, s).glow(180, 210, 255).interpolation(3, 0);
                geyserBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Geyser vent base
            BlockDisplayHandle vent = displayBuilder.spawnBlock(center.clone().add(0, -0.3, 0), Material.PRISMARINE);
            vent.scale(1.5f, 0.3f, 1.5f).glow(150, 230, 255).interpolation(3, 0);
            spawnedEntities.add(vent.entity());

            BlockDisplayHandle ventInner = displayBuilder.spawnBlock(center, Material.DARK_PRISMARINE);
            ventInner.scale(0.8f, 0.2f, 0.8f).glow(100, 180, 220).interpolation(3, 0);
            spawnedEntities.add(ventInner.entity());

            DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Build pressure (ticks 1-30) — shake
            if (ticksAlive <= 30) {
                float tremble = (float) Math.sin(ticksAlive * 3) * 0.1f * (ticksAlive / 30.0f);
                for (BlockDisplayHandle h : geyserBlocks) {
                    BlockDisplay bd = h.entity();
                    bd.teleport(bd.getLocation().add(tremble, 0, -tremble));
                }
                // Warning particles
                if (ticksAlive % 3 == 0) {
                    c.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, c, 5, 0.3, 0.2, 0.3, 0.05);
                    DisplayBuilder.dustParticles(c.clone().add(0, 0.5, 0), 4, 0.5, 150, 230, 255, 1.0f);
                }
            }

            // Phase 2: Eruption (tick 30) — launch upward
            if (ticksAlive == 30 && !erupted) {
                erupted = true;
                DisplayBuilder.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.7f);
                DisplayBuilder.playSound(c, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.3f);
                c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, 2, 0), 60, 1.0, 2.0, 1.0, 0.1);
                DisplayBuilder.dustParticles(c.clone().add(0, 3, 0), 40, 2.0, 180, 210, 255, 2.0f);
            }

            // Phase 3: Blocks fly up then scatter (ticks 31-70)
            if (ticksAlive > 30 && ticksAlive <= 70) {
                double flyProgress = (ticksAlive - 30) / 40.0;
                for (int i = 0; i < geyserBlocks.size(); i++) {
                    double peakHeight = 8.0 + Math.random() * 0.2; // slightly varied
                    double height;
                    double xOff, zOff;
                    if (flyProgress < 0.5) {
                        // Rising
                        height = flyProgress * 2 * peakHeight;
                        xOff = 0;
                        zOff = 0;
                    } else {
                        // Scattering outward and falling
                        double fallProgress = (flyProgress - 0.5) * 2;
                        height = peakHeight * (1.0 - fallProgress * fallProgress);
                        xOff = landingOffsets[i][0] * fallProgress;
                        zOff = landingOffsets[i][1] * fallProgress;
                    }
                    Location target = c.clone().add(xOff, height, zOff);
                    geyserBlocks.get(i).entity().teleport(target);
                    geyserBlocks.get(i).rotate((float)(flyProgress * 3), 1, 0.5f, 0.3f);
                    geyserBlocks.get(i).interpolation(2, 0);
                }

                // Water column particles
                if (ticksAlive % 2 == 0) {
                    c.getWorld().spawnParticle(Particle.SPLASH, c.clone().add(0, flyProgress * 8, 0),
                            8, 0.5, 1.0, 0.5, 0);
                }
            }

            // Phase 4: Landing impacts (tick 70)
            if (ticksAlive == 70) {
                for (int i = 0; i < 10; i++) {
                    if (!landed[i]) {
                        landed[i] = true;
                        Location landLoc = c.clone().add(landingOffsets[i][0], 0, landingOffsets[i][1]);
                        triggerImpactDamage(landLoc);
                        DisplayBuilder.playSound(landLoc, Sound.BLOCK_POINTED_DRIPSTONE_LAND, 0.6f, 0.6f);
                        DisplayBuilder.dustParticles(landLoc, 10, 0.8, 180, 210, 255, 1.2f);
                        landLoc.getWorld().spawnParticle(Particle.SPLASH, landLoc.clone().add(0, 0.5, 0),
                                15, 0.5, 0.3, 0.5, 0);
                    }
                }
            }

            // Post-landing: blocks rest on ground
            if (ticksAlive > 70) {
                for (int i = 0; i < geyserBlocks.size(); i++) {
                    Location restLoc = c.clone().add(landingOffsets[i][0], 0, landingOffsets[i][1]);
                    geyserBlocks.get(i).entity().teleport(restLoc);
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new GeyserEruption(plugin); }
    }

    // ================================================================
    // 11. FROZEN BREAKER — BLUE_ICE cresting wave, shatters at peak
    //     Impact from fragments.
    // ================================================================
    public static class FrozenBreaker extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> waveBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> fragmentBlocks = new ArrayList<>();
        private boolean shattered = false;
        private double dirX, dirZ;

        public FrozenBreaker(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("frozen_breaker", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamageOnImpactOnly(true);
            config.setImpactDamage(8.0);
            config.setImpactRadius(4.0);
            config.setDurationTicks(150);
            config.setCooldownTicks(320);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double angle = Math.random() * Math.PI * 2;
            dirX = Math.cos(angle);
            dirZ = Math.sin(angle);

            // Cresting wave: 16 BLUE_ICE blocks in wave shape
            for (int i = 0; i < 16; i++) {
                double t = i / 15.0;
                // Wave profile: rises to crest then curls over
                double height = Math.sin(t * Math.PI) * 5.0;
                double curl = (t > 0.6) ? (t - 0.6) * 4.0 : 0; // Curl at top
                double perpSpread = (i % 2 == 0 ? 0.3 : -0.3);

                Location loc = center.clone().add(
                        dirX * (t * 4.0 + curl) + (-dirZ) * perpSpread,
                        height,
                        dirZ * (t * 4.0 + curl) + dirX * perpSpread
                );
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.BLUE_ICE);
                float scaleY = 0.7f + (float)(height / 5.0) * 0.5f;
                h.scale(0.8f, scaleY, 0.8f).glow(150, 230, 255).interpolation(3, 0);
                waveBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 0.5f);
            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.2f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Phase 1: Wave rises and crests (ticks 1-50)
            if (ticksAlive <= 50) {
                float riseProgress = Math.min(1.0f, ticksAlive / 35.0f);
                for (int i = 0; i < waveBlocks.size(); i++) {
                    double t = i / 15.0;
                    double height = Math.sin(t * Math.PI) * 5.0 * riseProgress;
                    double curl = (t > 0.6) ? (t - 0.6) * 4.0 * riseProgress : 0;
                    double perpSpread = (i % 2 == 0 ? 0.3 : -0.3);
                    Location target = c.clone().add(
                            dirX * (t * 4.0 + curl) + (-dirZ) * perpSpread,
                            height,
                            dirZ * (t * 4.0 + curl) + dirX * perpSpread
                    );
                    waveBlocks.get(i).entity().teleport(target);
                    waveBlocks.get(i).interpolation(2, 0);
                }

                // Frost particles along crest
                if (ticksAlive % 4 == 0 && ticksAlive > 20) {
                    Location crest = c.clone().add(dirX * 3, 5, dirZ * 3);
                    DisplayBuilder.dustParticles(crest, 6, 1.0, 150, 230, 255, 1.5f);
                }
            }

            // Phase 2: Shatter (tick 50)
            if (ticksAlive == 50 && !shattered) {
                shattered = true;

                // Spawn fragment blocks scattering outward
                for (int i = 0; i < 12; i++) {
                    double fragAngle = Math.random() * Math.PI * 2;
                    double fragDist = 1.0 + Math.random() * 3.0;
                    Location fragLoc = c.clone().add(
                            Math.cos(fragAngle) * fragDist + dirX * 3,
                            3.0 + Math.random() * 2.0,
                            Math.sin(fragAngle) * fragDist + dirZ * 3
                    );
                    BlockDisplayHandle frag = displayBuilder.spawnBlock(fragLoc, Material.BLUE_ICE);
                    float fragScale = 0.2f + (float)(Math.random() * 0.3);
                    frag.scale(fragScale, fragScale, fragScale).glow(180, 210, 255).interpolation(2, 0);
                    frag.rotate((float)(Math.random() * 3), 1, 1, 0);
                    fragmentBlocks.add(frag);
                    spawnedEntities.add(frag.entity());
                }

                // Impact
                Location impactLoc = c.clone().add(dirX * 3, 0, dirZ * 3);
                triggerImpactDamage(impactLoc);
                DisplayBuilder.playSound(impactLoc, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.4f);
                DisplayBuilder.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 0.8f);
                DisplayBuilder.dustParticles(impactLoc, 50, 3.0, 180, 210, 255, 2.0f);
                c.getWorld().spawnParticle(Particle.SNOWFLAKE, impactLoc.clone().add(0, 2, 0), 40, 2.0, 1.5, 2.0, 0.05);
            }

            // Phase 3: Fragments scatter and fall (ticks 51+)
            if (ticksAlive > 50 && !fragmentBlocks.isEmpty()) {
                double fallProgress = (ticksAlive - 50) / 40.0;
                for (BlockDisplayHandle frag : fragmentBlocks) {
                    BlockDisplay bd = frag.entity();
                    if (bd.isValid()) {
                        Location loc = bd.getLocation();
                        loc.add(0, -0.15, 0); // Gravity
                        bd.teleport(loc);
                        frag.rotate((float)(ticksAlive * 0.2), 1, 0.5f, 0.5f);
                        frag.interpolation(2, 0);
                    }
                }

                // Fade original wave
                for (BlockDisplayHandle h : waveBlocks) {
                    BlockDisplay bd = h.entity();
                    if (bd.isValid()) {
                        Location loc = bd.getLocation();
                        loc.add(0, -0.1, 0);
                        bd.teleport(loc);
                    }
                }
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new FrozenBreaker(plugin); }
    }

    // ================================================================
    // 12. ABYSSAL TENDRIL — DARK_PRISMARINE tentacle curving up
    //     Sways side to side.
    // ================================================================
    public static class AbyssalTendril extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tendrilBlocks = new ArrayList<>();
        private float swayPhase = 0;

        public AbyssalTendril(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("abyssal_tendril", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(6.0);
            config.setDamageRadius(3.5);
            config.setDurationTicks(300);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(25);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Tentacle: 13 DARK_PRISMARINE blocks curving upward
            for (int i = 0; i < 13; i++) {
                double t = i / 12.0;
                double height = t * 7.0; // Rise 7 blocks
                double curveX = Math.sin(t * Math.PI * 0.8) * 2.0;
                double curveZ = Math.cos(t * Math.PI * 0.6) * 1.0;
                Location loc = center.clone().add(curveX, height - 2, curveZ);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                // Taper from thick base to thin tip
                float taper = 1.0f - (float)t * 0.6f;
                h.scale(taper, 0.9f, taper).glow(100, 180, 220).interpolation(3, 0);
                tendrilBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ENTITY_DROWNED_AMBIENT, 1.0f, 0.3f);
            DisplayBuilder.playSound(center, Sound.AMBIENT_UNDERWATER_LOOP, 0.7f, 0.5f);
            DisplayBuilder.dustParticles(center, 20, 1.5, 150, 230, 255, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Emerge over first 30 ticks
            float emergeProgress = Math.min(1.0f, ticksAlive / 30.0f);

            // Sway side to side
            swayPhase += 0.06f;

            for (int i = 0; i < tendrilBlocks.size(); i++) {
                double t = i / 12.0;
                double height = t * 7.0 * emergeProgress;
                // Base curve + sway (sway increases toward tip)
                double swayAmount = Math.sin(swayPhase + t * 2) * t * 1.5;
                double swayZ = Math.cos(swayPhase * 0.7 + t * 1.5) * t * 0.8;
                double curveX = Math.sin(t * Math.PI * 0.8) * 2.0 + swayAmount;
                double curveZ = Math.cos(t * Math.PI * 0.6) * 1.0 + swayZ;

                Location target = c.clone().add(curveX, height - 2 + emergeProgress * 2, curveZ);
                tendrilBlocks.get(i).entity().teleport(target);

                // Rotate segments to follow curve
                float segmentTilt = (float)(swayAmount * 0.15);
                tendrilBlocks.get(i).rotate(segmentTilt, 0, 0, 1);
                tendrilBlocks.get(i).interpolation(3, 0);
            }

            // Drip/ooze particles from tendril
            if (ticksAlive % 4 == 0 && emergeProgress >= 1.0f) {
                int idx = (int)(Math.random() * tendrilBlocks.size());
                Location drip = tendrilBlocks.get(idx).entity().getLocation();
                c.getWorld().spawnParticle(Particle.DRIPPING_WATER, drip, 3, 0.2, 0.3, 0.2, 0);
                DisplayBuilder.dustParticles(drip, 2, 0.3, 100, 180, 220, 0.8f);
            }

            // Tip glow pulse
            if (ticksAlive % 10 == 0 && !tendrilBlocks.isEmpty()) {
                BlockDisplayHandle tip = tendrilBlocks.get(tendrilBlocks.size() - 1);
                int pulse = 150 + (int)(Math.sin(ticksAlive * 0.2) * 50);
                tip.glow(pulse, 230, 255);
            }

            // Ambient sound
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.ENTITY_DROWNED_AMBIENT, 0.5f, 0.4f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new AbyssalTendril(plugin); }
    }

    // ================================================================
    // 13. MAELSTROM EYE — Concentric PRISMARINE circles rotating
    //     opposite directions. Pull effect at center.
    // ================================================================
    public static class MaelstromEye extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> outerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> middleRing = new ArrayList<>();
        private final List<BlockDisplayHandle> innerRing = new ArrayList<>();
        private final List<BlockDisplayHandle> coreBlocks = new ArrayList<>();
        private float outerAngle = 0;
        private float innerAngle = 0;

        public MaelstromEye(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("maelstrom_eye", AttackType.BLOCK_DISPLAY, 1, "modes/bluemoon/attacks"));
            config.setDamage(8.0);
            config.setDamageRadius(4.0);
            config.setDurationTicks(280);
            config.setCooldownTicks(350);
            config.setTicksBetweenDamage(20);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Outer ring: 6 PRISMARINE at radius 3.5
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                Location loc = center.clone().add(Math.cos(angle) * 3.5, 0.5, Math.sin(angle) * 3.5);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE);
                h.scale(0.9f, 0.4f, 0.9f).glow(150, 230, 255).interpolation(3, 0);
                outerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Middle ring: 5 DARK_PRISMARINE at radius 2.2
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                Location loc = center.clone().add(Math.cos(angle) * 2.2, 0.5, Math.sin(angle) * 2.2);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.DARK_PRISMARINE);
                h.scale(0.7f, 0.5f, 0.7f).glow(100, 180, 220).interpolation(3, 0);
                middleRing.add(h);
                spawnedEntities.add(h.entity());
            }

            // Inner ring: 4 PRISMARINE_BRICKS at radius 1.0
            for (int i = 0; i < 4; i++) {
                double angle = (2 * Math.PI * i) / 4;
                Location loc = center.clone().add(Math.cos(angle) * 1.0, 0.5, Math.sin(angle) * 1.0);
                BlockDisplayHandle h = displayBuilder.spawnBlock(loc, Material.PRISMARINE_BRICKS);
                h.scale(0.5f, 0.6f, 0.5f).glow(180, 210, 255).interpolation(3, 0);
                innerRing.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.2f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_DROWNED_AMBIENT, 0.8f, 0.5f);
            DisplayBuilder.dustParticles(center, 30, 2.5, 150, 230, 255, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location c = getCenter();
            if (c == null || c.getWorld() == null) return;

            // Rotate outer ring clockwise, inner counter-clockwise
            outerAngle += (float) Math.toRadians(3); // 3 deg/tick CW
            innerAngle -= (float) Math.toRadians(5); // 5 deg/tick CCW
            float middleAngle = -outerAngle * 0.7f; // Opposite at different speed

            // Outer ring rotation
            for (int i = 0; i < outerRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6;
                double angle = baseAngle + outerAngle;
                Location target = c.clone().add(Math.cos(angle) * 3.5, 0.5, Math.sin(angle) * 3.5);
                outerRing.get(i).entity().teleport(target);
                outerRing.get(i).rotate(outerAngle, 0, 1, 0);
                outerRing.get(i).interpolation(2, 0);
            }

            // Middle ring rotation (opposite direction)
            for (int i = 0; i < middleRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 5;
                double angle = baseAngle + middleAngle;
                Location target = c.clone().add(Math.cos(angle) * 2.2, 0.5, Math.sin(angle) * 2.2);
                middleRing.get(i).entity().teleport(target);
                middleRing.get(i).rotate(middleAngle, 0, 1, 0);
                middleRing.get(i).interpolation(2, 0);
            }

            // Inner ring rotation (same as outer but faster)
            for (int i = 0; i < innerRing.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 4;
                double angle = baseAngle + innerAngle;
                Location target = c.clone().add(Math.cos(angle) * 1.0, 0.5, Math.sin(angle) * 1.0);
                innerRing.get(i).entity().teleport(target);
                innerRing.get(i).rotate(innerAngle, 0, 1, 0);
                innerRing.get(i).interpolation(2, 0);
            }

            // Pull effect: draw nearby players toward center
            if (ticksAlive % 4 == 0) {
                for (Player player : c.getWorld().getPlayers()) {
                    double dist = player.getLocation().distance(c);
                    if (dist <= 6.0 && dist > 0.5) {
                        Vector pull = c.toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.12);
                        pull.setY(-0.02);
                        player.setVelocity(player.getVelocity().add(pull));
                    }
                }
            }

            // Vortex particles spiraling inward
            if (ticksAlive % 2 == 0) {
                double pAngle = outerAngle * 3;
                for (int i = 0; i < 3; i++) {
                    double pRadius = 3.5 - i * 1.0;
                    double pA = pAngle + i * Math.PI * 0.6;
                    Location pLoc = c.clone().add(Math.cos(pA) * pRadius, 0.8, Math.sin(pA) * pRadius);
                    DisplayBuilder.dustParticles(pLoc, 3, 0.2, 150, 230, 255, 1.0f);
                }
            }

            // Center eye glow pulse
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.dustParticles(c.clone().add(0, 0.8, 0), 5, 0.3, 180, 210, 255, 1.5f);
                c.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, c.clone().add(0, 0.3, 0), 3, 0.3, 0.1, 0.3, 0);
            }

            // Particle rings at each orbit level
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.particleRing(c.clone().add(0, 0.3, 0), 3.5, Particle.DUST, 16,
                        new Particle.DustOptions(Color.fromRGB(150, 230, 255), 0.6f));
                DisplayBuilder.particleRing(c.clone().add(0, 0.5, 0), 2.2, Particle.DUST, 12,
                        new Particle.DustOptions(Color.fromRGB(180, 210, 255), 0.8f));
                DisplayBuilder.particleRing(c.clone().add(0, 0.7, 0), 1.0, Particle.DUST, 8,
                        new Particle.DustOptions(Color.fromRGB(200, 230, 255), 1.0f));
            }

            // Ambient sounds
            if (ticksAlive % 60 == 0) {
                DisplayBuilder.playSound(c, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6f, 0.5f);
            }
            if (ticksAlive % 80 == 0) {
                DisplayBuilder.playSound(c, Sound.AMBIENT_UNDERWATER_LOOP, 0.4f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() { displayBuilder.removeAll(); }

        @Override
        public AbstractAttack newInstance() { return new MaelstromEye(plugin); }
    }
}
