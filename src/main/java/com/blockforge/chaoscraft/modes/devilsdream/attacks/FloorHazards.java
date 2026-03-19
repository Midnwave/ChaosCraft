package com.blockforge.chaoscraft.modes.devilsdream.attacks;

import com.blockforge.chaoscraft.ChaosCraftPlugin;
import com.blockforge.chaoscraft.modes.calamity.attacks.*;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder;
import com.blockforge.chaoscraft.modes.calamity.display.DisplayBuilder.BlockDisplayHandle;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Devil's Dream — FLOOR HAZARD ENVIRONMENTAL ATTACKS
 * 13 ground-based hazards: lava cracks, soul sand patches, fire trails,
 * magma bubbles, and corrosive pools that spread across the floor.
 */
public class FloorHazards {

    private FloorHazards() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new LavaCracks(plugin));
        registry.register(new SoulSandPatch(plugin));
        registry.register(new FireTrail(plugin));
        registry.register(new MagmaBubble(plugin));
        registry.register(new BrimstoneFloor(plugin));
        registry.register(new CrumblingGround(plugin));
        registry.register(new NightmareQuicksand(plugin));
        registry.register(new HellfireGeyserLine(plugin));
        registry.register(new SoulFireRing(plugin));
        registry.register(new DarkIce(plugin));
        registry.register(new BurningFootprints(plugin));
        registry.register(new CorrosivePool(plugin));
        registry.register(new NetherFissure(plugin));
    }

    // 1. LAVA CRACKS — Glowing orange/red crack lines spread across ground
    public static class LavaCracks extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> cracks = new ArrayList<>();
        private int cracksRevealed = 0;
        public LavaCracks(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("lava_cracks", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < 12; i++) { double angle = Math.random()*Math.PI*2; double dist = 1+Math.random()*6;
                BlockDisplayHandle c2 = displayBuilder.spawnBlock(center.clone().add(Math.cos(angle)*dist,0.05,Math.sin(angle)*dist), Material.MAGMA_BLOCK);
                c2.scale(0.01f,0.01f,0.01f).glow(255,100,20).interpolation(8,0); cracks.add(c2); spawnedEntities.add(c2.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive % 8 == 0 && cracksRevealed < cracks.size()) {
                float len = 1.5f + (float)(Math.random()*2); float wid = 0.3f + (float)(Math.random()*0.3);
                cracks.get(cracksRevealed).animateTo(new org.joml.Vector3f(-len/2,-0.1f,-wid/2), new org.joml.AxisAngle4f((float)(Math.random()*Math.PI),0,1,0),
                        new org.joml.Vector3f(len,0.15f,wid), 10);
                Location crackLoc = cracks.get(cracksRevealed).entity().getLocation();
                w.spawnParticle(Particle.LAVA, crackLoc, 5, 0.5, 0.1, 0.5, 0);
                DisplayBuilder.playSound(crackLoc, Sound.BLOCK_BASALT_BREAK, 0.4f, 0.5f); cracksRevealed++; }
            if (ticksAlive % 4 == 0) for (int i = 0; i < Math.min(cracksRevealed, cracks.size()); i++) {
                Location cl = cracks.get(i).entity().getLocation();
                w.spawnParticle(Particle.FLAME, cl.clone().add(0,0.2,0), 2, 0.3, 0.1, 0.3, 0.02);
                if (Math.random()<0.2) w.spawnParticle(Particle.LAVA, cl, 1, 0.2, 0.05, 0.2, 0); }
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.4f, 0.6f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new LavaCracks(plugin); }
    }

    // 2. SOUL SAND PATCH — Expanding soul sand area that slows players
    public static class SoulSandPatch extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> patches = new ArrayList<>(); private float patchRadius = 1;
        public SoulSandPatch(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_sand_patch", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(5.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<10;i++) { double a=(2*Math.PI*i)/10; BlockDisplayHandle p = displayBuilder.spawnBlock(center.clone().add(Math.cos(a),0.05,Math.sin(a)), Material.SOUL_SAND);
                p.scale(1.0f,0.2f,1.0f).glow(70,100,120).interpolation(4,0); patches.add(p); spawnedEntities.add(p.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_PLACE, 0.8f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (patchRadius < 7) patchRadius += 0.02f;
            for (int i=0;i<patches.size();i++) { double a=(2*Math.PI*i)/patches.size();
                patches.get(i).entity().teleport(c.clone().add(Math.cos(a)*patchRadius*0.5,0.05,Math.sin(a)*patchRadius*0.5));
                patches.get(i).scale(patchRadius*0.3f,0.2f,patchRadius*0.3f); }
            // Slow players on the patch
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=patchRadius*patchRadius)
                p.setVelocity(p.getVelocity().multiply(0.6)); }
            if (ticksAlive%5==0) { double a=Math.random()*Math.PI*2; double d=Math.random()*patchRadius;
                w.spawnParticle(Particle.SOUL, c.clone().add(Math.cos(a)*d,0.3,Math.sin(a)*d), 2, 0.2, 0.1, 0.2, 0.02); }
            if (ticksAlive%30==0) DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_STEP, 0.4f, 0.4f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulSandPatch(plugin); }
    }

    // 3. FIRE TRAIL — Line of fire follows/chases the player
    public static class FireTrail extends EnvironmentalAttack {
        private final List<Location> trailPoints = new ArrayList<>();
        public FireTrail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("fire_trail", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(9.0); config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(8); config.setDurationTicks(350); config.setCooldownTicks(280);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%3==0) { trailPoints.add(c.clone()); if (trailPoints.size()>30) trailPoints.remove(0); }
            for (int i=0;i<trailPoints.size();i++) { Location tp = trailPoints.get(i); float age = (float)i/trailPoints.size();
                w.spawnParticle(Particle.FLAME, tp.clone().add(0,0.3,0), 3, 0.2, 0.3, 0.2, 0.02);
                if (age>0.5) w.spawnParticle(Particle.SMOKE, tp.clone().add(0,0.5,0), 1, 0.1, 0.2, 0.1, 0.01);
                if (i%3==0) DisplayBuilder.dustParticles(tp.clone().add(0,0.2,0), 2, 0.3, 255, 120, 20, 1.2f); }
            if (ticksAlive%10==0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f); }
        @Override protected void onCleanup() { super.onCleanup(); trailPoints.clear(); }
        @Override public AbstractAttack newInstance() { return new FireTrail(plugin); }
    }

    // 4. MAGMA BUBBLE — Large magma bubbles form on ground and pop with splash damage
    public static class MagmaBubble extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> bubbles = new ArrayList<>();
        private final List<Float> bubbleSizes = new ArrayList<>(); private final List<Float> growRates = new ArrayList<>();
        private final List<Double> bx = new ArrayList<>(), bz = new ArrayList<>();
        public MagmaBubble(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("magma_bubble", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(10.0); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(30); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<8;i++) { bx.add(Math.random()*10-5); bz.add(Math.random()*10-5); bubbleSizes.add(0.1f); growRates.add(0.02f+(float)(Math.random()*0.03));
                BlockDisplayHandle b = displayBuilder.spawnBlock(center.clone().add(bx.get(i),0.1,bz.get(i)), Material.MAGMA_BLOCK);
                b.scale(0.1f,0.1f,0.1f).glow(255,140,30).interpolation(3,0); bubbles.add(b); spawnedEntities.add(b.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_AMBIENT, 0.8f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i=0;i<bubbles.size();i++) { float size = bubbleSizes.get(i) + growRates.get(i);
                if (size > 2.5f) { // Pop!
                    size = 0.1f; Location popLoc = c.clone().add(bx.get(i),0.5,bz.get(i));
                    w.spawnParticle(Particle.LAVA, popLoc, 10, 1, 0.5, 1, 0); w.spawnParticle(Particle.FLAME, popLoc, 8, 0.5, 0.5, 0.5, 0.05);
                    DisplayBuilder.playSound(popLoc, Sound.BLOCK_LAVA_POP, 0.6f, 0.6f);
                    bx.set(i, Math.random()*10-5); bz.set(i, Math.random()*10-5); }
                bubbleSizes.set(i, size);
                bubbles.get(i).entity().teleport(c.clone().add(bx.get(i), size*0.3, bz.get(i)));
                bubbles.get(i).scale(size, size*0.8f, size); }
            if (ticksAlive%6==0) for (int i=0;i<3;i++) w.spawnParticle(Particle.DRIPPING_LAVA,
                    c.clone().add(Math.random()*10-5,0.5,Math.random()*10-5), 1, 0.1, 0.05, 0.1, 0);
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.3f, 0.6f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new MagmaBubble(plugin); }
    }

    // 5. BRIMSTONE FLOOR — Ground slowly turns to hot brimstone, expanding circle of red particles
    public static class BrimstoneFloor extends EnvironmentalAttack {
        private float radius = 1;
        public BrimstoneFloor(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_floor", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(7.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (radius < 10) radius += 0.03f;
            if (ticksAlive%3==0) { DisplayBuilder.particleRing(c.clone().add(0,0.2,0), radius, Particle.FLAME, (int)(radius*4), null);
                DisplayBuilder.particleRing(c.clone().add(0,0.1,0), radius*0.7, Particle.DUST, (int)(radius*3),
                        new Particle.DustOptions(Color.fromRGB(200,60,20), 1.5f)); }
            if (ticksAlive%2==0) for (int i=0;i<(int)(radius*2);i++) { double a=Math.random()*Math.PI*2; double d=Math.random()*radius;
                w.spawnParticle(Particle.FLAME, c.clone().add(Math.cos(a)*d,0.3,Math.sin(a)*d), 1, 0.1, 0.2, 0.1, 0.01); }
            if (ticksAlive%25==0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_AMBIENT, 0.5f, 0.5f); }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneFloor(plugin); }
    }

    // 6. CRUMBLING GROUND — Blocks under player "crack" with particles
    public static class CrumblingGround extends EnvironmentalAttack {
        public CrumblingGround(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crumbling_ground", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(6.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(350); config.setCooldownTicks(280);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%5==0) { for (int i=0;i<6;i++) { double a=Math.random()*Math.PI*2; double d=Math.random()*5;
                Location crack = c.clone().add(Math.cos(a)*d,0.1,Math.sin(a)*d);
                w.spawnParticle(Particle.BLOCK, crack, 8, 0.3, 0.1, 0.3, 0.02, Material.DEEPSLATE.createBlockData());
                w.spawnParticle(Particle.SMOKE, crack, 3, 0.2, 0.1, 0.2, 0.01); } }
            if (ticksAlive%10==0) DisplayBuilder.playSound(c, Sound.BLOCK_DEEPSLATE_BREAK, 0.4f, 0.5f);
            if (ticksAlive%3==0) { DisplayBuilder.dustParticles(c.clone().add(Math.random()*8-4,0.2,Math.random()*8-4), 3, 0.5, 80, 70, 60, 1.5f); } }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrumblingGround(plugin); }
    }

    // 7. NIGHTMARE QUICKSAND — Pulls players downward with soul sand effects
    public static class NightmareQuicksand extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> sandBlocks = new ArrayList<>(); private float qRadius = 3;
        public NightmareQuicksand(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_quicksand", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(5.0);
            config.setTicksBetweenDamage(10); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<8;i++) { double a=(2*Math.PI*i)/8; BlockDisplayHandle s = displayBuilder.spawnBlock(center.clone().add(Math.cos(a)*2,0.05,Math.sin(a)*2), Material.SOUL_SAND);
                s.scale(1.5f,0.15f,1.5f).glow(100,80,60).interpolation(3,0); sandBlocks.add(s); spawnedEntities.add(s.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (qRadius < 6) qRadius += 0.015f;
            for (int i=0;i<sandBlocks.size();i++) { double a=(2*Math.PI*i)/sandBlocks.size();
                sandBlocks.get(i).entity().teleport(c.clone().add(Math.cos(a)*qRadius*0.4,0.05,Math.sin(a)*qRadius*0.4));
                sandBlocks.get(i).scale(qRadius*0.4f,0.15f,qRadius*0.4f); }
            // Pull players down
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=qRadius*qRadius)
                p.setVelocity(p.getVelocity().add(new Vector(0,-0.15,0))); }
            if (ticksAlive%3==0) { double a=Math.random()*Math.PI*2; double d=Math.random()*qRadius;
                w.spawnParticle(Particle.SMOKE, c.clone().add(Math.cos(a)*d,0.2,Math.sin(a)*d), 3, 0.2, 0.05, 0.2, 0.01);
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,0.15,Math.sin(a)*d), 2, 0.2, 100, 80, 60, 1.5f); }
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_STEP, 0.4f, 0.3f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareQuicksand(plugin); }
    }

    // 8. HELLFIRE GEYSER LINE — Sequential fire geysers in a line toward player
    public static class HellfireGeyserLine extends EnvironmentalAttack {
        private int activeGeyser = 0;
        public HellfireGeyserLine(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hellfire_geyser_line", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(10.0); config.setDamageRadius(4.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(300); config.setCooldownTicks(280);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%15==0 && activeGeyser<8) {
                double dist = activeGeyser * 2.0; Location gLoc = c.clone().add(0,0,dist-8);
                w.spawnParticle(Particle.FLAME, gLoc, 20, 0.3, 2.0, 0.3, 0.05);
                w.spawnParticle(Particle.SMOKE, gLoc, 10, 0.5, 1.5, 0.5, 0.03);
                w.spawnParticle(Particle.LAVA, gLoc, 5, 0.3, 0.3, 0.3, 0);
                DisplayBuilder.playSound(gLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.5f); activeGeyser++; }
            // Active geyser trails
            for (int i=0;i<activeGeyser;i++) { double dist=i*2.0; Location gLoc=c.clone().add(0,0,dist-8);
                if (ticksAlive%4==0) w.spawnParticle(Particle.FLAME, gLoc, 3, 0.2, 0.5, 0.2, 0.02); } }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellfireGeyserLine(plugin); }
    }

    // 9. SOUL FIRE RING — Expanding ring of soul fire particles
    public static class SoulFireRing extends EnvironmentalAttack {
        private float ringRadius = 1;
        public SoulFireRing(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_fire_ring", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_SOUL_SAND_BREAK, 0.8f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ringRadius < 8) ringRadius += 0.04f;
            if (ticksAlive%2==0) { int points = (int)(ringRadius*6);
                for (int i=0;i<points;i++) { double a=(2*Math.PI*i)/points;
                    Location p = c.clone().add(Math.cos(a)*ringRadius,0.3,Math.sin(a)*ringRadius);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.05, 0.2, 0.05, 0.01);
                    if (i%3==0) w.spawnParticle(Particle.SOUL, p, 1, 0.1, 0.1, 0.1, 0.01); } }
            // Inner glow
            if (ticksAlive%4==0) DisplayBuilder.particleRing(c.clone().add(0,0.15,0), ringRadius*0.8,
                    Particle.DUST, (int)(ringRadius*3), new Particle.DustOptions(Color.fromRGB(80,180,200), 1.2f));
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_SOUL_SAND_PLACE, 0.4f, 0.5f); }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulFireRing(plugin); }
    }

    // 10. DARK ICE — Spreading dark ice patches with frost particles
    public static class DarkIce extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> icePatches = new ArrayList<>(); private float iceRadius = 1;
        public DarkIce(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dark_ice", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(6.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<10;i++) { BlockDisplayHandle ic = displayBuilder.spawnBlock(center.clone().add(Math.random()*2-1,0.05,Math.random()*2-1), Material.BLUE_ICE);
                ic.scale(0.5f,0.1f,0.5f).glow(60,80,120).interpolation(5,0); icePatches.add(ic); spawnedEntities.add(ic.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_GLASS_BREAK, 0.6f, 1.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (iceRadius < 7) iceRadius += 0.02f;
            for (int i=0;i<icePatches.size();i++) { double a=(2*Math.PI*i)/icePatches.size(); double d=iceRadius*(0.3+0.7*((i%3+1)/3.0));
                icePatches.get(i).entity().teleport(c.clone().add(Math.cos(a)*d,0.05,Math.sin(a)*d));
                icePatches.get(i).scale(iceRadius*0.25f,0.1f,iceRadius*0.25f); }
            if (ticksAlive%3==0) for (int i=0;i<5;i++) w.spawnParticle(Particle.SNOWFLAKE,
                    c.clone().add(Math.random()*iceRadius*2-iceRadius,0.3,Math.random()*iceRadius*2-iceRadius), 2, 0.2, 0.1, 0.2, 0.01);
            if (ticksAlive%4==0) DisplayBuilder.dustParticles(c.clone().add(Math.random()*iceRadius*2-iceRadius,0.2,Math.random()*iceRadius*2-iceRadius), 2, 0.3, 60, 80, 140, 1.0f);
            if (ticksAlive%25==0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_PLACE, 0.3f, 1.5f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new DarkIce(plugin); }
    }

    // 11. BURNING FOOTPRINTS — Fire particles appear where players walk
    public static class BurningFootprints extends EnvironmentalAttack {
        private final List<Location> footprints = new ArrayList<>();
        public BurningFootprints(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("burning_footprints", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(7.0); config.setDamageRadius(3.0);
            config.setTicksBetweenDamage(8); config.setDurationTicks(350); config.setCooldownTicks(280);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.5f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%4==0) { footprints.add(c.clone()); if (footprints.size()>40) footprints.remove(0); }
            for (Location fp : footprints) { w.spawnParticle(Particle.FLAME, fp.clone().add(0,0.15,0), 2, 0.15, 0.1, 0.15, 0.01);
                if (Math.random()<0.1) w.spawnParticle(Particle.SMOKE, fp.clone().add(0,0.3,0), 1, 0.1, 0.2, 0.1, 0.005); }
            if (ticksAlive%15==0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.3f, 0.6f); }
        @Override protected void onCleanup() { super.onCleanup(); footprints.clear(); }
        @Override public AbstractAttack newInstance() { return new BurningFootprints(plugin); }
    }

    // 12. CORROSIVE POOL — Expanding green/yellow toxic pool
    public static class CorrosivePool extends EnvironmentalAttack {
        private float poolR = 1;
        public CorrosivePool(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("corrosive_pool", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(10); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_LAVA_EXTINGUISH, 0.8f, 0.8f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (poolR < 8) poolR += 0.025f;
            if (ticksAlive%2==0) { int pts=(int)(poolR*5); for (int i=0;i<pts;i++) { double a=Math.random()*Math.PI*2; double d=Math.random()*poolR;
                Location p = c.clone().add(Math.cos(a)*d,0.15,Math.sin(a)*d);
                DisplayBuilder.dustParticles(p, 1, 0.2, 100, 200, 30, 1.5f); } }
            if (ticksAlive%4==0) { DisplayBuilder.particleRing(c.clone().add(0,0.1,0), poolR, Particle.DUST, (int)(poolR*4),
                    new Particle.DustOptions(Color.fromRGB(80,180,20), 1.2f)); }
            // Bubbling
            if (ticksAlive%6==0) for (int i=0;i<3;i++) { double a=Math.random()*Math.PI*2; double d=Math.random()*poolR;
                w.spawnParticle(Particle.BUBBLE_POP, c.clone().add(Math.cos(a)*d,0.2,Math.sin(a)*d), 2, 0.1, 0.1, 0.1, 0.02); }
            if (ticksAlive%25==0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.3f, 1.2f); }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CorrosivePool(plugin); }
    }

    // 13. NETHER FISSURE — Jagged crack opens, nether particles pour out
    public static class NetherFissure extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> fissureBlocks = new ArrayList<>(); private float fissureLen = 0;
        public NetherFissure(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nether_fissure", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(9.0); config.setDamageRadius(6.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<10;i++) { BlockDisplayHandle f = displayBuilder.spawnBlock(center.clone().add(0,0.05,i-5), Material.NETHERRACK);
                f.scale(0.01f,0.01f,0.01f).glow(200,60,30).interpolation(6,0); fissureBlocks.add(f); spawnedEntities.add(f.entity()); }
            DisplayBuilder.playSound(center, Sound.BLOCK_BASALT_BREAK, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%6==0 && fissureLen < fissureBlocks.size()) {
                int idx = (int)fissureLen; float wid = 0.4f+(float)(Math.random()*0.4);
                fissureBlocks.get(idx).animateTo(new org.joml.Vector3f(-wid/2,-0.1f,-0.5f), new org.joml.AxisAngle4f(0,0,1,0),
                        new org.joml.Vector3f(wid,0.2f,1.0f), 8);
                DisplayBuilder.playSound(fissureBlocks.get(idx).entity().getLocation(), Sound.BLOCK_BASALT_BREAK, 0.3f, 0.5f);
                fissureLen++; }
            // Nether particles from crack
            for (int i=0;i<(int)fissureLen;i++) { if (ticksAlive%3==0) { Location fl = fissureBlocks.get(i).entity().getLocation();
                w.spawnParticle(Particle.FLAME, fl.clone().add(0,0.3,0), 2, 0.2, 0.3, 0.2, 0.02);
                w.spawnParticle(Particle.SMOKE, fl.clone().add(0,0.5,0), 1, 0.1, 0.2, 0.1, 0.01);
                if (Math.random()<0.2) w.spawnParticle(Particle.LAVA, fl, 1, 0.1, 0.05, 0.1, 0); } }
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_LAVA_POP, 0.4f, 0.5f); }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NetherFissure(plugin); }
    }
}
