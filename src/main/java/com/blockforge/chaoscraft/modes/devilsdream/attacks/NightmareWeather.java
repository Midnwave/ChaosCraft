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
 * Devil's Dream — NIGHTMARE WEATHER ENVIRONMENTAL ATTACKS
 * 13 demonic weather effects with heavy particle usage and atmospheric sounds.
 */
public class NightmareWeather {

    private NightmareWeather() {}

    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new BloodStorm(plugin));
        registry.register(new SoulHail(plugin));
        registry.register(new BrimstoneWind(plugin));
        registry.register(new NightFog(plugin));
        registry.register(new EmberShower(plugin));
        registry.register(new AshCloud(plugin));
        registry.register(new ThunderScream(plugin));
        registry.register(new HellWind(plugin));
        registry.register(new SoulBlizzard(plugin));
        registry.register(new CrimsonDew(plugin));
        registry.register(new VolcanicAsh(plugin));
        registry.register(new NightmareTornado(plugin));
        registry.register(new DreamStorm(plugin));
    }

    // 1. BLOOD STORM
    public static class BloodStorm extends EnvironmentalAttack {
        public BloodStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("blood_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) {
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.3f);
        }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i = 0; i < 30; i++) {
                double x = c.getX() + Math.random() * 24 - 12, z = c.getZ() + Math.random() * 24 - 12;
                w.spawnParticle(Particle.DUST, new Location(w, x, c.getY() + 10 + Math.random() * 5, z), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(150, 15, 15), 1.8f));
            }
            if (ticksAlive % 3 == 0) for (int i = 0; i < 8; i++) {
                w.spawnParticle(Particle.DUST, new Location(w, c.getX() + Math.random() * 20 - 10, c.getY() + 0.2, c.getZ() + Math.random() * 20 - 10),
                        3, 0.3, 0.05, 0.3, 0, new Particle.DustOptions(Color.fromRGB(120, 10, 10), 2.0f));
            }
            if (ticksAlive % 2 == 0) { double sweepZ = c.getZ() + Math.sin(ticksAlive * 0.08) * 8;
                for (int x = -8; x <= 8; x += 2) for (double y = 0; y < 6; y += 1.5)
                    w.spawnParticle(Particle.DUST, new Location(w, c.getX() + x, c.getY() + y, sweepZ), 1, 0.5, 0.3, 0.1, 0,
                            new Particle.DustOptions(Color.fromRGB(180, 20, 20), 1.5f));
            }
            if (ticksAlive % 60 == 0) { DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.3f);
                w.spawnParticle(Particle.END_ROD, c.clone().add(0, 12, 0), 30, 8, 2, 8, 0.01); }
            if (ticksAlive % 20 == 0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN_ABOVE, 0.6f, 0.3f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BloodStorm(plugin); }
    }

    // 2. SOUL HAIL
    public static class SoulHail extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> hail = new ArrayList<>();
        private final List<Float> hailY = new ArrayList<>(); private final List<Double> hailX = new ArrayList<>(), hailZ = new ArrayList<>();
        public SoulHail(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_hail", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(7.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) {
            World w = center.getWorld(); if (w == null) return;
            for (int i = 0; i < 15; i++) { hailX.add(Math.random()*16-8); hailZ.add(Math.random()*16-8); hailY.add(12f+(float)(Math.random()*6));
                BlockDisplayHandle h = displayBuilder.spawnBlock(center, Material.BLUE_ICE); h.scale(0.3f,0.3f,0.3f).glow(80,180,220).interpolation(1,0);
                hail.add(h); spawnedEntities.add(h.entity()); }
            DisplayBuilder.playSound(center, Sound.WEATHER_RAIN, 0.8f, 1.5f);
        }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i = 0; i < hail.size(); i++) { float y = hailY.get(i) - 0.5f;
                if (y <= 0) { y = 14f; hailX.set(i, Math.random()*16-8); hailZ.set(i, Math.random()*16-8);
                    Location imp = c.clone().add(hailX.get(i), 0.2, hailZ.get(i));
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME, imp, 5, 0.3, 0.1, 0.3, 0.02); w.spawnParticle(Particle.SNOWFLAKE, imp, 3, 0.2, 0.1, 0.2, 0.01); }
                hailY.set(i, y); hail.get(i).entity().teleport(c.clone().add(hailX.get(i), y, hailZ.get(i))); hail.get(i).rotate(ticksAlive*0.15f,1,0,1); }
            if (ticksAlive % 3 == 0) for (int i = 0; i < 5; i++)
                w.spawnParticle(Particle.SOUL, c.clone().add(Math.random()*16-8, Math.random()*8, Math.random()*16-8), 1, 0.1, 0.1, 0.1, 0.02);
            if (ticksAlive % 15 == 0) DisplayBuilder.playSound(c, Sound.BLOCK_GLASS_BREAK, 0.3f, 1.5f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new SoulHail(plugin); }
    }

    // 3. BRIMSTONE WIND
    public static class BrimstoneWind extends EnvironmentalAttack {
        private double windAngle = 0;
        public BrimstoneWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("brimstone_wind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(6.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { windAngle = Math.random()*Math.PI*2; DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive % 80 == 0) { windAngle += Math.PI*0.5+Math.random()*Math.PI; DisplayBuilder.playSound(c, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 0.4f); }
            double wx = Math.cos(windAngle), wz = Math.sin(windAngle);
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=100) p.setVelocity(p.getVelocity().add(new Vector(wx*0.15,0,wz*0.15))); }
            for (int i = 0; i < 15; i++) { double x = c.getX()-wx*10+(Math.random()*20-10)*wz, z = c.getZ()-wz*10+(Math.random()*20-10)*wx, y = c.getY()+Math.random()*4;
                w.spawnParticle(Particle.FLAME, new Location(w,x,y,z), 1, wx*0.5, 0.1, wz*0.5, 0.05);
                w.spawnParticle(Particle.SMOKE, new Location(w,x,y,z), 1, wx*0.3, 0.05, wz*0.3, 0.03); }
            if (ticksAlive%2==0) for (int i=0;i<8;i++) w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, c.clone().add(Math.random()*16-8,0.3,Math.random()*16-8),1,wx*0.3,0.05,wz*0.3,0.02);
            if (ticksAlive%10==0) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.4f, 0.4f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new BrimstoneWind(plugin); }
    }

    // 4. NIGHT FOG
    public static class NightFog extends EnvironmentalAttack {
        private float fogDensity = 0;
        public NightFog(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("night_fog", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(5.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(25); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.AMBIENT_CAVE, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (fogDensity < 1.0f) fogDensity += 0.015f;
            for (int i = 0; i < (int)(fogDensity * 40); i++) {
                double x = c.getX()+Math.random()*24-12, z = c.getZ()+Math.random()*24-12;
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, new Location(w,x,c.getY()+Math.random()*2.5,z), 1, 0.5, 0.2, 0.5, 0.003); }
            if (ticksAlive%2==0) for (int i=0;i<(int)(fogDensity*15);i++)
                DisplayBuilder.dustParticles(new Location(w,c.getX()+Math.random()*20-10,c.getY()+Math.random()*3,c.getZ()+Math.random()*20-10), 1, 0.3, 20, 15, 30, 2.5f);
            if (ticksAlive%(30+(int)(Math.random()*30))==0) { Sound[] s={Sound.ENTITY_VEX_AMBIENT,Sound.ENTITY_PHANTOM_AMBIENT,Sound.AMBIENT_CAVE};
                DisplayBuilder.playSound(c.clone().add(Math.random()*16-8,1,Math.random()*16-8), s[(int)(Math.random()*s.length)], 0.3f, 0.3f); }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new NightFog(plugin); }
    }

    // 5. EMBER SHOWER
    public static class EmberShower extends EnvironmentalAttack {
        public EmberShower(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ember_shower", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(7.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(350); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.4f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i = 0; i < 20; i++) { double x=c.getX()+Math.random()*20-10, z=c.getZ()+Math.random()*20-10, y=c.getY()+8+Math.random()*6;
                w.spawnParticle(Particle.FLAME, new Location(w,x,y,z), 1, 0.1, -0.5, 0.1, 0.02);
                if (Math.random()<0.3) w.spawnParticle(Particle.LAVA, new Location(w,x,y,z), 1, 0, 0, 0, 0); }
            if (ticksAlive%3==0) for (int i=0;i<5;i++) w.spawnParticle(Particle.ELECTRIC_SPARK,
                    new Location(w, c.getX()+Math.random()*16-8, c.getY()+0.2, c.getZ()+Math.random()*16-8), 3, 0.2, 0.1, 0.2, 0.05);
            if (ticksAlive%50==0) { w.spawnParticle(Particle.END_ROD, c.clone().add(0,6,0), 40, 8, 3, 8, 0.02); DisplayBuilder.playSound(c, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 0.3f); }
            if (ticksAlive%2==0) for (int i=0;i<10;i++) DisplayBuilder.dustParticles(
                    new Location(w, c.getX()+Math.random()*18-9, c.getY()+3+Math.random()*4, c.getZ()+Math.random()*18-9), 1, 0.2, 255, 140, 30, 1.0f);
            if (ticksAlive%15==0) DisplayBuilder.playSound(c, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 0.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new EmberShower(plugin); }
    }

    // 6. ASH CLOUD
    public static class AshCloud extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> ashChunks = new ArrayList<>();
        public AshCloud(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("ash_cloud", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(5.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            for (int i=0;i<10;i++) { BlockDisplayHandle a = displayBuilder.spawnBlock(center.clone().add(Math.random()*16-8,2+Math.random()*6,Math.random()*16-8), Material.GRAY_CONCRETE);
                a.scale(0.3f+(float)(Math.random()*0.4),0.2f,0.3f+(float)(Math.random()*0.4)).glow(100,100,100).interpolation(3,0);
                ashChunks.add(a); spawnedEntities.add(a.entity()); }
            DisplayBuilder.playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i=0;i<25;i++) { double x=c.getX()+Math.random()*24-12, z=c.getZ()+Math.random()*24-12;
                w.spawnParticle(Particle.ASH, new Location(w,x,c.getY()+Math.random()*8,z), 3, 0.5, 0.5, 0.5, 0);
                if (Math.random()<0.2) DisplayBuilder.dustParticles(new Location(w,x,c.getY()+Math.random()*8,z), 1, 0.3, 120, 120, 110, 1.2f); }
            for (int i=0;i<ashChunks.size();i++) { Location l=ashChunks.get(i).entity().getLocation();
                ashChunks.get(i).entity().teleport(l.add(Math.sin(ticksAlive*0.02+i*1.2)*0.05, (float)Math.sin(ticksAlive*0.03+i*0.8)*0.03, Math.sin(ticksAlive*0.02+i)*0.025)); }
            if (ticksAlive%4==0) for (int i=0;i<8;i++) w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,
                    c.clone().add(Math.random()*20-10,0.5,Math.random()*20-10), 1, 0.5, 0.1, 0.5, 0.003);
            if (ticksAlive%25==0) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.3f, 0.3f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new AshCloud(plugin); }
    }

    // 7. THUNDER SCREAM
    public static class ThunderScream extends EnvironmentalAttack {
        public ThunderScream(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("thunder_scream", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(12.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(40); config.setDurationTicks(300); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (ticksAlive%40==0) {
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=100)
                    p.setVelocity(p.getVelocity().add(new Vector((Math.random()-0.5)*0.15,0.05,(Math.random()-0.5)*0.15))); }
                Location bolt = c.clone().add(Math.random()*8-4,15,Math.random()*8-4);
                DisplayBuilder.particleLine(bolt, bolt.clone().add(0,-15,0), Particle.END_ROD, 6, null);
                w.spawnParticle(Particle.END_ROD, c.clone().add(0,5,0), 50, 10, 5, 10, 0.01);
                Sound[] t = {Sound.ENTITY_LIGHTNING_BOLT_THUNDER, Sound.ENTITY_WITHER_BREAK_BLOCK, Sound.ENTITY_GENERIC_EXPLODE};
                DisplayBuilder.playSound(c, t[(ticksAlive/40)%3], 1.0f, 0.3f+(float)(Math.random()*0.3)); }
            if (ticksAlive%8==0) w.spawnParticle(Particle.SMOKE, c.clone().add(Math.random()*16-8,8,Math.random()*16-8), 3, 1, 0.5, 1, 0.01);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new ThunderScream(plugin); }
    }

    // 8. HELL WIND
    public static class HellWind extends EnvironmentalAttack {
        private double windDir = 0;
        public HellWind(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("hell_wind", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(8.0); config.setDamageRadius(10.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(300); config.setCooldownTicks(280);
        }
        @Override protected void onSpawn(Location center) { windDir = Math.random()*Math.PI*2; DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            windDir += 0.003; double wx=Math.cos(windDir), wz=Math.sin(windDir);
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=100) p.setVelocity(p.getVelocity().add(new Vector(wx*0.25,0.05,wz*0.25))); }
            for (int i=0;i<25;i++) { double x=c.getX()-wx*12+(Math.random()*20-10)*wz, z=c.getZ()-wz*12+(Math.random()*20-10)*wx, y=c.getY()+Math.random()*4;
                w.spawnParticle(Particle.FLAME, new Location(w,x,y,z), 1, wx*0.8, 0.1, wz*0.8, 0.08); }
            if (ticksAlive%2==0) for (int i=0;i<10;i++) DisplayBuilder.dustParticles(
                    new Location(w, c.getX()+Math.random()*18-9, c.getY()+Math.random()*3, c.getZ()+Math.random()*18-9), 1, 0.2, 180, 60, 40, 1.5f);
            if (ticksAlive%12==0) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.6f, 0.4f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new HellWind(plugin); }
    }

    // 9. SOUL BLIZZARD
    public static class SoulBlizzard extends EnvironmentalAttack {
        public SoulBlizzard(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("soul_blizzard", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(7.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(15); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_POLAR_BEAR_WARNING, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            double wa = ticksAlive*0.01, wx=Math.cos(wa), wz=Math.sin(wa);
            for (int i=0;i<30;i++) { double x=c.getX()+Math.random()*24-12, z=c.getZ()+Math.random()*24-12, y=c.getY()+Math.random()*8;
                w.spawnParticle(Particle.SNOWFLAKE, new Location(w,x,y,z), 1, wx*0.3, 0.1, wz*0.3, 0.05);
                if (Math.random()<0.3) w.spawnParticle(Particle.SOUL_FIRE_FLAME, new Location(w,x,y,z), 1, wx*0.2, 0.05, wz*0.2, 0.02); }
            if (ticksAlive%2==0) for (int i=0;i<12;i++) DisplayBuilder.dustParticles(
                    new Location(w, c.getX()+Math.random()*20-10, c.getY()+Math.random()*6, c.getZ()+Math.random()*20-10), 1, 0.2, 80, 200, 240, 1.3f);
            if (ticksAlive%6==0) w.spawnParticle(Particle.SOUL, c.clone().add(Math.random()*16-8,1+Math.random()*4,Math.random()*16-8), 2, 0.3, 0.3, 0.3, 0.02);
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(c)<=144) p.setVelocity(p.getVelocity().add(new Vector(wx*0.08,0,wz*0.08))); }
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.WEATHER_RAIN, 0.4f, 1.5f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new SoulBlizzard(plugin); }
    }

    // 10. CRIMSON DEW
    public static class CrimsonDew extends EnvironmentalAttack {
        public CrimsonDew(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("crimson_dew", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(5.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(20); config.setDurationTicks(400); config.setCooldownTicks(300);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.BLOCK_NETHER_SPROUTS_BREAK, 0.8f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i=0;i<20;i++) { double x=c.getX()+Math.random()*20-10, z=c.getZ()+Math.random()*20-10;
                DisplayBuilder.dustParticles(new Location(w,x,c.getY()+Math.random()*0.5,z), 2, 0.3, 180, 30, 30, 1.8f);
                w.spawnParticle(Particle.CRIMSON_SPORE, new Location(w,x,c.getY()+Math.random()*3,z), 1, 0.3, 0.5, 0.3, 0.01); }
            if (ticksAlive%3==0) for (int i=0;i<6;i++) w.spawnParticle(Particle.DUST,
                    new Location(w,c.getX()+Math.random()*16-8,c.getY()+0.1,c.getZ()+Math.random()*16-8), 1, 0, 0.3, 0, 0.02,
                    new Particle.DustOptions(Color.fromRGB(200, 40, 40), 0.8f));
            if (ticksAlive%30==0) DisplayBuilder.playSound(c, Sound.BLOCK_VINE_STEP, 0.3f, 0.3f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new CrimsonDew(plugin); }
    }

    // 11. VOLCANIC ASH
    public static class VolcanicAsh extends EnvironmentalAttack {
        public VolcanicAsh(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("volcanic_ash", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(6.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(18); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            for (int i=0;i<35;i++) { double x=c.getX()+Math.random()*24-12, z=c.getZ()+Math.random()*24-12, y=c.getY()+8+Math.random()*6;
                w.spawnParticle(Particle.ASH, new Location(w,x,y,z), 2, 0.5, 0.3, 0.5, 0);
                DisplayBuilder.dustParticles(new Location(w,x,y,z), 1, 0.3, 60, 55, 50, 1.5f); }
            if (ticksAlive%4==0) for (int i=0;i<3;i++) w.spawnParticle(Particle.LAVA,
                    new Location(w,c.getX()+Math.random()*20-10,c.getY()+5+Math.random()*8,c.getZ()+Math.random()*20-10), 1, 0, 0, 0, 0);
            if (ticksAlive%5==0) for (int i=0;i<8;i++) DisplayBuilder.dustParticles(
                    new Location(w,c.getX()+Math.random()*18-9,c.getY()+0.15,c.getZ()+Math.random()*18-9), 2, 0.4, 80, 75, 70, 2.0f);
            if (ticksAlive%20==0) DisplayBuilder.playSound(c, Sound.BLOCK_SAND_FALL, 0.4f, 0.3f);
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new VolcanicAsh(plugin); }
    }

    // 12. NIGHTMARE TORNADO
    public static class NightmareTornado extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> debris = new ArrayList<>(); private float intensity = 0;
        public NightmareTornado(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("nightmare_tornado", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(10.0); config.setDamageRadius(8.0);
            config.setTicksBetweenDamage(10); config.setDurationTicks(400); config.setCooldownTicks(350);
        }
        @Override protected void onSpawn(Location center) { World w = center.getWorld(); if (w == null) return;
            Material[] m = {Material.SOUL_SAND,Material.NETHERRACK,Material.BLACKSTONE,Material.COAL_BLOCK};
            for (int i=0;i<8;i++) { BlockDisplayHandle d = displayBuilder.spawnBlock(center, m[i%4]);
                d.scale(0.5f+(float)(Math.random()*0.5),0.5f,0.5f+(float)(Math.random()*0.5)).glow(40,20,50).interpolation(2,0);
                debris.add(d); spawnedEntities.add(d.entity()); }
            DisplayBuilder.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            if (intensity < 1.0f) intensity += 0.003f;
            for (int layer=0;layer<8;layer++) { double h=layer*1.5, r=1.0+layer*0.8*intensity;
                for (int i=0;i<(int)(10*intensity);i++) { double a=ticksAlive*0.1+Math.random()*Math.PI*2;
                    w.spawnParticle(Particle.SMOKE, c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r), 1, 0.3, 0.2, 0.3, 0.02);
                    if (layer<3) DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r), 1, 0.2, 40, 20, 50, 1.5f); } }
            for (int i=0;i<debris.size();i++) { double a=ticksAlive*0.08+i*(Math.PI*2/debris.size()), h=1.0+(i%4)*2.5, r=2.0+h*0.3*intensity;
                debris.get(i).entity().teleport(c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r)); debris.get(i).rotate(ticksAlive*0.1f+i,1,1,0); }
            for (Player p : w.getPlayers()) { if (isExempt(p)) continue; double d=p.getLocation().distance(c);
                if (d<10&&d>1) p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.1*intensity))); }
            if (ticksAlive%10==0) DisplayBuilder.playSound(c, Sound.ENTITY_PHANTOM_FLAP, 0.5f+intensity*0.5f, 0.3f);
        }
        @Override protected void onCleanup() { displayBuilder.removeAll(); }
        @Override public AbstractAttack newInstance() { return new NightmareTornado(plugin); }
    }

    // 13. DREAM STORM — Ultimate combined supernatural storm
    public static class DreamStorm extends EnvironmentalAttack {
        public DreamStorm(ChaosCraftPlugin plugin) {
            super(plugin, new AttackConfig("dream_storm", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true); config.setDamage(10.0); config.setDamageRadius(12.0);
            config.setTicksBetweenDamage(12); config.setDurationTicks(400); config.setCooldownTicks(400);
        }
        @Override protected void onSpawn(Location center) { DisplayBuilder.playSound(center, Sound.ENTITY_WITHER_SPAWN, 0.8f, 0.4f);
            DisplayBuilder.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.3f); }
        @Override protected void onTick(int ticksAlive) {
            Location c = getCenter(); if (c == null || c.getWorld() == null) return; World w = c.getWorld();
            // Portal rain
            for (int i=0;i<15;i++) { double x=c.getX()+Math.random()*24-12, z=c.getZ()+Math.random()*24-12, y=c.getY()+6+Math.random()*8;
                w.spawnParticle(Particle.REVERSE_PORTAL, new Location(w,x,y,z), 2, 0.5, 0.5, 0.5, 0.1);
                w.spawnParticle(Particle.PORTAL, new Location(w,x,y-2,z), 1, 0.3, 0.3, 0.3, 0.5); }
            // Enchant sparkle
            for (int i=0;i<10;i++) w.spawnParticle(Particle.ENCHANT,
                    new Location(w,c.getX()+Math.random()*20-10,c.getY()+Math.random()*10,c.getZ()+Math.random()*20-10), 3, 0.3, 1, 0.3, 0.5);
            // Soul lightning
            if (ticksAlive%30==0) { Location b=c.clone().add(Math.random()*16-8,0,Math.random()*16-8);
                DisplayBuilder.particleLine(b.clone().add(0,15,0),b, Particle.SOUL_FIRE_FLAME, 5, null);
                w.spawnParticle(Particle.SOUL, b, 15, 1, 0.5, 1, 0.05);
                DisplayBuilder.playSound(b, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 0.4f); }
            // Color dust storm
            if (ticksAlive%2==0) { Color[] cols={Color.fromRGB(150,50,200),Color.fromRGB(50,150,200),Color.fromRGB(200,50,50),Color.fromRGB(200,200,50)};
                for (int i=0;i<8;i++) w.spawnParticle(Particle.DUST,
                        new Location(w,c.getX()+Math.random()*20-10,c.getY()+Math.random()*6,c.getZ()+Math.random()*20-10), 2, 0.5, 0.5, 0.5, 0,
                        new Particle.DustOptions(cols[(int)(Math.random()*4)], 1.5f)); }
            // Gravity anomalies
            if (ticksAlive%20==0) { Location a=c.clone().add(Math.random()*12-6,0,Math.random()*12-6);
                w.spawnParticle(Particle.REVERSE_PORTAL, a, 20, 0.5, 2, 0.5, 0.1);
                for (Player p : w.getPlayers()) { if (isExempt(p)) continue; if (p.getLocation().distanceSquared(a)<=9) p.setVelocity(p.getVelocity().add(new Vector(0,0.8,0))); } }
            if (ticksAlive%45==0) { DisplayBuilder.playSound(c, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.3f+(float)(Math.random()*0.3));
                w.spawnParticle(Particle.END_ROD, c.clone().add(0,8,0), 40, 10, 4, 10, 0.01); }
        }
        @Override protected void onCleanup() { super.onCleanup(); }
        @Override public AbstractAttack newInstance() { return new DreamStorm(plugin); }
    }
}
