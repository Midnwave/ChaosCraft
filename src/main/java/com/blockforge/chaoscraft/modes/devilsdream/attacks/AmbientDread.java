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

/** Devil's Dream — AMBIENT DREAD ENVIRONMENTAL ATTACKS. 13 atmospheric horror effects. */
public class AmbientDread {
    private AmbientDread() {}
    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new WhisperZone(plugin)); registry.register(new HeartbeatPulse(plugin)); registry.register(new NightmareEcho(plugin));
        registry.register(new ColdPresence(plugin)); registry.register(new WatchingFeeling(plugin)); registry.register(new DreadAura(plugin));
        registry.register(new ShadowFollower(plugin)); registry.register(new InsanityTick(plugin)); registry.register(new NightmareBreath(plugin));
        registry.register(new FadingReality(plugin)); registry.register(new DreamDecay(plugin)); registry.register(new PhantomTouch(plugin));
        registry.register(new DarkResonance(plugin));
    }

    // 1. WHISPER ZONE — Eerie whisper sounds from random directions, soul particles drift
    public static class WhisperZone extends EnvironmentalAttack {
        public WhisperZone(ChaosCraftPlugin p){super(p,new AttackConfig("whisper_zone",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.AMBIENT_CAVE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%(15+(int)(Math.random()*25))==0){Location wLoc=c.clone().add(Math.random()*16-8,1+Math.random()*3,Math.random()*16-8);
                Sound[] whispers={Sound.ENTITY_VEX_AMBIENT,Sound.ENTITY_PHANTOM_AMBIENT,Sound.ENTITY_ENDERMAN_AMBIENT,Sound.AMBIENT_CAVE};
                DisplayBuilder.playSound(wLoc,whispers[(int)(Math.random()*whispers.length)],0.3f,0.2f+(float)(Math.random()*0.3));
                w.spawnParticle(Particle.SOUL,wLoc,3,0.3,0.3,0.3,0.02);}
            if(t%5==0){double a=Math.random()*Math.PI*2;double d=Math.random()*8;
                w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(a)*d,0.5+Math.random()*3,Math.sin(a)*d),1,0.2,0.2,0.2,0.01);
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,1+Math.random()*2,Math.sin(a)*d),1,0.3,60,40,80,1.0f);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new WhisperZone(plugin);}
    }

    // 2. HEARTBEAT PULSE — Rhythmic red pulse expanding from center, thumping sound
    public static class HeartbeatPulse extends EnvironmentalAttack {
        private int beatPhase=0;
        public HeartbeatPulse(ChaosCraftPlugin p){super(p,new AttackConfig("heartbeat_pulse",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_HEARTBEAT,0.8f,0.5f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Double-beat pattern: beat-beat...pause...beat-beat
            int cycle=t%40;boolean beating=(cycle<3)||(cycle>6&&cycle<10);
            if(beating){float radius=(cycle%7)*3.0f;
                DisplayBuilder.particleRing(c.clone().add(0,0.3,0),radius,Particle.DUST,(int)(radius*6),
                        new Particle.DustOptions(Color.fromRGB(180,20,20),2.0f));
                DisplayBuilder.particleRing(c.clone().add(0,1.5,0),radius*0.8,Particle.DUST,(int)(radius*4),
                        new Particle.DustOptions(Color.fromRGB(140,10,10),1.5f));}
            if(cycle==0||cycle==7)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_HEARTBEAT,0.6f,0.5f);
            // Ambient red glow
            if(t%4==0)for(int i=0;i<3;i++)DisplayBuilder.dustParticles(c.clone().add(Math.random()*8-4,Math.random()*2,Math.random()*8-4),2,0.5,120,15,15,1.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new HeartbeatPulse(plugin);}
    }

    // 3. NIGHTMARE ECHO — Distorted sounds replay from random positions
    public static class NightmareEcho extends EnvironmentalAttack {
        public NightmareEcho(ChaosCraftPlugin p){super(p,new AttackConfig("nightmare_echo",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%(10+(int)(Math.random()*20))==0){Location echoLoc=c.clone().add(Math.random()*12-6,Math.random()*4,Math.random()*12-6);
                Sound[] echos={Sound.ENTITY_PLAYER_HURT,Sound.ENTITY_ZOMBIE_AMBIENT,Sound.BLOCK_ANVIL_LAND,Sound.ENTITY_GHAST_SCREAM,
                        Sound.ENTITY_SKELETON_AMBIENT,Sound.ENTITY_WOLF_GROWL,Sound.BLOCK_GLASS_BREAK};
                DisplayBuilder.playSound(echoLoc,echos[(int)(Math.random()*echos.length)],0.3f,0.2f+(float)(Math.random()*0.5));
                w.spawnParticle(Particle.REVERSE_PORTAL,echoLoc,8,0.5,0.5,0.5,0.1);}
            if(t%6==0){double a=Math.random()*Math.PI*2;
                w.spawnParticle(Particle.ENCHANT,c.clone().add(Math.cos(a)*5,2,Math.sin(a)*5),3,0.3,0.3,0.3,0.3);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new NightmareEcho(plugin);}
    }

    // 4. COLD PRESENCE — Temperature drop. Frost particles, snowflakes, chill sounds
    public static class ColdPresence extends EnvironmentalAttack {
        public ColdPresence(ChaosCraftPlugin p){super(p,new AttackConfig("cold_presence",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_POLAR_BEAR_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int i=0;i<10;i++){double x=c.getX()+Math.random()*16-8,z=c.getZ()+Math.random()*16-8,y=c.getY()+Math.random()*5;
                w.spawnParticle(Particle.SNOWFLAKE,new Location(w,x,y,z),1,0.3,0.2,0.3,0.01);}
            if(t%3==0)for(int i=0;i<5;i++)DisplayBuilder.dustParticles(
                    c.clone().add(Math.random()*14-7,Math.random()*3,Math.random()*14-7),1,0.3,180,200,240,1.2f);
            // Breath particles near player area
            if(t%8==0)w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,c.clone().add(Math.random()*4-2,1.5,Math.random()*4-2),1,0.1,0.05,0.1,0.002);
            if(t%40==0)DisplayBuilder.playSound(c,Sound.ENTITY_POLAR_BEAR_AMBIENT,0.3f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new ColdPresence(plugin);}
    }

    // 5. WATCHING FEELING — Floating "eye" particles that track the player from distance
    public static class WatchingFeeling extends EnvironmentalAttack {
        private final List<double[]> eyePositions=new ArrayList<>();
        public WatchingFeeling(ChaosCraftPlugin p){super(p,new AttackConfig("watching_feeling",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){for(int i=0;i<6;i++)eyePositions.add(new double[]{Math.random()*16-8,3+Math.random()*5,Math.random()*16-8});
            DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(double[] eye:eyePositions){Location eLoc=c.clone().add(eye[0],eye[1],eye[2]);
                // Glowing eye — two particles side by side
                if(t%4==0){w.spawnParticle(Particle.END_ROD,eLoc.clone().add(-0.15,0,0),1,0.02,0.02,0.02,0.005);
                    w.spawnParticle(Particle.END_ROD,eLoc.clone().add(0.15,0,0),1,0.02,0.02,0.02,0.005);
                    DisplayBuilder.dustParticles(eLoc,2,0.1,220,200,50,0.8f);}}
            // Eyes slowly drift
            if(t%10==0)for(double[] eye:eyePositions){eye[0]+=(Math.random()-0.5)*0.3;eye[2]+=(Math.random()-0.5)*0.3;}
            if(t%60==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.2f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();eyePositions.clear();}@Override public AbstractAttack newInstance(){return new WatchingFeeling(plugin);}
    }

    // 6. DREAD AURA — Dark purple aura emanates from center, pulsing outward
    public static class DreadAura extends EnvironmentalAttack {
        public DreadAura(ChaosCraftPlugin p){super(p,new AttackConfig("dread_aura",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            float pulse=(float)Math.sin(t*0.06)*3+5;
            if(t%2==0){DisplayBuilder.particleRing(c.clone().add(0,0.3,0),pulse,Particle.DUST,(int)(pulse*5),
                    new Particle.DustOptions(Color.fromRGB(80,20,120),1.8f));
                DisplayBuilder.particleRing(c.clone().add(0,2.0,0),pulse*0.7,Particle.DUST,(int)(pulse*3),
                        new Particle.DustOptions(Color.fromRGB(60,10,100),1.3f));}
            for(int i=0;i<8;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*pulse;
                w.spawnParticle(Particle.REVERSE_PORTAL,c.clone().add(Math.cos(a)*d,0.5+Math.random()*3,Math.sin(a)*d),1,0.1,0.2,0.1,0.03);}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,0.4f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DreadAura(plugin);}
    }

    // 7. SHADOW FOLLOWER — Dark particle silhouette follows behind the player with delay
    public static class ShadowFollower extends EnvironmentalAttack {
        private final List<Location> history=new ArrayList<>();
        public ShadowFollower(ChaosCraftPlugin p){super(p,new AttackConfig("shadow_follower",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_AMBIENT,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%3==0){history.add(c.clone());if(history.size()>20)history.remove(0);}
            if(history.size()>5){Location shadow=history.get(Math.max(0,history.size()-6));
                if(t%2==0){for(double y=0;y<2.0;y+=0.3)DisplayBuilder.dustParticles(shadow.clone().add(0,y,0),2,0.15,15,10,20,1.5f);
                    w.spawnParticle(Particle.SMOKE,shadow.clone().add(0,1,0),3,0.2,0.5,0.2,0.01);}}
            if(t%50==0)DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_AMBIENT,0.3f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();history.clear();}@Override public AbstractAttack newInstance(){return new ShadowFollower(plugin);}
    }

    // 8. INSANITY TICK — Erratic clicking sounds and particle flashes from everywhere
    public static class InsanityTick extends EnvironmentalAttack {
        public InsanityTick(ChaosCraftPlugin p){super(p,new AttackConfig("insanity_tick",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_SCULK_SENSOR_CLICKING,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%(3+(int)(Math.random()*8))==0){Location tickLoc=c.clone().add(Math.random()*12-6,Math.random()*4,Math.random()*12-6);
                DisplayBuilder.playSound(tickLoc,Sound.BLOCK_SCULK_SENSOR_CLICKING,0.3f,0.2f+(float)(Math.random()*1.5));
                w.spawnParticle(Particle.ELECTRIC_SPARK,tickLoc,5,0.2,0.2,0.2,0.05);}
            if(t%(5+(int)(Math.random()*10))==0){w.spawnParticle(Particle.END_ROD,c.clone().add(Math.random()*10-5,Math.random()*5,Math.random()*10-5),8,0.3,0.3,0.3,0.02);}
            if(t%4==0)DisplayBuilder.dustParticles(c.clone().add(Math.random()*8-4,Math.random()*3,Math.random()*8-4),2,0.3,150,100,200,1.0f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new InsanityTick(plugin);}
    }

    // 9. NIGHTMARE BREATH — Dark smoke exhales from an invisible presence
    public static class NightmareBreath extends EnvironmentalAttack {
        private double breathAngle=0;
        public NightmareBreath(ChaosCraftPlugin p){super(p,new AttackConfig("nightmare_breath",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){breathAngle=Math.random()*Math.PI*2;DisplayBuilder.playSound(c,Sound.ENTITY_ENDER_DRAGON_GROWL,0.4f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            breathAngle+=0.02;Location breathSource=c.clone().add(Math.cos(breathAngle)*6,3,Math.sin(breathAngle)*6);
            double dx=c.getX()-breathSource.getX(),dz=c.getZ()-breathSource.getZ();double len=Math.sqrt(dx*dx+dz*dz);dx/=len;dz/=len;
            // Breath stream
            if(t%2==0)for(int i=0;i<8;i++){double d=i*0.8;
                Location bLoc=breathSource.clone().add(dx*d,-(i*0.15),dz*d);
                w.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE,bLoc,2,0.3,0.2,0.3,0.005);
                DisplayBuilder.dustParticles(bLoc,1,0.2,30,20,40,1.5f+(i*0.2f));}
            if(t%30==0)DisplayBuilder.playSound(breathSource,Sound.ENTITY_ENDER_DRAGON_GROWL,0.2f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new NightmareBreath(plugin);}
    }

    // 10. FADING REALITY — World flickers between normal and dark. Particle noise bursts
    public static class FadingReality extends EnvironmentalAttack {
        public FadingReality(ChaosCraftPlugin p){super(p,new AttackConfig("fading_reality",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            boolean dark=(t%30)<10;
            if(dark){for(int i=0;i<20;i++){double x=c.getX()+Math.random()*20-10,z=c.getZ()+Math.random()*20-10,y=c.getY()+Math.random()*6;
                DisplayBuilder.dustParticles(new Location(w,x,y,z),1,0.3,10,5,15,2.5f);}
                if(t%3==0)w.spawnParticle(Particle.SMOKE,c.clone().add(0,2,0),15,6,3,6,0.02);}
            else{if(t%5==0)for(int i=0;i<5;i++)w.spawnParticle(Particle.END_ROD,c.clone().add(Math.random()*14-7,Math.random()*5,Math.random()*14-7),1,0.1,0.1,0.1,0.01);}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.4f,0.3f+(float)(Math.random()*0.3));}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new FadingReality(plugin);}
    }

    // 11. DREAM DECAY — Reality crumbles. Block break particles everywhere, structures dissolve
    public static class DreamDecay extends EnvironmentalAttack {
        public DreamDecay(ChaosCraftPlugin p){super(p,new AttackConfig("dream_decay",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_DEEPSLATE_BREAK,1.0f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            Material[] decayMats={Material.DEEPSLATE,Material.STONE,Material.DIRT,Material.SAND,Material.NETHERRACK};
            if(t%3==0)for(int i=0;i<8;i++){double x=c.getX()+Math.random()*16-8,z=c.getZ()+Math.random()*16-8,y=c.getY()+Math.random()*5;
                w.spawnParticle(Particle.BLOCK,new Location(w,x,y,z),5,0.3,0.3,0.3,0.02,decayMats[(int)(Math.random()*decayMats.length)].createBlockData());}
            if(t%2==0)for(int i=0;i<5;i++){w.spawnParticle(Particle.SMOKE,c.clone().add(Math.random()*14-7,Math.random()*4,Math.random()*14-7),2,0.3,0.3,0.3,0.01);
                DisplayBuilder.dustParticles(c.clone().add(Math.random()*12-6,Math.random()*3,Math.random()*12-6),1,0.3,80,60,50,1.5f);}
            if(t%15==0)DisplayBuilder.playSound(c,Sound.BLOCK_DEEPSLATE_BREAK,0.4f,0.3f+(float)(Math.random()*0.3));}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DreamDecay(plugin);}
    }

    // 12. PHANTOM TOUCH — Brief velocity impulses like being poked/shoved by invisible hands
    public static class PhantomTouch extends EnvironmentalAttack {
        public PhantomTouch(ChaosCraftPlugin p){super(p,new AttackConfig("phantom_touch",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_VEX_AMBIENT,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%15==0)for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=64){
                Vector shove=new Vector((Math.random()-0.5)*0.3,0.1,(Math.random()-0.5)*0.3);p.setVelocity(p.getVelocity().add(shove));
                // "Touch" particles at player
                w.spawnParticle(Particle.SOUL,p.getLocation().clone().add(Math.random()*2-1,1,Math.random()*2-1),3,0.2,0.3,0.2,0.02);
                DisplayBuilder.playSound(p.getLocation(),Sound.ENTITY_VEX_AMBIENT,0.2f,0.5f);}}
            if(t%6==0)w.spawnParticle(Particle.SMOKE,c.clone().add(Math.random()*8-4,1,Math.random()*8-4),2,0.3,0.3,0.3,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new PhantomTouch(plugin);}
    }

    // 13. DARK RESONANCE — Deep vibrating bass, concentric dark rings expand from center
    public static class DarkResonance extends EnvironmentalAttack {
        private float resonancePhase=0;
        public DarkResonance(ChaosCraftPlugin p){super(p,new AttackConfig("dark_resonance",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.3f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            resonancePhase+=0.08f;
            // Multiple expanding rings at different phases
            for(int ring=0;ring<3;ring++){float r=(resonancePhase+ring*4)%12;
                DisplayBuilder.particleRing(c.clone().add(0,0.3+ring*0.5,0),r,Particle.DUST,(int)(r*4),
                        new Particle.DustOptions(Color.fromRGB(30+ring*10,15,40+ring*15),1.5f));}
            // Screen shake on bass hit
            if(t%25==0){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.2f,0.3f);
                for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=100)
                    p.setVelocity(p.getVelocity().add(new Vector(0,0.05,0)));}}
            if(t%3==0)w.spawnParticle(Particle.SMOKE,c.clone().add(0,0.5,0),5,3,0.3,3,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DarkResonance(plugin);}
    }
}
