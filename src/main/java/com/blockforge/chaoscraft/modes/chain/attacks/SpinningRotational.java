package com.blockforge.chaoscraft.modes.chain.attacks;

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
 * 15 chain-themed spinning/rotational BlockDisplay attacks for Chain Mode.
 * All attacks use iron/chain color palette: Iron gray RGB(180,180,190),
 * Dark RGB(100,100,110), Rust RGB(180,100,40).
 * Materials: CHAIN, IRON_BLOCK, NETHERITE_BLOCK, DEEPSLATE, HEAVY_CORE, ANVIL.
 */
public final class SpinningRotational {
    private SpinningRotational() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ChainBuzzsaw(plugin));
        registry.register(new IronWindmill(plugin));
        registry.register(new ChainDrill(plugin));
        registry.register(new TornadoOfChains(plugin));
        registry.register(new SpinningChainStar(plugin));
        registry.register(new ChainCentrifuge(plugin));
        registry.register(new HelixSpiral(plugin));
        registry.register(new ChainPropeller(plugin));
        registry.register(new GrindingGears(plugin));
        registry.register(new ChainWhirlpool(plugin));
        registry.register(new RotatingChainCross(plugin));
        registry.register(new ChainTurbine(plugin));
        registry.register(new OrbitalRings(plugin));
        registry.register(new ChainRoulette(plugin));
        registry.register(new MeatGrinderSpiral(plugin));
    }

    // ================================================================
    // 1. CHAIN BUZZSAW
    // 12 chain blocks in a flat disc (radius 3). Spins rapidly around
    // center axis. Slowly moves forward. Spark particles at cutting edge.
    // Grinding saw sound. Continuous damage.
    // ================================================================
    public static class ChainBuzzsaw extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> discBlocks = new ArrayList<>();
        private double forwardX = 0;
        private double forwardZ = 0;
        private double moveAngle;

        public ChainBuzzsaw(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_buzzsaw", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            moveAngle = Math.random() * 2 * Math.PI;

            // 12 chain blocks in a flat disc at radius 3
            for (int i = 0; i < 12; i++) {
                double angle = (2 * Math.PI * i) / 12;
                double x = Math.cos(angle) * 3.0;
                double z = Math.sin(angle) * 3.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 0, z), Material.CHAIN);
                h.scale(1.2f, 1.2f, 1.2f).glow(180, 180, 190).interpolation(2, 0);
                discBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Rapid spin: 6 degrees/tick = 0.1047 rad/tick
            double spinAngle = ticksAlive * 0.1047;

            // Slowly move forward at 0.06 blocks/tick
            forwardX += Math.cos(moveAngle) * 0.06;
            forwardZ += Math.sin(moveAngle) * 0.06;

            Location offset = center.clone().add(forwardX, 0, forwardZ);
            setCenter(offset);

            for (int i = 0; i < discBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 12 + spinAngle;
                double x = Math.cos(baseAngle) * 3.0;
                double z = Math.sin(baseAngle) * 3.0;
                discBlocks.get(i).entity().teleport(offset.clone().add(x, 0, z));
            }

            // Spark particles at the leading edge every 2 ticks
            if (ticksAlive % 2 == 0) {
                double edgeAngle = spinAngle;
                Location sparkLoc = offset.clone().add(
                        Math.cos(edgeAngle) * 3.0, 0, Math.sin(edgeAngle) * 3.0);
                w.spawnParticle(Particle.LAVA, sparkLoc, 3, 0.2, 0.1, 0.2, 0);
                DisplayBuilder.dustParticles(sparkLoc, 2, 0.15, 255, 200, 50, 0.8f);
            }

            // Grinding saw sound loop every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(offset, Sound.BLOCK_GRINDSTONE_USE, 1.2f, 1.5f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainBuzzsaw(plugin);
        }
    }

    // ================================================================
    // 2. IRON WINDMILL
    // 4 arms of 3 chains each from a central hub (13 blocks total).
    // Spins horizontally like a windmill. Arms gradually extend longer.
    // Whooshing air sounds on each arm pass.
    // ================================================================
    public static class IronWindmill extends BlockDisplayAttack {

        private BlockDisplayHandle hub;
        private final List<BlockDisplayHandle> armBlocks = new ArrayList<>();
        private double currentRadius = 1.5;

        public IronWindmill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("iron_windmill", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central hub (iron block)
            hub = displayBuilder.spawnBlock(center.clone(), Material.IRON_BLOCK);
            hub.scale(1.8f, 1.8f, 1.8f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(hub.entity());

            // 4 arms, 3 chains each = 12 chain blocks
            for (int arm = 0; arm < 4; arm++) {
                double armAngle = (Math.PI / 2) * arm;
                for (int seg = 1; seg <= 3; seg++) {
                    double x = Math.cos(armAngle) * seg * 1.5;
                    double z = Math.sin(armAngle) * seg * 1.5;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            center.clone().add(x, 0, z), Material.CHAIN);
                    h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                    armBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.6f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin speed increases from 2 deg/tick to 5 deg/tick over duration
            double speedFactor = 0.035 + (ticksAlive / (double) config.getDurationTicks()) * 0.052;
            double spinAngle = ticksAlive * speedFactor;

            // Arms extend from 1.5 to 3.0 radius over duration
            currentRadius = 1.5 + (ticksAlive / (double) config.getDurationTicks()) * 1.5;

            hub.entity().teleport(center);

            int idx = 0;
            for (int arm = 0; arm < 4; arm++) {
                double armAngle = (Math.PI / 2) * arm + spinAngle;
                for (int seg = 1; seg <= 3; seg++) {
                    double r = currentRadius * seg / 3.0;
                    double x = Math.cos(armAngle) * r;
                    double z = Math.sin(armAngle) * r;
                    armBlocks.get(idx).entity().teleport(center.clone().add(x, 0, z));
                    idx++;
                }
            }

            // Whooshing air sounds every 10 ticks
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 0.8f, 1.2f);
            }

            // Wind particles at arm tips every 3 ticks
            if (ticksAlive % 3 == 0) {
                for (int arm = 0; arm < 4; arm++) {
                    double tipAngle = (Math.PI / 2) * arm + spinAngle;
                    Location tip = center.clone().add(
                            Math.cos(tipAngle) * currentRadius, 0, Math.sin(tipAngle) * currentRadius);
                    w.spawnParticle(Particle.CLOUD, tip, 2, 0.1, 0.05, 0.1, 0.02);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new IronWindmill(plugin);
        }
    }

    // ================================================================
    // 3. CHAIN DRILL
    // Pointed spiral of 14 chains forming a drill shape. Spins on
    // vertical axis while descending into the ground. Debris particles
    // spray outward. Drilling/grinding sound. Damage at the drill tip.
    // ================================================================
    public static class ChainDrill extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> drillBlocks = new ArrayList<>();
        private double yOffset = 5.0;

        public ChainDrill(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_drill", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(180);
            config.setCooldownTicks(260);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 chains in a spiral drill shape
            // Spiral from top (wide, radius 2) to bottom (narrow, radius 0.3)
            for (int i = 0; i < 14; i++) {
                double t = i / 13.0; // 0 to 1 (top to tip)
                double spiralAngle = t * Math.PI * 4; // 2 full rotations
                double radius = 2.0 * (1.0 - t) + 0.3 * t;
                double y = (1.0 - t) * 4.0; // 4 blocks tall
                double x = Math.cos(spiralAngle) * radius;
                double z = Math.sin(spiralAngle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y + yOffset, z), Material.CHAIN);
                h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                drillBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin at 8 deg/tick = 0.1396 rad/tick
            double spinAngle = ticksAlive * 0.1396;

            // Descend at 0.04 blocks/tick
            yOffset = Math.max(-1.0, 5.0 - ticksAlive * 0.04);

            for (int i = 0; i < drillBlocks.size(); i++) {
                double t = i / 13.0;
                double spiralAngle = t * Math.PI * 4 + spinAngle;
                double radius = 2.0 * (1.0 - t) + 0.3 * t;
                double y = (1.0 - t) * 4.0;
                double x = Math.cos(spiralAngle) * radius;
                double z = Math.sin(spiralAngle) * radius;
                drillBlocks.get(i).entity().teleport(center.clone().add(x, y + yOffset, z));
            }

            // Debris particles spray outward from drill tip
            if (ticksAlive % 3 == 0) {
                Location tipLoc = center.clone().add(0, yOffset, 0);
                w.spawnParticle(Particle.BLOCK, tipLoc, 8, 0.5, 0.2, 0.5, 0.1,
                        Material.GRAVEL.createBlockData());
                DisplayBuilder.dustParticles(tipLoc, 3, 0.4, 180, 100, 40, 1.0f);
            }

            // Grinding sound every 12 ticks
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center.clone().add(0, yOffset, 0),
                        Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.8f + (float) (ticksAlive * 0.003));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainDrill(plugin);
        }
    }

    // ================================================================
    // 4. TORNADO OF CHAINS
    // 18 chains in a vertical spiral (tornado funnel shape). Rotates
    // around its axis. Bottom spins faster than top. Wind particles.
    // Grows taller over time.
    // ================================================================
    public static class TornadoOfChains extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> tornadoBlocks = new ArrayList<>();
        private double heightMultiplier = 1.0;

        public TornadoOfChains(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("tornado_of_chains", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 18 chains in a tornado funnel (bottom narrow, top wide)
            for (int i = 0; i < 18; i++) {
                double t = i / 17.0; // 0=bottom, 1=top
                double radius = 1.0 + t * 3.0; // 1 at bottom, 4 at top
                double y = t * 6.0;
                double angle = t * Math.PI * 6; // 3 full spirals
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;

                Material mat = (i % 3 == 0) ? Material.IRON_BLOCK : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                h.scale(1.1f, 1.1f, 1.1f).glow(180, 180, 190).interpolation(2, 0);
                tornadoBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Grows taller over time (1.0 to 1.8)
            heightMultiplier = 1.0 + (ticksAlive / (double) config.getDurationTicks()) * 0.8;

            for (int i = 0; i < tornadoBlocks.size(); i++) {
                double t = i / 17.0;

                // Bottom spins faster (5x at bottom, 1x at top)
                double speedFactor = 5.0 - t * 4.0;
                double spinAngle = ticksAlive * 0.06 * speedFactor;

                double radius = 1.0 + t * 3.0;
                double y = t * 6.0 * heightMultiplier;
                double baseAngle = t * Math.PI * 6 + spinAngle;
                double x = Math.cos(baseAngle) * radius;
                double z = Math.sin(baseAngle) * radius;

                tornadoBlocks.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Wind particles spiraling up
            if (ticksAlive % 2 == 0) {
                double windT = (ticksAlive % 60) / 60.0;
                double windRadius = 1.0 + windT * 3.0;
                double windY = windT * 6.0 * heightMultiplier;
                double windAngle = windT * Math.PI * 6 + ticksAlive * 0.3;
                Location windLoc = center.clone().add(
                        Math.cos(windAngle) * windRadius, windY, Math.sin(windAngle) * windRadius);
                w.spawnParticle(Particle.CLOUD, windLoc, 2, 0.2, 0.1, 0.2, 0.02);
                DisplayBuilder.dustParticles(windLoc, 2, 0.2, 180, 180, 190, 0.7f);
            }

            // Elytra whooshing every 20 ticks
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.0f, 0.6f);
            }

            // Metallic clink at base every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.8f, 0.5f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new TornadoOfChains(plugin);
        }
    }

    // ================================================================
    // 5. SPINNING CHAIN STAR
    // 5 arms of 2 chains each forming a star (10 chains + 1 center = 11).
    // Spins flat at waist height. Tips glow brighter. Star shape clearly
    // recognizable. Metallic hum sound.
    // ================================================================
    public static class SpinningChainStar extends BlockDisplayAttack {

        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> starBlocks = new ArrayList<>();

        public SpinningChainStar(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("spinning_chain_star", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(60.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location waist = center.clone().add(0, 1.0, 0);

            // Center heavy core
            centerBlock = displayBuilder.spawnBlock(waist, Material.HEAVY_CORE);
            centerBlock.scale(1.35f, 1.35f, 1.35f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // 5 arms, 2 chains each
            for (int arm = 0; arm < 5; arm++) {
                double armAngle = (2 * Math.PI * arm) / 5;
                for (int seg = 1; seg <= 2; seg++) {
                    double r = seg * 1.8;
                    double x = Math.cos(armAngle) * r;
                    double z = Math.sin(armAngle) * r;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            waist.clone().add(x, 0, z), Material.CHAIN);
                    // Tips glow brighter (rust orange)
                    if (seg == 2) {
                        h.scale(1.2f, 1.2f, 1.2f).glow(255, 180, 80).interpolation(2, 0);
                    } else {
                        h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                    }
                    starBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location waist = center.clone().add(0, 1.0, 0);

            // Spin at 3 deg/tick = 0.05236 rad/tick
            double spinAngle = ticksAlive * 0.05236;

            centerBlock.entity().teleport(waist);

            int idx = 0;
            for (int arm = 0; arm < 5; arm++) {
                double armAngle = (2 * Math.PI * arm) / 5 + spinAngle;
                for (int seg = 1; seg <= 2; seg++) {
                    double r = seg * 1.8;
                    double x = Math.cos(armAngle) * r;
                    double z = Math.sin(armAngle) * r;
                    starBlocks.get(idx).entity().teleport(waist.clone().add(x, 0, z));
                    idx++;
                }
            }

            // Glow trail at tips every 2 ticks
            if (ticksAlive % 2 == 0) {
                for (int arm = 0; arm < 5; arm++) {
                    double tipAngle = (2 * Math.PI * arm) / 5 + spinAngle;
                    Location tipLoc = waist.clone().add(
                            Math.cos(tipAngle) * 3.6, 0, Math.sin(tipAngle) * 3.6);
                    DisplayBuilder.dustParticles(tipLoc, 2, 0.1, 255, 180, 80, 1.0f);
                }
            }

            // Metallic hum every 30 ticks
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(waist, Sound.BLOCK_BEACON_AMBIENT, 0.7f, 0.4f);
            }

            // Chain clink every 8 ticks
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(waist, Sound.BLOCK_CHAIN_PLACE, 0.6f, 1.4f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new SpinningChainStar(plugin);
        }
    }

    // ================================================================
    // 6. CHAIN CENTRIFUGE
    // Vertical drum (4 iron blocks forming a pillar) with 8 chains
    // attached at the middle. As drum spins, chains extend outward
    // horizontally. Increasing radius danger zone. Centrifugal force visual.
    // ================================================================
    public static class ChainCentrifuge extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> pillarBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> chainBlocks = new ArrayList<>();
        private double chainRadius = 0.8;

        public ChainCentrifuge(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_centrifuge", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(11.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 4 iron blocks forming a vertical pillar
            for (int y = 0; y < 4; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y, 0), Material.IRON_BLOCK);
                h.scale(1.35f, 1.35f, 1.35f).glow(180, 180, 190).interpolation(2, 0);
                pillarBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 8 chains at the middle height (y=1.5), evenly spaced
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * chainRadius;
                double z = Math.sin(angle) * chainRadius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.5, z), Material.CHAIN);
                h.scale(0.9f, 0.9f, 0.9f).glow(100, 100, 110).interpolation(2, 0);
                chainBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_USE, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin accelerates: 2 deg/tick up to 7 deg/tick
            double speedFactor = 0.035 + (ticksAlive / (double) config.getDurationTicks()) * 0.087;
            double spinAngle = ticksAlive * speedFactor;

            // Chains extend outward over time: 0.8 to 4.5 radius
            chainRadius = 0.8 + (ticksAlive / (double) config.getDurationTicks()) * 3.7;

            // Pillar rotates in place
            for (int y = 0; y < pillarBlocks.size(); y++) {
                double px = Math.cos(spinAngle) * 0.01;
                double pz = Math.sin(spinAngle) * 0.01;
                pillarBlocks.get(y).entity().teleport(center.clone().add(px, y, pz));
            }

            // Chains orbit outward
            for (int i = 0; i < chainBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 8 + spinAngle;
                double x = Math.cos(angle) * chainRadius;
                double z = Math.sin(angle) * chainRadius;
                // Chains droop slightly as they extend (lower y at greater radius)
                double droop = Math.min(0.8, chainRadius * 0.1);
                chainBlocks.get(i).entity().teleport(center.clone().add(x, 1.5 - droop, z));
            }

            // Centrifugal force particle trails every 3 ticks
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI * i) / 8 + spinAngle;
                    Location chainLoc = center.clone().add(
                            Math.cos(angle) * chainRadius, 1.5, Math.sin(angle) * chainRadius);
                    DisplayBuilder.dustParticles(chainLoc, 1, 0.1, 180, 180, 190, 0.6f);
                }
            }

            // Metallic whirring sound every 18 ticks
            if (ticksAlive % 18 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.9f,
                        0.8f + (float) (ticksAlive * 0.002));
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainCentrifuge(plugin);
        }
    }

    // ================================================================
    // 7. HELIX SPIRAL
    // 12 chains in a double-helix (DNA strand pattern). Rotates around
    // vertical axis. Glowing particles trace the helix path.
    // Ethereal hum. Mesmerizing visual.
    // ================================================================
    public static class HelixSpiral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> helixA = new ArrayList<>();
        private final List<BlockDisplayHandle> helixB = new ArrayList<>();

        public HelixSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("helix_spiral", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(12);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double radius = 2.0;
            double height = 6.0;

            // Strand A: 6 chains
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double angle = t * Math.PI * 4; // 2 full turns
                double y = t * height;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), Material.CHAIN);
                h.scale(1.05f, 1.05f, 1.05f).glow(180, 180, 190).interpolation(2, 0);
                helixA.add(h);
                spawnedEntities.add(h.entity());
            }

            // Strand B: 6 chains (offset by PI)
            for (int i = 0; i < 6; i++) {
                double t = i / 5.0;
                double angle = t * Math.PI * 4 + Math.PI; // opposite strand
                double y = t * height;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), Material.DEEPSLATE);
                h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                helixB.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double spinAngle = ticksAlive * 0.04; // ~2.3 deg/tick
            double radius = 2.0;
            double height = 6.0;

            // Animate strand A
            for (int i = 0; i < helixA.size(); i++) {
                double t = i / 5.0;
                double angle = t * Math.PI * 4 + spinAngle;
                double y = t * height;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                helixA.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Animate strand B (offset by PI)
            for (int i = 0; i < helixB.size(); i++) {
                double t = i / 5.0;
                double angle = t * Math.PI * 4 + Math.PI + spinAngle;
                double y = t * height;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                helixB.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Glowing particles trace the helix path every 2 ticks
            if (ticksAlive % 2 == 0) {
                int tracePoints = 20;
                for (int i = 0; i < tracePoints; i++) {
                    double t = i / (double) tracePoints;
                    double angleA = t * Math.PI * 4 + spinAngle;
                    double y = t * height;
                    Location pA = center.clone().add(
                            Math.cos(angleA) * radius, y, Math.sin(angleA) * radius);
                    DisplayBuilder.dustParticles(pA, 1, 0.05, 180, 180, 190, 0.5f);

                    double angleB = angleA + Math.PI;
                    Location pB = center.clone().add(
                            Math.cos(angleB) * radius, y, Math.sin(angleB) * radius);
                    DisplayBuilder.dustParticles(pB, 1, 0.05, 100, 100, 110, 0.5f);
                }
            }

            // Ethereal hum every 40 ticks
            if (ticksAlive % 40 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.6f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new HelixSpiral(plugin);
        }
    }

    // ================================================================
    // 8. CHAIN PROPELLER
    // 3 blades of 4 chains each (12 chains), tilted 15 deg from
    // horizontal. Spins like a helicopter rotor. Downward particle "wind."
    // Lifts slightly off ground as it accelerates. Chopper sounds.
    // ================================================================
    public static class ChainPropeller extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> bladeBlocks = new ArrayList<>();
        private double liftY = 0;

        public ChainPropeller(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_propeller", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location rotor = center.clone().add(0, 2.0, 0);

            // 3 blades, 4 chains each
            for (int blade = 0; blade < 3; blade++) {
                double bladeAngle = (2 * Math.PI * blade) / 3;
                for (int seg = 1; seg <= 4; seg++) {
                    double r = seg * 1.2;
                    double x = Math.cos(bladeAngle) * r;
                    double z = Math.sin(bladeAngle) * r;
                    // 15 deg tilt: y offset increases with radius
                    double tiltY = Math.sin(Math.toRadians(15)) * r;
                    Material mat = (seg == 4) ? Material.NETHERITE_BLOCK : Material.CHAIN;
                    BlockDisplayHandle h = displayBuilder.spawnBlock(
                            rotor.clone().add(x, tiltY, z), mat);
                    h.scale(1.05f, 1.05f, 1.05f).glow(180, 180, 190).interpolation(2, 0);
                    bladeBlocks.add(h);
                    spawnedEntities.add(h.entity());
                }
            }

            DisplayBuilder.playSound(center, Sound.ITEM_ELYTRA_FLYING, 1.2f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin accelerates: 4 deg/tick to 10 deg/tick
            double speedFactor = 0.07 + (ticksAlive / (double) config.getDurationTicks()) * 0.105;
            double spinAngle = ticksAlive * speedFactor;

            // Lifts off ground as it accelerates (up to 2.5 blocks higher)
            liftY = (ticksAlive / (double) config.getDurationTicks()) * 2.5;

            Location rotor = center.clone().add(0, 2.0 + liftY, 0);

            int idx = 0;
            for (int blade = 0; blade < 3; blade++) {
                double bladeAngle = (2 * Math.PI * blade) / 3 + spinAngle;
                for (int seg = 1; seg <= 4; seg++) {
                    double r = seg * 1.2;
                    double x = Math.cos(bladeAngle) * r;
                    double z = Math.sin(bladeAngle) * r;
                    double tiltY = Math.sin(Math.toRadians(15)) * r;
                    bladeBlocks.get(idx).entity().teleport(rotor.clone().add(x, tiltY, z));
                    idx++;
                }
            }

            // Downward wind particles every 2 ticks
            if (ticksAlive % 2 == 0) {
                for (int i = 0; i < 6; i++) {
                    double pAngle = Math.random() * 2 * Math.PI;
                    double pR = Math.random() * 3.0;
                    Location windLoc = rotor.clone().add(
                            Math.cos(pAngle) * pR, -0.5, Math.sin(pAngle) * pR);
                    w.spawnParticle(Particle.CLOUD, windLoc, 1, 0.1, 0.3, 0.1, 0.05);
                }
            }

            // Chopper sounds every 8 ticks (faster pitch as speed increases)
            if (ticksAlive % 8 == 0) {
                float pitch = 0.5f + (float) (ticksAlive / (double) config.getDurationTicks()) * 1.0f;
                DisplayBuilder.playSound(rotor, Sound.ITEM_ELYTRA_FLYING, 1.0f, pitch);
            }

            // Blade tip sparks every 5 ticks
            if (ticksAlive % 5 == 0) {
                for (int blade = 0; blade < 3; blade++) {
                    double tipAngle = (2 * Math.PI * blade) / 3 + spinAngle;
                    double tipR = 4.8;
                    Location tipLoc = rotor.clone().add(
                            Math.cos(tipAngle) * tipR,
                            Math.sin(Math.toRadians(15)) * tipR,
                            Math.sin(tipAngle) * tipR);
                    DisplayBuilder.dustParticles(tipLoc, 2, 0.1, 180, 100, 40, 0.8f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainPropeller(plugin);
        }
    }

    // ================================================================
    // 9. GRINDING GEARS
    // 2 interlocking gear-shaped rings (each 8 chains in a circle,
    // offset to mesh). Counter-rotate like meshing gears. Chains at
    // mesh point glow hot. Grinding metal sounds. Damage at mesh zone.
    // ================================================================
    public static class GrindingGears extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> gearA = new ArrayList<>();
        private final List<BlockDisplayHandle> gearB = new ArrayList<>();

        public GrindingGears(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("grinding_gears", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(72.0);
            config.setDamageRadius(8.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            double gearRadius = 2.5;
            // Gear A center offset by -gearRadius on X
            Location gearACenter = center.clone().add(-gearRadius + 0.3, 0.5, 0);
            // Gear B center offset by +gearRadius on X
            Location gearBCenter = center.clone().add(gearRadius - 0.3, 0.5, 0);

            // 8 chains each gear
            for (int i = 0; i < 8; i++) {
                double angle = (2 * Math.PI * i) / 8;
                double x = Math.cos(angle) * gearRadius;
                double z = Math.sin(angle) * gearRadius;

                BlockDisplayHandle hA = displayBuilder.spawnBlock(
                        gearACenter.clone().add(x, 0, z), Material.CHAIN);
                hA.scale(1.2f, 1.2f, 1.2f).glow(180, 180, 190).interpolation(2, 0);
                gearA.add(hA);
                spawnedEntities.add(hA.entity());

                BlockDisplayHandle hB = displayBuilder.spawnBlock(
                        gearBCenter.clone().add(x, 0, z), Material.CHAIN);
                hB.scale(1.2f, 1.2f, 1.2f).glow(180, 180, 190).interpolation(2, 0);
                gearB.add(hB);
                spawnedEntities.add(hB.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            double gearRadius = 2.5;
            // Spin at 3 deg/tick = 0.05236 rad/tick
            double spinAngle = ticksAlive * 0.05236;

            Location gearACenter = center.clone().add(-gearRadius + 0.3, 0.5, 0);
            Location gearBCenter = center.clone().add(gearRadius - 0.3, 0.5, 0);

            // Gear A rotates clockwise
            for (int i = 0; i < gearA.size(); i++) {
                double angle = (2 * Math.PI * i) / 8 + spinAngle;
                double x = Math.cos(angle) * gearRadius;
                double z = Math.sin(angle) * gearRadius;
                Location blockLoc = gearACenter.clone().add(x, 0, z);

                // Glow hot near mesh zone (near center between gears)
                double distToMesh = blockLoc.distance(center.clone().add(0, 0.5, 0));
                if (distToMesh < 1.5) {
                    gearA.get(i).glow(255, 120, 30); // hot orange
                } else {
                    gearA.get(i).glow(180, 180, 190); // normal
                }

                gearA.get(i).entity().teleport(blockLoc);
            }

            // Gear B counter-rotates
            for (int i = 0; i < gearB.size(); i++) {
                double angle = (2 * Math.PI * i) / 8 - spinAngle;
                double x = Math.cos(angle) * gearRadius;
                double z = Math.sin(angle) * gearRadius;
                Location blockLoc = gearBCenter.clone().add(x, 0, z);

                double distToMesh = blockLoc.distance(center.clone().add(0, 0.5, 0));
                if (distToMesh < 1.5) {
                    gearB.get(i).glow(255, 120, 30);
                } else {
                    gearB.get(i).glow(180, 180, 190);
                }

                gearB.get(i).entity().teleport(blockLoc);
            }

            // Spark particles at mesh point every 3 ticks
            if (ticksAlive % 3 == 0) {
                Location meshPoint = center.clone().add(0, 0.5, 0);
                w.spawnParticle(Particle.LAVA, meshPoint, 4, 0.3, 0.2, 0.3, 0);
                DisplayBuilder.dustParticles(meshPoint, 3, 0.2, 255, 120, 30, 1.2f);
            }

            // Grinding metal sounds every 10 ticks
            if (ticksAlive % 10 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 0.6f);
            }

            // Heavy clank when teeth "mesh" every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new GrindingGears(plugin);
        }
    }

    // ================================================================
    // 10. CHAIN WHIRLPOOL
    // 16 chains start in a wide circle (radius 5) and spiral inward
    // while spinning. Creates a whirlpool effect. Chains descend as
    // they spiral in. Water-like particle spiral. Crushing center damage.
    // ================================================================
    public static class ChainWhirlpool extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> whirlBlocks = new ArrayList<>();

        public ChainWhirlpool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_whirlpool", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(75.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 16 chains in a wide circle at radius 5
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                double x = Math.cos(angle) * 5.0;
                double z = Math.sin(angle) * 5.0;
                Material mat = (i % 4 == 0) ? Material.ANVIL : Material.CHAIN;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, 1.0, z), mat);
                h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                whirlBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_WATER_AMBIENT, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Progress 0 to 1 over duration
            double progress = Math.min(1.0, ticksAlive / (double) config.getDurationTicks());

            // Radius shrinks from 5 to 0.5, speed increases
            double currentRadius = 5.0 - progress * 4.5;
            double spinSpeed = 0.04 + progress * 0.12; // accelerating spin
            double spinAngle = ticksAlive * spinSpeed;

            // Y descends from 1.0 to -1.0 as spiral tightens
            double currentY = 1.0 - progress * 2.0;

            for (int i = 0; i < whirlBlocks.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 16 + spinAngle;
                // Each block has a slight spiral offset based on index
                double spiralOffset = (i / 16.0) * Math.PI * 2 * progress;
                double angle = baseAngle + spiralOffset;
                double x = Math.cos(angle) * currentRadius;
                double z = Math.sin(angle) * currentRadius;
                double y = currentY - (i / 16.0) * progress * 1.5;
                whirlBlocks.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Water-like spiral particles every 2 ticks
            if (ticksAlive % 2 == 0) {
                int trailPoints = 12;
                for (int i = 0; i < trailPoints; i++) {
                    double t = i / (double) trailPoints;
                    double pRadius = currentRadius * (1.0 - t * 0.3);
                    double pAngle = spinAngle + t * Math.PI * 2;
                    double pY = currentY - t * 0.5;
                    Location pLoc = center.clone().add(
                            Math.cos(pAngle) * pRadius, pY, Math.sin(pAngle) * pRadius);
                    DisplayBuilder.dustParticles(pLoc, 1, 0.05, 100, 100, 110, 0.6f);
                }
            }

            // Crushing sound as center tightens
            if (ticksAlive % 20 == 0) {
                float pitch = 0.3f + (float) progress * 1.2f;
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 1.0f, pitch);
            }

            // Chain rattling every 12 ticks
            if (ticksAlive % 12 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.8f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainWhirlpool(plugin);
        }
    }

    // ================================================================
    // 11. ROTATING CHAIN CROSS
    // Large cross shape (5 horizontal + 5 vertical, center shared =
    // 9 blocks + extras). Rotates on its center like a clock hand.
    // Alternates between slow and fast rotation. Tick-tock sounds.
    // ================================================================
    public static class RotatingChainCross extends BlockDisplayAttack {

        private BlockDisplayHandle centerBlock;
        private final List<BlockDisplayHandle> horizontalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> verticalBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> cornerAccents = new ArrayList<>();
        private boolean fastPhase = false;

        public RotatingChainCross(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("rotating_chain_cross", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(63.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location mid = center.clone().add(0, 1.5, 0);

            // Center block (heavy core)
            centerBlock = displayBuilder.spawnBlock(mid, Material.HEAVY_CORE);
            centerBlock.scale(1.5f, 1.5f, 1.5f).glow(180, 180, 190).interpolation(2, 0);
            spawnedEntities.add(centerBlock.entity());

            // Horizontal arm: 2 blocks each side of center (4 total)
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(i * 1.5, 0, 0), Material.CHAIN);
                h.scale(1.2f, 1.2f, 1.2f).glow(100, 100, 110).interpolation(2, 0);
                horizontalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // Vertical arm: 2 blocks each side of center (4 total)
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(0, 0, i * 1.5), Material.CHAIN);
                h.scale(1.2f, 1.2f, 1.2f).glow(100, 100, 110).interpolation(2, 0);
                verticalBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 4 corner accent blocks (deepslate)
            double[][] cornerOffsets = {{1.5, 0, 1.5}, {1.5, 0, -1.5}, {-1.5, 0, 1.5}, {-1.5, 0, -1.5}};
            for (double[] off : cornerOffsets) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(off[0], off[1], off[2]), Material.DEEPSLATE);
                h.scale(0.75f, 0.75f, 0.75f).glow(80, 80, 90).interpolation(2, 0);
                cornerAccents.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location mid = center.clone().add(0, 1.5, 0);

            // Alternate slow/fast every 40 ticks
            fastPhase = ((ticksAlive / 40) % 2 == 1);
            double speed = fastPhase ? 0.105 : 0.026; // ~6 deg/tick or ~1.5 deg/tick
            double spinAngle = ticksAlive * speed;

            centerBlock.entity().teleport(mid);

            // Horizontal arm rotates around Y axis
            int[] hOffsets = {-2, -1, 1, 2};
            for (int i = 0; i < horizontalBlocks.size(); i++) {
                double r = hOffsets[i] * 1.5;
                double x = Math.cos(spinAngle) * r;
                double z = Math.sin(spinAngle) * r;
                horizontalBlocks.get(i).entity().teleport(mid.clone().add(x, 0, z));
            }

            // Vertical arm (perpendicular to horizontal, rotated 90 deg offset)
            for (int i = 0; i < verticalBlocks.size(); i++) {
                double r = hOffsets[i] * 1.5;
                double x = Math.cos(spinAngle + Math.PI / 2) * r;
                double z = Math.sin(spinAngle + Math.PI / 2) * r;
                verticalBlocks.get(i).entity().teleport(mid.clone().add(x, 0, z));
            }

            // Corner accents rotate
            double[][] baseCorners = {{1.5, 1.5}, {1.5, -1.5}, {-1.5, 1.5}, {-1.5, -1.5}};
            for (int i = 0; i < cornerAccents.size(); i++) {
                double bx = baseCorners[i][0], bz = baseCorners[i][1];
                double rx = Math.cos(spinAngle) * bx - Math.sin(spinAngle) * bz;
                double rz = Math.sin(spinAngle) * bx + Math.cos(spinAngle) * bz;
                cornerAccents.get(i).entity().teleport(mid.clone().add(rx, 0, rz));
            }

            // Tick-tock sounds at phase transitions
            if (ticksAlive % 40 == 0) {
                if (fastPhase) {
                    DisplayBuilder.playSound(mid, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.5f);
                } else {
                    DisplayBuilder.playSound(mid, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 0.5f);
                }
            }

            // Metallic tick every 20 ticks
            if (ticksAlive % 20 == 0) {
                DisplayBuilder.playSound(mid, Sound.BLOCK_CHAIN_PLACE, 0.5f, 1.0f);
            }

            // Dust particles at tips every 4 ticks
            if (ticksAlive % 4 == 0) {
                double tipR = 3.0;
                for (int a = 0; a < 4; a++) {
                    double tipAngle = spinAngle + a * (Math.PI / 2);
                    Location tip = mid.clone().add(
                            Math.cos(tipAngle) * tipR, 0, Math.sin(tipAngle) * tipR);
                    DisplayBuilder.dustParticles(tip, 1, 0.1, 180, 180, 190, 0.7f);
                }
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new RotatingChainCross(plugin);
        }
    }

    // ================================================================
    // 12. CHAIN TURBINE
    // Central cylinder (3 iron blocks) with 10 angled chain vanes.
    // Spins on vertical axis. Vanes redirect particle "air flow."
    // Industrial turbine sound. Expanding damage area.
    // ================================================================
    public static class ChainTurbine extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> cylinderBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> vaneBlocks = new ArrayList<>();

        public ChainTurbine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_turbine", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(69.0);
            config.setDamageRadius(10.0);
            config.setDurationTicks(220);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // Central cylinder: 3 iron blocks vertically stacked
            for (int y = 0; y < 3; y++) {
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(0, y + 0.5, 0), Material.IRON_BLOCK);
                h.scale(1.5f, 1.5f, 1.5f).glow(180, 180, 190).interpolation(2, 0);
                cylinderBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 10 angled chain vanes radiating from the cylinder
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 2.0;
                double z = Math.sin(angle) * 2.0;
                // Vanes alternate between two vertical levels with slight angle
                double y = (i % 2 == 0) ? 1.0 : 2.0;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), Material.CHAIN);
                h.scale(1.2f, 1.8f, 0.6f).glow(100, 100, 110).interpolation(2, 0);
                vaneBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 1.2f, 0.4f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin at 4 deg/tick = 0.06981 rad/tick
            double spinAngle = ticksAlive * 0.06981;

            // Expanding damage radius over time
            double expandedRadius = 2.0 + (ticksAlive / (double) config.getDurationTicks()) * 2.5;

            // Cylinder stays centered but wobbles slightly
            double wobble = Math.sin(ticksAlive * 0.1) * 0.05;
            for (int y = 0; y < cylinderBlocks.size(); y++) {
                cylinderBlocks.get(y).entity().teleport(
                        center.clone().add(wobble, y + 0.5, wobble));
            }

            // Vanes spin and expand outward
            for (int i = 0; i < vaneBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 10 + spinAngle;
                double x = Math.cos(angle) * expandedRadius;
                double z = Math.sin(angle) * expandedRadius;
                double y = (i % 2 == 0) ? 1.0 : 2.0;
                vaneBlocks.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Air flow particles redirected by vanes every 3 ticks
            if (ticksAlive % 3 == 0) {
                for (int i = 0; i < 5; i++) {
                    double pAngle = spinAngle + i * (Math.PI * 2 / 5);
                    double pR = expandedRadius + 0.5;
                    Location flowLoc = center.clone().add(
                            Math.cos(pAngle) * pR, 1.5, Math.sin(pAngle) * pR);
                    // Particles shoot outward
                    double vx = Math.cos(pAngle) * 0.15;
                    double vz = Math.sin(pAngle) * 0.15;
                    w.spawnParticle(Particle.CLOUD, flowLoc, 1, vx, 0.02, vz, 0.01);
                }
            }

            // Industrial turbine sound every 14 ticks
            if (ticksAlive % 14 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_PISTON_EXTEND, 0.8f, 1.5f);
                DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 0.5f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainTurbine(plugin);
        }
    }

    // ================================================================
    // 13. ORBITAL RINGS
    // 3 rings of chains at different orbit planes (XY, XZ, YZ), 6
    // chains each (18 total). Each ring rotates independently.
    // Atomic-model visual. Ethereal glow at crossing points.
    // ================================================================
    public static class OrbitalRings extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> ringXY = new ArrayList<>();
        private final List<BlockDisplayHandle> ringXZ = new ArrayList<>();
        private final List<BlockDisplayHandle> ringYZ = new ArrayList<>();

        public OrbitalRings(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("orbital_rings", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(66.0);
            config.setDamageRadius(9.0);
            config.setDurationTicks(260);
            config.setCooldownTicks(320);
            config.setTicksBetweenDamage(10);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location mid = center.clone().add(0, 2.5, 0);
            double radius = 3.0;

            // Ring XY plane (vertical ring facing Z): 6 chains
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * radius;
                double y = Math.sin(angle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(x, y, 0), Material.CHAIN);
                h.scale(1.05f, 1.05f, 1.05f).glow(180, 180, 190).interpolation(2, 0);
                ringXY.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring XZ plane (horizontal ring): 6 chains
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(x, 0, z), Material.DEEPSLATE);
                h.scale(1.05f, 1.05f, 1.05f).glow(100, 100, 110).interpolation(2, 0);
                ringXZ.add(h);
                spawnedEntities.add(h.entity());
            }

            // Ring YZ plane (vertical ring facing X): 6 chains
            for (int i = 0; i < 6; i++) {
                double angle = (2 * Math.PI * i) / 6;
                double y = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        mid.clone().add(0, y, z), Material.NETHERITE_BLOCK);
                h.scale(1.05f, 1.05f, 1.05f).glow(180, 100, 40).interpolation(2, 0);
                ringYZ.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 1.0f, 1.2f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location mid = center.clone().add(0, 2.5, 0);
            double radius = 3.0;

            // Each ring rotates at different speeds
            double angleXY = ticksAlive * 0.04;   // ~2.3 deg/tick
            double angleXZ = ticksAlive * 0.055;   // ~3.15 deg/tick
            double angleYZ = ticksAlive * 0.035;   // ~2 deg/tick

            // Ring XY (rotates within XY plane — spinning around Z axis)
            for (int i = 0; i < ringXY.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6 + angleXY;
                double x = Math.cos(baseAngle) * radius;
                double y = Math.sin(baseAngle) * radius;
                ringXY.get(i).entity().teleport(mid.clone().add(x, y, 0));
            }

            // Ring XZ (rotates within XZ plane — spinning around Y axis)
            for (int i = 0; i < ringXZ.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6 + angleXZ;
                double x = Math.cos(baseAngle) * radius;
                double z = Math.sin(baseAngle) * radius;
                ringXZ.get(i).entity().teleport(mid.clone().add(x, 0, z));
            }

            // Ring YZ (rotates within YZ plane — spinning around X axis)
            for (int i = 0; i < ringYZ.size(); i++) {
                double baseAngle = (2 * Math.PI * i) / 6 + angleYZ;
                double y = Math.cos(baseAngle) * radius;
                double z = Math.sin(baseAngle) * radius;
                ringYZ.get(i).entity().teleport(mid.clone().add(0, y, z));
            }

            // Ethereal glow at crossing points every 4 ticks
            if (ticksAlive % 4 == 0) {
                // Approximate crossing points: where rings would intersect
                // Top and bottom (+Y, -Y on all rings)
                Location top = mid.clone().add(0, radius, 0);
                Location bottom = mid.clone().add(0, -radius, 0);
                Location left = mid.clone().add(-radius, 0, 0);
                Location right = mid.clone().add(radius, 0, 0);
                Location front = mid.clone().add(0, 0, radius);
                Location back = mid.clone().add(0, 0, -radius);

                for (Location crossPt : new Location[]{top, bottom, left, right, front, back}) {
                    w.spawnParticle(Particle.END_ROD, crossPt, 2, 0.2, 0.2, 0.2, 0.01);
                }
            }

            // Ambient hum every 50 ticks
            if (ticksAlive % 50 == 0) {
                DisplayBuilder.playSound(mid, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 1.5f);
            }

            // Chain rattle every 15 ticks
            if (ticksAlive % 15 == 0) {
                DisplayBuilder.playSound(mid, Sound.BLOCK_CHAIN_PLACE, 0.4f, 1.2f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new OrbitalRings(plugin);
        }
    }

    // ================================================================
    // 14. CHAIN ROULETTE
    // Flat spinning disc (10 chains in a circle + 2 chains as "ball"
    // that rolls around edge). The ball moves around the rim as the
    // wheel spins. Random damage spot where ball is. Casino-click sounds.
    // ================================================================
    public static class ChainRoulette extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> wheelBlocks = new ArrayList<>();
        private final List<BlockDisplayHandle> ballBlocks = new ArrayList<>();
        private double ballAngle = 0;

        public ChainRoulette(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("chain_roulette", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(78.0);
            config.setDamageRadius(6.0);
            config.setDurationTicks(240);
            config.setCooldownTicks(300);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            Location table = center.clone().add(0, 0.5, 0);

            // 10 chains forming the wheel rim (radius 3.5)
            for (int i = 0; i < 10; i++) {
                double angle = (2 * Math.PI * i) / 10;
                double x = Math.cos(angle) * 3.5;
                double z = Math.sin(angle) * 3.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        table.clone().add(x, 0, z), Material.CHAIN);
                h.scale(1.2f, 1.2f, 1.2f).glow(100, 100, 110).interpolation(2, 0);
                wheelBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            // 2 chains as the "ball" (starts at angle 0 on the rim)
            for (int i = 0; i < 2; i++) {
                double offset = i * 0.5;
                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        table.clone().add(3.5, 0.3 + offset, 0), Material.NETHERITE_BLOCK);
                h.scale(0.75f, 0.75f, 0.75f).glow(255, 180, 80).interpolation(2, 0);
                ballBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 0.5f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            Location table = center.clone().add(0, 0.5, 0);

            // Wheel spins at 3 deg/tick
            double wheelAngle = ticksAlive * 0.05236;

            // Ball moves opposite direction at varying speed (decelerates)
            double ballSpeed = 0.12 - (ticksAlive / (double) config.getDurationTicks()) * 0.09;
            ballAngle += ballSpeed;

            // Wheel rotation
            for (int i = 0; i < wheelBlocks.size(); i++) {
                double angle = (2 * Math.PI * i) / 10 + wheelAngle;
                double x = Math.cos(angle) * 3.5;
                double z = Math.sin(angle) * 3.5;
                wheelBlocks.get(i).entity().teleport(table.clone().add(x, 0, z));
            }

            // Ball rolls along the rim
            double ballR = 3.5;
            for (int i = 0; i < ballBlocks.size(); i++) {
                double offset = i * 0.3;
                double bAngle = ballAngle + offset * 0.1;
                double x = Math.cos(bAngle) * ballR;
                double z = Math.sin(bAngle) * ballR;
                ballBlocks.get(i).entity().teleport(table.clone().add(x, 0.3, z));
            }

            // Apply extra damage at ball location
            if (ticksAlive % 8 == 0) {
                Location ballLoc = table.clone().add(
                        Math.cos(ballAngle) * ballR, 0.3, Math.sin(ballAngle) * ballR);
                triggerImpactDamage(ballLoc);
            }

            // Casino-click sounds every 6 ticks
            if (ticksAlive % 6 == 0) {
                DisplayBuilder.playSound(table, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f,
                        1.0f + (float) (Math.random() * 0.5));
            }

            // Ball glow trail every 2 ticks
            if (ticksAlive % 2 == 0) {
                Location ballTrail = table.clone().add(
                        Math.cos(ballAngle) * ballR, 0.3, Math.sin(ballAngle) * ballR);
                DisplayBuilder.dustParticles(ballTrail, 2, 0.1, 255, 180, 80, 1.0f);
            }

            // Periodic "spin" sound every 30 ticks
            if (ticksAlive % 30 == 0) {
                DisplayBuilder.playSound(table, Sound.BLOCK_CHAIN_PLACE, 0.7f, 1.6f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new ChainRoulette(plugin);
        }
    }

    // ================================================================
    // 15. MEAT GRINDER SPIRAL
    // Funnel-shaped spiral of 14 chains, wide at top (radius 4),
    // narrow at bottom (radius 1). Rotates around vertical axis.
    // Chains at bottom glow hot. Particles funnel downward.
    // Grinding sound increases in intensity.
    // ================================================================
    public static class MeatGrinderSpiral extends BlockDisplayAttack {

        private final List<BlockDisplayHandle> spiralBlocks = new ArrayList<>();

        public MeatGrinderSpiral(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("meat_grinder_spiral", AttackType.BLOCK_DISPLAY, 1, "modes/chain/attacks"));
            config.setDamage(75.0);
            config.setDamageRadius(7.0);
            config.setDurationTicks(200);
            config.setCooldownTicks(280);
            config.setTicksBetweenDamage(8);
        }

        @Override
        protected void onSpawn(Location center) {
            World w = center.getWorld();
            if (w == null) return;

            // 14 chains in a funnel spiral: top=wide (radius 4), bottom=narrow (radius 1)
            for (int i = 0; i < 14; i++) {
                double t = i / 13.0; // 0=top, 1=bottom
                double radius = 4.0 * (1.0 - t) + 1.0 * t;
                double y = (1.0 - t) * 5.0; // 5 blocks tall
                double spiralAngle = t * Math.PI * 6; // 3 full spirals
                double x = Math.cos(spiralAngle) * radius;
                double z = Math.sin(spiralAngle) * radius;

                Material mat;
                if (t > 0.7) {
                    mat = Material.NETHERITE_BLOCK; // bottom blocks glow hot
                } else if (t > 0.4) {
                    mat = Material.DEEPSLATE;
                } else {
                    mat = Material.CHAIN;
                }

                BlockDisplayHandle h = displayBuilder.spawnBlock(
                        center.clone().add(x, y, z), mat);
                h.scale(1.1f, 1.1f, 1.1f);

                // Bottom blocks glow hot (rust orange), top blocks iron gray
                if (t > 0.7) {
                    h.glow(255, 100, 20);
                } else {
                    h.glow(180, 180, 190);
                }

                h.interpolation(2, 0);
                spiralBlocks.add(h);
                spawnedEntities.add(h.entity());
            }

            DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE, 1.5f, 0.3f);
        }

        @Override
        protected void onTick(int ticksAlive) {
            Location center = getCenter();
            if (center == null) return;
            World w = center.getWorld();
            if (w == null) return;

            // Spin at 4 deg/tick = 0.06981 rad/tick, slightly faster at bottom
            double baseSpinAngle = ticksAlive * 0.06981;

            for (int i = 0; i < spiralBlocks.size(); i++) {
                double t = i / 13.0;
                double radius = 4.0 * (1.0 - t) + 1.0 * t;
                double y = (1.0 - t) * 5.0;

                // Bottom spins 2x faster than top
                double speedMult = 1.0 + t * 1.0;
                double spiralAngle = t * Math.PI * 6 + baseSpinAngle * speedMult;

                double x = Math.cos(spiralAngle) * radius;
                double z = Math.sin(spiralAngle) * radius;
                spiralBlocks.get(i).entity().teleport(center.clone().add(x, y, z));
            }

            // Particles funnel downward every 2 ticks
            if (ticksAlive % 2 == 0) {
                int funnelPoints = 8;
                for (int i = 0; i < funnelPoints; i++) {
                    double t = i / (double) funnelPoints;
                    double pRadius = 4.0 * (1.0 - t) + 1.0 * t;
                    double pY = (1.0 - t) * 5.0;
                    double pAngle = t * Math.PI * 6 + baseSpinAngle * (1.0 + t);
                    Location pLoc = center.clone().add(
                            Math.cos(pAngle) * pRadius, pY, Math.sin(pAngle) * pRadius);

                    if (t > 0.7) {
                        // Hot particles at bottom
                        DisplayBuilder.dustParticles(pLoc, 2, 0.1, 255, 100, 20, 1.0f);
                        w.spawnParticle(Particle.LAVA, pLoc, 1, 0.1, 0.1, 0.1, 0);
                    } else {
                        DisplayBuilder.dustParticles(pLoc, 1, 0.1, 180, 180, 190, 0.6f);
                    }
                }
            }

            // Grinding sound increases in pitch/volume over time
            if (ticksAlive % 12 == 0) {
                float intensity = 0.5f + (ticksAlive / (float) config.getDurationTicks()) * 1.0f;
                float pitch = 0.4f + (ticksAlive / (float) config.getDurationTicks()) * 0.8f;
                DisplayBuilder.playSound(center, Sound.BLOCK_GRINDSTONE_USE,
                        Math.min(intensity, 1.5f), pitch);
            }

            // Chain clinking at the output (bottom) every 8 ticks
            if (ticksAlive % 8 == 0) {
                DisplayBuilder.playSound(center, Sound.BLOCK_CHAIN_BREAK, 0.6f, 0.6f);
            }
        }

        @Override
        protected void onCleanup() {
            displayBuilder.removeAll();
        }

        @Override
        public AbstractAttack newInstance() {
            return new MeatGrinderSpiral(plugin);
        }
    }
}
