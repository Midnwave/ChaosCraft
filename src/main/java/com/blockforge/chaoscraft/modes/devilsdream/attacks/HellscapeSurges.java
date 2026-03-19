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

/** Devil's Dream — HELLSCAPE SURGE ENVIRONMENTAL ATTACKS. 13 fire/brimstone area effects. */
public class HellscapeSurges {
    private HellscapeSurges() {}
    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new HeatWave(plugin)); registry.register(new EmberStorm(plugin)); registry.register(new BrimstoneEruption(plugin));
        registry.register(new InfernalSurge(plugin)); registry.register(new MagmaRise(plugin)); registry.register(new HellfirePillar(plugin));
        registry.register(new CinderRain(plugin)); registry.register(new LavaFlow(plugin)); registry.register(new NetherBreeze(plugin));
        registry.register(new VolcanicBurst(plugin)); registry.register(new InfernalGround(plugin)); registry.register(new FireWhirl(plugin));
        registry.register(new HellPulse(plugin));
    }

    // 1. HEAT WAVE — Pulsing waves of heat distortion, flame particles sweep across
    public static class HeatWave extends EnvironmentalAttack {
        public HeatWave(ChaosCraftPlugin p) { super(p, new AttackConfig("heat_wave", AttackType.ENVIRONMENTAL, 1, "modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(15);config.setDurationTicks(350);config.setCooldownTicks(300); }
        @Override protected void onSpawn(Location c) { DisplayBuilder.playSound(c, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 0.3f); }
        @Override protected void onTick(int t) { Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            double waveZ = Math.sin(t*0.06)*12;
            for(int x=-8;x<=8;x+=2)for(double y=0;y<4;y+=1) w.spawnParticle(Particle.FLAME, c.clone().add(x,y,waveZ),1,0.5,0.3,0.1,0.02);
            if(t%2==0)for(int i=0;i<10;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*10;
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,1+Math.random()*3,Math.sin(a)*d),1,0.3,255,180,80,1.5f);}
            if(t%3==0)w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,c.clone().add(Math.random()*16-8,0.3,waveZ),2,0.5,0.2,0.5,0.005);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.5f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new HeatWave(plugin);}
    }

    // 2. EMBER STORM — Dense cloud of embers and sparks
    public static class EmberStorm extends EnvironmentalAttack {
        public EmberStorm(ChaosCraftPlugin p){super(p,new AttackConfig("ember_storm",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,1.0f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int i=0;i<25;i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10,y=c.getY()+Math.random()*6;
                w.spawnParticle(Particle.FLAME,new Location(w,x,y,z),1,0.3,0.3,0.3,0.04);
                if(Math.random()<0.3)w.spawnParticle(Particle.ELECTRIC_SPARK,new Location(w,x,y,z),2,0.1,0.1,0.1,0.05);}
            if(t%3==0)for(int i=0;i<8;i++)DisplayBuilder.dustParticles(new Location(w,c.getX()+Math.random()*18-9,c.getY()+Math.random()*5,c.getZ()+Math.random()*18-9),1,0.2,255,160,40,1.2f);
            if(t%15==0)DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.6f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new EmberStorm(plugin);}
    }

    // 3. BRIMSTONE ERUPTION — Periodic eruption bursts from center
    public static class BrimstoneEruption extends EnvironmentalAttack {
        public BrimstoneEruption(ChaosCraftPlugin p){super(p,new AttackConfig("brimstone_eruption",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(12.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(30);config.setDurationTicks(300);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%30==0){w.spawnParticle(Particle.FLAME,c,30,2,3,2,0.08);w.spawnParticle(Particle.LAVA,c,15,1.5,2,1.5,0);
                w.spawnParticle(Particle.SMOKE,c,20,2,2,2,0.05);DisplayBuilder.particleRing(c,5.0,Particle.FLAME,20,null);
                DisplayBuilder.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.7f,0.5f);
                for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=64){
                    Vector push=p.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.6).setY(0.4);
                    p.setVelocity(p.getVelocity().add(push));}}}
            if(t%4==0){double a=Math.random()*Math.PI*2;w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,c.clone().add(Math.cos(a)*2,0.5,Math.sin(a)*2),2,0.3,0.5,0.3,0.01);}
            if(t%10==0)DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_POP,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new BrimstoneEruption(plugin);}
    }

    // 4. INFERNAL SURGE — Rising column of fire particles from center
    public static class InfernalSurge extends EnvironmentalAttack {
        private float surgeHeight = 0;
        public InfernalSurge(ChaosCraftPlugin p){super(p,new AttackConfig("infernal_surge",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(10);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,1.0f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(surgeHeight<15)surgeHeight+=0.15f;
            for(double y=0;y<surgeHeight;y+=0.5){double wobble=Math.sin(y*0.5+t*0.1)*1.5;
                w.spawnParticle(Particle.FLAME,c.clone().add(wobble,y,Math.cos(y*0.5+t*0.1)*1.5),2,0.3,0.2,0.3,0.02);}
            if(t%2==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.LAVA,c.clone().add(Math.random()*2-1,Math.random()*surgeHeight,Math.random()*2-1),1,0,0,0,0);
            DisplayBuilder.particleRing(c.clone().add(0,0.3,0),3.0,Particle.FLAME,(int)(surgeHeight),null);
            if(t%12==0)DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.6f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new InfernalSurge(plugin);}
    }

    // 5. MAGMA RISE — Magma blocks rise from below forming an advancing floor
    public static class MagmaRise extends EnvironmentalAttack {
        private final List<BlockDisplayHandle> tiles=new ArrayList<>();private float riseY=-2;
        public MagmaRise(ChaosCraftPlugin p){super(p,new AttackConfig("magma_rise",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(10);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){World w=c.getWorld();if(w==null)return;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){BlockDisplayHandle t=displayBuilder.spawnBlock(c.clone().add(x*2,riseY,z*2),
                    (x+z)%2==0?Material.MAGMA_BLOCK:Material.NETHERRACK);t.scale(2.0f,0.5f,2.0f).glow(200,80,20).interpolation(3,0);
                tiles.add(t);spawnedEntities.add(t.entity());}
            DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_AMBIENT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(riseY<0)riseY+=0.015f;
            int idx=0;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){tiles.get(idx).entity().teleport(c.clone().add(x*2,riseY,z*2));idx++;}
            if(t%3==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.FLAME,c.clone().add(Math.random()*8-4,riseY+0.5,Math.random()*8-4),2,0.2,0.2,0.2,0.02);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_POP,0.5f,0.5f);}
        @Override protected void onCleanup(){displayBuilder.removeAll();}@Override public AbstractAttack newInstance(){return new MagmaRise(plugin);}
    }

    // 6. HELLFIRE PILLAR — Single massive fire column erupts periodically at random nearby spots
    public static class HellfirePillar extends EnvironmentalAttack {
        public HellfirePillar(ChaosCraftPlugin p){super(p,new AttackConfig("hellfire_pillar",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(10.0);config.setDamageRadius(5.0);config.setTicksBetweenDamage(20);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%25==0){Location pLoc=c.clone().add(Math.random()*10-5,0,Math.random()*10-5);
                for(double y=0;y<10;y+=0.5)w.spawnParticle(Particle.FLAME,pLoc.clone().add(0,y,0),3,0.4,0.2,0.4,0.03);
                w.spawnParticle(Particle.SMOKE,pLoc,15,0.5,5,0.5,0.03);
                DisplayBuilder.playSound(pLoc,Sound.ENTITY_BLAZE_SHOOT,0.7f,0.5f);
                DisplayBuilder.particleRing(pLoc,3.0,Particle.FLAME,16,null);}
            if(t%5==0)w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,c.clone().add(Math.random()*8-4,0.3,Math.random()*8-4),1,0.3,0.1,0.3,0.005);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new HellfirePillar(plugin);}
    }

    // 7. CINDER RAIN — Slow-falling embers and cinders
    public static class CinderRain extends EnvironmentalAttack {
        public CinderRain(ChaosCraftPlugin p){super(p,new AttackConfig("cinder_rain",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(6.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int i=0;i<15;i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10,y=c.getY()+6+Math.random()*8;
                w.spawnParticle(Particle.FLAME,new Location(w,x,y,z),1,0.1,-0.2,0.1,0.01);
                DisplayBuilder.dustParticles(new Location(w,x,y,z),1,0.2,200,120,40,0.8f);}
            if(t%4==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.ASH,c.clone().add(Math.random()*18-9,Math.random()*10,Math.random()*18-9),3,0.3,0.3,0.3,0);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.3f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new CinderRain(plugin);}
    }

    // 8. LAVA FLOW — Magma particles flow in one direction across the ground
    public static class LavaFlow extends EnvironmentalAttack {
        private double flowAngle=0;
        public LavaFlow(ChaosCraftPlugin p){super(p,new AttackConfig("lava_flow",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(10);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){flowAngle=Math.random()*Math.PI*2;DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_AMBIENT,1.0f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            double fx=Math.cos(flowAngle),fz=Math.sin(flowAngle);
            for(int i=0;i<20;i++){double perp=(Math.random()-0.5)*8;double along=Math.random()*16-8;
                Location p=c.clone().add(fx*along+fz*perp,0.2,-fz*along+fx*perp);
                w.spawnParticle(Particle.FLAME,p,1,fx*0.3,0.05,fz*0.3,0.02);
                DisplayBuilder.dustParticles(p,1,0.2,255,100,20,1.5f);}
            if(t%3==0)for(int i=0;i<5;i++){double perp=(Math.random()-0.5)*6;
                w.spawnParticle(Particle.LAVA,c.clone().add(fz*perp,0.3,-fx*perp),1,0,0,0,0);}
            if(t%15==0)DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_POP,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new LavaFlow(plugin);}
    }

    // 9. NETHER BREEZE — Warm particles drift with occasional ember gusts
    public static class NetherBreeze extends EnvironmentalAttack {
        public NetherBreeze(ChaosCraftPlugin p){super(p,new AttackConfig("nether_breeze",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(5.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(20);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_FLAP,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            double wx=Math.cos(t*0.005),wz=Math.sin(t*0.005);
            for(int i=0;i<12;i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10,y=c.getY()+Math.random()*4;
                w.spawnParticle(Particle.SMOKE,new Location(w,x,y,z),1,wx*0.2,0.05,wz*0.2,0.01);
                DisplayBuilder.dustParticles(new Location(w,x,y,z),1,0.2,180,100,60,1.0f);}
            if(t%8==0)for(int i=0;i<3;i++)w.spawnParticle(Particle.FLAME,
                    c.clone().add(Math.random()*16-8,Math.random()*3,Math.random()*16-8),1,wx*0.3,0.1,wz*0.3,0.02);
            if(t%30==0)DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_FLAP,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new NetherBreeze(plugin);}
    }

    // 10. VOLCANIC BURST — Random explosive bursts from the ground
    public static class VolcanicBurst extends EnvironmentalAttack {
        public VolcanicBurst(ChaosCraftPlugin p){super(p,new AttackConfig("volcanic_burst",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(11.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(25);config.setDurationTicks(300);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.6f,0.5f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%20==0){Location burst=c.clone().add(Math.random()*10-5,0,Math.random()*10-5);
                w.spawnParticle(Particle.FLAME,burst,20,0.5,2,0.5,0.06);w.spawnParticle(Particle.LAVA,burst,10,0.5,1,0.5,0);
                w.spawnParticle(Particle.EXPLOSION,burst,1,0,0,0,0);DisplayBuilder.playSound(burst,Sound.ENTITY_GENERIC_EXPLODE,0.5f,0.6f);}
            if(t%4==0)w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,c.clone().add(Math.random()*8-4,0.3,Math.random()*8-4),1,0.3,0.1,0.3,0.005);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new VolcanicBurst(plugin);}
    }

    // 11. INFERNAL GROUND — Ground glows hot, flame particles rise everywhere
    public static class InfernalGround extends EnvironmentalAttack {
        public InfernalGround(ChaosCraftPlugin p){super(p,new AttackConfig("infernal_ground",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_AMBIENT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int i=0;i<15;i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10;
                w.spawnParticle(Particle.FLAME,new Location(w,x,c.getY()+0.2,z),1,0.1,0.3,0.1,0.01);
                DisplayBuilder.dustParticles(new Location(w,x,c.getY()+0.1,z),1,0.3,255,100,20,1.8f);}
            if(t%4==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.LAVA,c.clone().add(Math.random()*18-9,0.3,Math.random()*18-9),1,0,0,0,0);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_LAVA_POP,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new InfernalGround(plugin);}
    }

    // 12. FIRE WHIRL — Spinning fire vortex column
    public static class FireWhirl extends EnvironmentalAttack {
        public FireWhirl(ChaosCraftPlugin p){super(p,new AttackConfig("fire_whirl",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(10);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int layer=0;layer<10;layer++){double h=layer*0.8;double r=1.5+layer*0.3;double a=t*0.15+layer*0.4;
                w.spawnParticle(Particle.FLAME,c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r),3,0.2,0.2,0.2,0.02);
                w.spawnParticle(Particle.SMOKE,c.clone().add(Math.cos(a+Math.PI)*r,h,Math.sin(a+Math.PI)*r),1,0.1,0.1,0.1,0.01);}
            if(t%3==0)DisplayBuilder.dustParticles(c.clone().add(0,2,0),5,2.0,255,100,20,1.5f);
            for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(c);if(d<6&&d>1)
                p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.08)));}
            if(t%12==0)DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_FLAP,0.5f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new FireWhirl(plugin);}
    }

    // 13. HELL PULSE — Expanding ring of fire that repeats from center
    public static class HellPulse extends EnvironmentalAttack {
        private float pulseRadius=0;
        public HellPulse(ChaosCraftPlugin p){super(p,new AttackConfig("hell_pulse",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_AMBIENT,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            pulseRadius+=0.3f;if(pulseRadius>12)pulseRadius=0;
            DisplayBuilder.particleRing(c.clone().add(0,0.3,0),pulseRadius,Particle.FLAME,(int)(pulseRadius*6),null);
            DisplayBuilder.particleRing(c.clone().add(0,1.0,0),pulseRadius*0.8,Particle.DUST,(int)(pulseRadius*4),
                    new Particle.DustOptions(Color.fromRGB(255,100,20),1.5f));
            if(pulseRadius<1&&t>5)DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,0.5f,0.5f);
            if(t%4==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.SMOKE,c.clone().add(Math.random()*pulseRadius*2-pulseRadius,0.5,Math.random()*pulseRadius*2-pulseRadius),1,0.2,0.2,0.2,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new HellPulse(plugin);}
    }
}
