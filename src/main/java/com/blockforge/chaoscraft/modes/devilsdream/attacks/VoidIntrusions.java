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

/** Devil's Dream — VOID INTRUSION ENVIRONMENTAL ATTACKS. 13 darkness/void themed effects. */
public class VoidIntrusions {
    private VoidIntrusions() {}
    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new ShadowCreep(plugin)); registry.register(new LightDrain(plugin)); registry.register(new VoidPocket(plugin));
        registry.register(new DarknessSurge(plugin)); registry.register(new AbyssGaze(plugin)); registry.register(new ShadowTentacle(plugin));
        registry.register(new VoidBurst(plugin)); registry.register(new NightEncroach(plugin)); registry.register(new ObsidianTear(plugin));
        registry.register(new ShadowWave(plugin)); registry.register(new VoidWhisper(plugin)); registry.register(new DarkPrison(plugin));
        registry.register(new AbyssalRift(plugin));
    }

    // 1. SHADOW CREEP — Dark particles slowly crawl across the ground from edges
    public static class ShadowCreep extends EnvironmentalAttack {
        private float creepRadius=12;
        public ShadowCreep(ChaosCraftPlugin p){super(p,new AttackConfig("shadow_creep",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(6.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(creepRadius>2)creepRadius-=0.02f;
            // Dark ground layer creeping inward
            for(int i=0;i<20;i++){double a=Math.random()*Math.PI*2;double d=creepRadius+(Math.random()*3);
                Location p=c.clone().add(Math.cos(a)*d,0.1,Math.sin(a)*d);
                DisplayBuilder.dustParticles(p,2,0.5,10,5,15,2.5f);
                if(Math.random()<0.3)w.spawnParticle(Particle.SMOKE,p.clone().add(0,0.2,0),1,0.3,0.1,0.3,0.005);}
            // Inner darkness filling
            if(t%3==0)for(int i=0;i<(int)((12-creepRadius)*2);i++){double a=Math.random()*Math.PI*2;double d=Math.random()*creepRadius;
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,0.15,Math.sin(a)*d),1,0.3,8,3,12,2.0f);}
            // Creeping tendrils
            if(t%4==0)for(int i=0;i<4;i++){double a=(2*Math.PI*i)/4+t*0.01;
                for(double d=creepRadius;d>creepRadius-3;d-=0.5)
                    w.spawnParticle(Particle.SMOKE,c.clone().add(Math.cos(a)*d,0.15,Math.sin(a)*d),1,0.1,0.05,0.1,0.003);}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new ShadowCreep(plugin);}
    }

    // 2. LIGHT DRAIN — Brightness dims. Dark particles absorb light, end rods flicker and die
    public static class LightDrain extends EnvironmentalAttack {
        private float darkness=0;
        public LightDrain(ChaosCraftPlugin p){super(p,new AttackConfig("light_drain",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(5.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(20);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(darkness<1)darkness+=0.004f;
            // Dense dark particles filling the area
            for(int i=0;i<(int)(darkness*30);i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10,y=c.getY()+Math.random()*5;
                DisplayBuilder.dustParticles(new Location(w,x,y,z),1,0.3,(int)(15-darkness*10),(int)(8-darkness*5),(int)(20-darkness*12),2.0f+darkness);}
            // Flickering dying light particles
            if(darkness>0.3f&&t%6==0)for(int i=0;i<3;i++){Location fLoc=c.clone().add(Math.random()*12-6,Math.random()*4,Math.random()*12-6);
                if(Math.random()<darkness)w.spawnParticle(Particle.SMOKE,fLoc,3,0.2,0.2,0.2,0.01);
                else w.spawnParticle(Particle.END_ROD,fLoc,1,0.1,0.1,0.1,0.01);}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,darkness*0.3f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new LightDrain(plugin);}
    }

    // 3. VOID POCKET — Spherical void zones appear randomly. Pure darkness inside
    public static class VoidPocket extends EnvironmentalAttack {
        private final List<double[]> pockets=new ArrayList<>();
        public VoidPocket(ChaosCraftPlugin p){super(p,new AttackConfig("void_pocket",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(10.0);config.setDamageRadius(4.0);config.setTicksBetweenDamage(10);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Spawn new pocket every 40 ticks
            if(t%40==0&&pockets.size()<6)pockets.add(new double[]{Math.random()*10-5,1+Math.random()*3,Math.random()*10-5,1.5+Math.random()*1.5});
            // Render void pockets
            for(double[] pocket:pockets){Location pLoc=c.clone().add(pocket[0],pocket[1],pocket[2]);double r=pocket[3];
                if(t%2==0){// Dark sphere shell
                    for(int i=0;i<12;i++){double a1=Math.random()*Math.PI*2;double a2=Math.random()*Math.PI;
                        Location sLoc=pLoc.clone().add(Math.cos(a1)*Math.sin(a2)*r,Math.cos(a2)*r,Math.sin(a1)*Math.sin(a2)*r);
                        DisplayBuilder.dustParticles(sLoc,1,0.1,5,2,10,2.0f);}
                    // Inner void
                    w.spawnParticle(Particle.SMOKE,pLoc,5,(float)r*0.5,(float)r*0.5,(float)r*0.5,0.005);
                    w.spawnParticle(Particle.REVERSE_PORTAL,pLoc,3,(float)r*0.3,(float)r*0.3,(float)r*0.3,0.05);}
                // Suction toward pocket center
                for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(pLoc);if(d<r+2&&d>0.5)
                    p.setVelocity(p.getVelocity().add(pLoc.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.08)));}}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();pockets.clear();}@Override public AbstractAttack newInstance(){return new VoidPocket(plugin);}
    }

    // 4. DARKNESS SURGE — Wave of pure darkness sweeps across area
    public static class DarknessSurge extends EnvironmentalAttack {
        private double surgeZ=-10;
        public DarknessSurge(ChaosCraftPlugin p){super(p,new AttackConfig("darkness_surge",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(12);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_ROAR,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            surgeZ+=0.15;if(surgeZ>10)surgeZ=-10;
            // Wall of darkness sweeping through
            for(int x=-10;x<=10;x+=1)for(double y=0;y<5;y+=0.5){
                Location sLoc=new Location(w,c.getX()+x,c.getY()+y,c.getZ()+surgeZ);
                DisplayBuilder.dustParticles(sLoc,1,0.3,8,3,12,2.5f);
                if(Math.random()<0.1)w.spawnParticle(Particle.SMOKE,sLoc,1,0.2,0.2,0.2,0.005);}
            // Trailing darkness behind the wave
            if(t%3==0)for(int i=0;i<5;i++){
                double x=c.getX()+Math.random()*18-9;
                w.spawnParticle(Particle.SMOKE,new Location(w,x,c.getY()+Math.random()*3,c.getZ()+surgeZ-Math.random()*3),2,0.3,0.2,0.3,0.008);}
            if(t%20==0)DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_FLAP,0.4f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DarknessSurge(plugin);}
    }

    // 5. ABYSS GAZE — Giant "eye" of darkness stares down from above
    public static class AbyssGaze extends EnvironmentalAttack {
        public AbyssGaze(ChaosCraftPlugin p){super(p,new AttackConfig("abyss_gaze",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            Location eyeCenter=c.clone().add(0,12,0);
            // Outer ring (iris)
            if(t%3==0){DisplayBuilder.particleRing(eyeCenter,4.0,Particle.DUST,20,new Particle.DustOptions(Color.fromRGB(30,10,50),2.0f));
                DisplayBuilder.particleRing(eyeCenter,3.0,Particle.DUST,16,new Particle.DustOptions(Color.fromRGB(50,20,80),1.5f));}
            // Pupil (dark center)
            if(t%2==0){w.spawnParticle(Particle.SMOKE,eyeCenter,8,1,0.2,1,0.005);
                DisplayBuilder.dustParticles(eyeCenter,3,1.0,5,2,8,2.5f);}
            // Eye glow at center
            if(t%4==0)w.spawnParticle(Particle.END_ROD,eyeCenter,2,0.5,0.1,0.5,0.005);
            // Gaze beam down to player
            if(t%6==0)DisplayBuilder.particleLine(eyeCenter,c.clone().add(0,2,0),Particle.DUST,1,
                    new Particle.DustOptions(Color.fromRGB(20,8,40),0.8f));
            if(t%50==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.3f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new AbyssGaze(plugin);}
    }

    // 6. SHADOW TENTACLE — Dark tentacles of particles reach toward the player from edges
    public static class ShadowTentacle extends EnvironmentalAttack {
        private final List<Double> tentAngles=new ArrayList<>();
        public ShadowTentacle(ChaosCraftPlugin p){super(p,new AttackConfig("shadow_tentacle",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(12);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){for(int i=0;i<6;i++)tentAngles.add((2*Math.PI*i)/6);
            DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_EMERGE,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int i=0;i<tentAngles.size();i++){double a=tentAngles.get(i);
                double reach=Math.min(8,t*0.04);
                // Tentacle made of dark dust from edge toward center
                for(double d=8;d>8-reach;d-=0.5){
                    double sway=Math.sin(d*0.8+t*0.05+i*1.5)*1.0;double y=0.5+Math.sin(d*0.5+t*0.03)*0.5;
                    Location tLoc=c.clone().add(Math.cos(a)*d+Math.sin(a)*sway,y,Math.sin(a)*d-Math.cos(a)*sway);
                    if(t%2==0){DisplayBuilder.dustParticles(tLoc,1,0.15,15,8,25,1.5f-(float)(d*0.08));
                        if(d<4)w.spawnParticle(Particle.SMOKE,tLoc,1,0.1,0.1,0.1,0.003);}}}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_TENDRIL_CLICKS,0.4f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();tentAngles.clear();}@Override public AbstractAttack newInstance(){return new ShadowTentacle(plugin);}
    }

    // 7. VOID BURST — Explosive void detonations at random locations
    public static class VoidBurst extends EnvironmentalAttack {
        public VoidBurst(ChaosCraftPlugin p){super(p,new AttackConfig("void_burst",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(11.0);config.setDamageRadius(5.0);config.setTicksBetweenDamage(25);config.setDurationTicks(300);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%25==0){Location burst=c.clone().add(Math.random()*10-5,1+Math.random()*2,Math.random()*10-5);
                // Dark explosion
                for(float r=0.5f;r<=4;r+=0.8f)DisplayBuilder.particleRing(burst,r,Particle.DUST,(int)(r*6),
                        new Particle.DustOptions(Color.fromRGB(10,5,20),2.0f));
                w.spawnParticle(Particle.REVERSE_PORTAL,burst,20,1.5,1,1.5,0.15);w.spawnParticle(Particle.SMOKE,burst,15,1,1,1,0.05);
                DisplayBuilder.playSound(burst,Sound.ENTITY_WARDEN_SONIC_BOOM,0.4f,0.5f);
                for(Player p:w.getPlayers()){if(!isExempt(p)&&p.getLocation().distanceSquared(burst)<=25){
                    Vector push=p.getLocation().toVector().subtract(burst.toVector()).normalize().multiply(0.6).setY(0.3);
                    p.setVelocity(p.getVelocity().add(push));}}}
            // Ambient void haze
            if(t%4==0)for(int i=0;i<3;i++)DisplayBuilder.dustParticles(c.clone().add(Math.random()*10-5,Math.random()*3,Math.random()*10-5),1,0.3,10,5,18,1.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new VoidBurst(plugin);}
    }

    // 8. NIGHT ENCROACH — Darkness slowly fills from all edges toward center
    public static class NightEncroach extends EnvironmentalAttack {
        private float nightRadius=12;
        public NightEncroach(ChaosCraftPlugin p){super(p,new AttackConfig("night_encroach",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(6.0);config.setDamageRadius(12.0);config.setTicksBetweenDamage(18);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.AMBIENT_CAVE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(nightRadius>2)nightRadius-=0.015f;
            // Darkness boundary ring
            if(t%3==0){DisplayBuilder.particleRing(c.clone().add(0,0.3,0),nightRadius,Particle.DUST,(int)(nightRadius*5),
                    new Particle.DustOptions(Color.fromRGB(10,5,15),2.0f));
                DisplayBuilder.particleRing(c.clone().add(0,2,0),nightRadius,Particle.SMOKE,(int)(nightRadius*3),null);}
            // Dense darkness outside the boundary
            for(int i=0;i<15;i++){double a=Math.random()*Math.PI*2;double d=nightRadius+Math.random()*4;
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,Math.random()*4,Math.sin(a)*d),1,0.3,8,3,12,2.5f);}
            if(t%30==0){DisplayBuilder.playSound(c,Sound.AMBIENT_CAVE,0.3f,0.3f);
                Sound[] creepy={Sound.ENTITY_VEX_AMBIENT,Sound.ENTITY_PHANTOM_AMBIENT};
                DisplayBuilder.playSound(c.clone().add(Math.random()*10-5,1,Math.random()*10-5),creepy[(int)(Math.random()*2)],0.2f,0.3f);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new NightEncroach(plugin);}
    }

    // 9. OBSIDIAN TEAR — Dark tears in reality drip obsidian-colored particles downward
    public static class ObsidianTear extends EnvironmentalAttack {
        private final List<double[]> tears=new ArrayList<>();
        public ObsidianTear(ChaosCraftPlugin p){super(p,new AttackConfig("obsidian_tear",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){for(int i=0;i<8;i++)tears.add(new double[]{Math.random()*12-6,6+Math.random()*4,Math.random()*12-6});
            DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(double[] tear:tears){Location tLoc=c.clone().add(tear[0],tear[1],tear[2]);
                // Tear shape — vertical dark slit
                if(t%3==0){for(double y=-1;y<=1;y+=0.3)DisplayBuilder.dustParticles(tLoc.clone().add(0,y,0),1,0.05,20,10,30,1.5f);
                    // Dripping particles
                    w.spawnParticle(Particle.DRIPPING_OBSIDIAN_TEAR,tLoc.clone().add(0,-1.5,0),2,0.1,0.1,0.1,0);
                    if(Math.random()<0.3)DisplayBuilder.dustParticles(tLoc.clone().add(0,-2-Math.random()*2,0),1,0.1,40,20,60,1.0f);}}
            // Pool particles where drips land
            if(t%5==0)for(double[] tear:tears)DisplayBuilder.dustParticles(c.clone().add(tear[0],0.1,tear[2]),1,0.3,30,15,50,1.5f);
            if(t%30==0)DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();tears.clear();}@Override public AbstractAttack newInstance(){return new ObsidianTear(plugin);}
    }

    // 10. SHADOW WAVE — Rippling shadow rings on the ground emanating from center
    public static class ShadowWave extends EnvironmentalAttack {
        private float wavePhase=0;
        public ShadowWave(ChaosCraftPlugin p){super(p,new AttackConfig("shadow_wave",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            wavePhase+=0.2f;
            // Multiple ripple rings
            for(int wave=0;wave<3;wave++){float r=(wavePhase+wave*4)%12;if(r>0&&r<12){
                float opacity=1.0f-r/12.0f;int color=(int)(15*opacity);
                DisplayBuilder.particleRing(c.clone().add(0,0.15,0),r,Particle.DUST,(int)(r*5),
                        new Particle.DustOptions(Color.fromRGB(color+5,color,color+10),1.5f+opacity));}}
            // Dark mist at ground level
            if(t%4==0)for(int i=0;i<5;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*10;
                w.spawnParticle(Particle.SMOKE,c.clone().add(Math.cos(a)*d,0.2,Math.sin(a)*d),1,0.3,0.05,0.3,0.003);}
            if(t%20==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.2f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new ShadowWave(plugin);}
    }

    // 11. VOID WHISPER — Directional whisper particles fly from one side with eerie sounds
    public static class VoidWhisper extends EnvironmentalAttack {
        private double whisperAngle=0;
        public VoidWhisper(ChaosCraftPlugin p){super(p,new AttackConfig("void_whisper",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(5.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(20);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){whisperAngle=Math.random()*Math.PI*2;DisplayBuilder.playSound(c,Sound.ENTITY_VEX_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            whisperAngle+=0.005;double wx=Math.cos(whisperAngle),wz=Math.sin(whisperAngle);
            // Whisper particles streaming from one direction
            if(t%2==0)for(int i=0;i<8;i++){double perp=(Math.random()-0.5)*8;double along=-8+Math.random()*16;
                Location wLoc=c.clone().add(wx*along+wz*perp,0.5+Math.random()*3,-wz*along+wx*perp);
                DisplayBuilder.dustParticles(wLoc,1,0.2,30,20,50,1.0f);
                w.spawnParticle(Particle.SMOKE,wLoc,1,wx*0.2,0.05,wz*0.2,0.005);}
            // Random whisper sounds
            if(t%(20+(int)(Math.random()*30))==0){Location sLoc=c.clone().add(wx*8+Math.random()*4-2,2,wz*8+Math.random()*4-2);
                Sound[] s={Sound.ENTITY_VEX_AMBIENT,Sound.ENTITY_ENDERMAN_AMBIENT,Sound.ENTITY_PHANTOM_AMBIENT};
                DisplayBuilder.playSound(sLoc,s[(int)(Math.random()*3)],0.2f,0.3f+(float)(Math.random()*0.3));}
            if(t%6==0)w.spawnParticle(Particle.SOUL,c.clone().add(Math.random()*8-4,1,Math.random()*8-4),1,0.2,0.2,0.2,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new VoidWhisper(plugin);}
    }

    // 12. DARK PRISON — Walls of darkness form around the player, closing slowly
    public static class DarkPrison extends EnvironmentalAttack {
        private float wallDist=6;
        public DarkPrison(ChaosCraftPlugin p){super(p,new AttackConfig("dark_prison",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_PISTON_EXTEND,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(wallDist>2)wallDist-=0.015f;
            // 4 dark walls (particle sheets)
            if(t%2==0)for(int wall=0;wall<4;wall++){double angle=(Math.PI/2)*wall;double wx=Math.cos(angle)*wallDist,wz=Math.sin(angle)*wallDist;
                for(int i=-4;i<=4;i++)for(double y=0;y<4;y+=0.5){
                    double px=c.getX()+wx+Math.sin(angle)*i;double pz=c.getZ()+wz-Math.cos(angle)*i;
                    DisplayBuilder.dustParticles(new Location(w,px,c.getY()+y,pz),1,0.1,10,5,18,2.0f);}}
            // Ceiling
            if(t%4==0)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)
                DisplayBuilder.dustParticles(c.clone().add(x*wallDist*0.3,4,z*wallDist*0.3),1,0.2,8,3,15,1.5f);
            // Push players inward when touching walls
            for(Player p:w.getPlayers()){if(isExempt(p))continue;Location pLoc=p.getLocation();
                if(Math.abs(pLoc.getX()-c.getX())>wallDist-0.5||Math.abs(pLoc.getZ()-c.getZ())>wallDist-0.5)
                    p.setVelocity(p.getVelocity().add(c.toVector().subtract(pLoc.toVector()).normalize().multiply(0.2)));}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.BLOCK_PISTON_CONTRACT,0.4f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DarkPrison(plugin);}
    }

    // 13. ABYSSAL RIFT — Massive vertical tear in space with void particles pouring through
    public static class AbyssalRift extends EnvironmentalAttack {
        private float riftHeight=0;
        public AbyssalRift(ChaosCraftPlugin p){super(p,new AttackConfig("abyssal_rift",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(10);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_ROAR,0.6f,0.3f);
            DisplayBuilder.playSound(c,Sound.BLOCK_GLASS_BREAK,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(riftHeight<10)riftHeight+=0.06f;
            // Vertical rift — jagged line of void particles
            if(t%2==0)for(double y=0;y<riftHeight;y+=0.3){double jagged=Math.sin(y*1.5+t*0.05)*0.5;
                Location rLoc=c.clone().add(jagged,y,-3);
                // Rift edges
                DisplayBuilder.dustParticles(rLoc.clone().add(-0.3,0,0),1,0.05,20,10,40,1.8f);
                DisplayBuilder.dustParticles(rLoc.clone().add(0.3,0,0),1,0.05,20,10,40,1.8f);
                // Void inside
                w.spawnParticle(Particle.REVERSE_PORTAL,rLoc,2,0.15,0.1,0.1,0.08);
                if(Math.random()<0.15)w.spawnParticle(Particle.SMOKE,rLoc,1,0.1,0.1,0.1,0.01);}
            // Particles pouring out of the rift
            if(t%3==0)for(int i=0;i<5;i++){double y=Math.random()*riftHeight;
                Location pourLoc=c.clone().add(Math.sin(y*1.5+t*0.05)*0.5,y,-3);
                w.spawnParticle(Particle.SMOKE,pourLoc.clone().add(0,0,Math.random()*3),2,0.5,0.2,0.5,0.01);
                DisplayBuilder.dustParticles(pourLoc.clone().add(0,0,Math.random()*2),1,0.3,15,8,30,1.5f);}
            // Pull toward rift
            for(Player p:w.getPlayers()){if(isExempt(p))continue;Location pLoc=p.getLocation();Location riftCenter=c.clone().add(0,riftHeight/2,-3);
                if(pLoc.distanceSquared(riftCenter)<=36)p.setVelocity(p.getVelocity().add(riftCenter.toVector().subtract(pLoc.toVector()).normalize().multiply(0.06)));}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.3f,0.3f);
            if(t%40==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_ROAR,0.2f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new AbyssalRift(plugin);}
    }
}
