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
 * Devil's Dream — ADAPTATION RESPONSE ENVIRONMENTAL ATTACKS
 * 13 attacks that respond to player behavior patterns.
 * The dream fights back against what players do most.
 */
public class AdaptationResponses {
    private AdaptationResponses() {}
    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SprintPunish(plugin)); registry.register(new MiningBacklash(plugin)); registry.register(new BuildCollapse(plugin));
        registry.register(new AggressionMirror(plugin)); registry.register(new StillnessGrasp(plugin)); registry.register(new FearEscalation(plugin));
        registry.register(new FlightDenial(plugin)); registry.register(new ParanoiaWatcher(plugin)); registry.register(new HyperAdaptation(plugin));
        registry.register(new DreamLearning(plugin)); registry.register(new PatternBreaker(plugin)); registry.register(new NightmareMemory(plugin));
        registry.register(new DreamFeedback(plugin));
    }

    // 1. SPRINT PUNISH — Dream punishes sprinting. Ground erupts fire where player runs
    public static class SprintPunish extends EnvironmentalAttack {
        private final List<Location> sprintTrail=new ArrayList<>();
        public SprintPunish(ChaosCraftPlugin p){super(p,new AttackConfig("sprint_punish",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_BLAZE_SHOOT,0.6f,0.5f);
            c.getWorld().spawnParticle(Particle.FLAME,c,15,1,0.5,1,0.05);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%2==0){sprintTrail.add(c.clone());if(sprintTrail.size()>50)sprintTrail.remove(0);}
            for(int i=0;i<sprintTrail.size();i++){Location tp=sprintTrail.get(i);float age=(float)i/sprintTrail.size();
                if(t%3==0){w.spawnParticle(Particle.FLAME,tp.clone().add(0,0.2,0),(int)(3*age),0.3,0.3+age,0.3,0.02);
                    if(age>0.3)DisplayBuilder.dustParticles(tp.clone().add(0,0.3,0),1,0.3,255,(int)(100*age),20,1.2f);}}
            // Warning symbol above player
            if(t%10==0)DisplayBuilder.particleRing(c.clone().add(0,2.5,0),1.0,Particle.SOUL_FIRE_FLAME,8,null);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_FIRE_AMBIENT,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();sprintTrail.clear();}@Override public AbstractAttack newInstance(){return new SprintPunish(plugin);}
    }

    // 2. MINING BACKLASH — Shockwave erupts when players mine. Ground particles explode outward
    public static class MiningBacklash extends EnvironmentalAttack {
        public MiningBacklash(ChaosCraftPlugin p){super(p,new AttackConfig("mining_backlash",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_GENERIC_EXPLODE,0.6f,0.6f);
            c.getWorld().spawnParticle(Particle.BLOCK,c,20,1,0.5,1,0.1,Material.STONE.createBlockData());}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Periodic shockwave rings
            if(t%20==0){for(float r=1;r<=6;r+=1.5f)DisplayBuilder.particleRing(c.clone().add(0,0.3,0),r,Particle.BLOCK,(int)(r*6),
                    Material.STONE.createBlockData());
                w.spawnParticle(Particle.SMOKE,c,10,2,0.5,2,0.03);DisplayBuilder.playSound(c,Sound.BLOCK_STONE_BREAK,0.6f,0.5f);}
            // Rumbling particles
            if(t%4==0)for(int i=0;i<5;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*5;
                w.spawnParticle(Particle.BLOCK,c.clone().add(Math.cos(a)*d,0.2,Math.sin(a)*d),3,0.2,0.1,0.2,0.02,Material.DEEPSLATE.createBlockData());}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new MiningBacklash(plugin);}
    }

    // 3. BUILD COLLAPSE — Structures players build get "rejected". Falling block particles
    public static class BuildCollapse extends EnvironmentalAttack {
        public BuildCollapse(ChaosCraftPlugin p){super(p,new AttackConfig("build_collapse",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WITHER_BREAK_BLOCK,0.6f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Periodic collapse waves
            if(t%25==0){Location collapseAt=c.clone().add(Math.random()*8-4,0,Math.random()*8-4);
                for(double y=5;y>=0;y-=0.5)w.spawnParticle(Particle.BLOCK,collapseAt.clone().add(0,y,0),5,0.5,0.2,0.5,0.05,
                        Material.OAK_PLANKS.createBlockData());
                w.spawnParticle(Particle.SMOKE,collapseAt,10,1,1,1,0.03);DisplayBuilder.playSound(collapseAt,Sound.BLOCK_WOOD_BREAK,0.6f,0.4f);}
            if(t%5==0)w.spawnParticle(Particle.SMOKE,c.clone().add(Math.random()*8-4,2,Math.random()*8-4),2,0.3,0.5,0.3,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new BuildCollapse(plugin);}
    }

    // 4. AGGRESSION MIRROR — Damage reflected back. Red mirror particles flash near attacker
    public static class AggressionMirror extends EnvironmentalAttack {
        public AggressionMirror(ChaosCraftPlugin p){super(p,new AttackConfig("aggression_mirror",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_GLASS_BREAK,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Pulsing red mirror effect
            if(t%4==0){DisplayBuilder.particleRing(c.clone().add(0,1.5,0),4.0,Particle.DUST,(int)20,
                    new Particle.DustOptions(Color.fromRGB(220,30,30),1.5f));
                for(int i=0;i<5;i++)w.spawnParticle(Particle.END_ROD,c.clone().add(Math.random()*6-3,Math.random()*3,Math.random()*6-3),1,0.1,0.1,0.1,0.01);}
            // Flash when "reflecting"
            if(t%20==0){w.spawnParticle(Particle.END_ROD,c.clone().add(0,2,0),15,3,1,3,0.02);
                DisplayBuilder.playSound(c,Sound.BLOCK_GLASS_BREAK,0.3f,0.5f);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new AggressionMirror(plugin);}
    }

    // 5. STILLNESS GRASP — Dream grabs players who stand still. Dark tendrils rise from feet
    public static class StillnessGrasp extends EnvironmentalAttack {
        public StillnessGrasp(ChaosCraftPlugin p){super(p,new AttackConfig("stillness_grasp",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Tendrils reaching up from the ground
            if(t%3==0)for(int i=0;i<6;i++){double a=(2*Math.PI*i)/6+t*0.02;double d=1.5;
                for(double y=0;y<2+Math.sin(t*0.05+i)*1;y+=0.3){
                    DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,y,Math.sin(a)*d),1,0.1,20,10,30,1.5f);
                    w.spawnParticle(Particle.SMOKE,c.clone().add(Math.cos(a)*d,y,Math.sin(a)*d),1,0.1,0.1,0.1,0.005);}}
            // Pull toward center if close
            for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=25)
                p.setVelocity(p.getVelocity().add(new Vector(0,-0.08,0)));}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_DIG,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new StillnessGrasp(plugin);}
    }

    // 6. FEAR ESCALATION — Intensity ramps over time. Particles get denser, damage increases
    public static class FearEscalation extends EnvironmentalAttack {
        private float fear=0;
        public FearEscalation(ChaosCraftPlugin p){super(p,new AttackConfig("fear_escalation",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_HEARTBEAT,0.5f,0.5f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(fear<1.0f)fear+=0.002f;int count=(int)(fear*20);
            for(int i=0;i<count;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*8;
                DisplayBuilder.dustParticles(c.clone().add(Math.cos(a)*d,Math.random()*4,Math.sin(a)*d),1,0.3,(int)(50+fear*200),(int)(10+fear*30),(int)(20+fear*50),1.0f+fear);}
            if(t%4==0&&fear>0.3f)for(int i=0;i<(int)(fear*8);i++)w.spawnParticle(Particle.SMOKE,
                    c.clone().add(Math.random()*12-6,Math.random()*4,Math.random()*12-6),1,0.3,0.3,0.3,0.02);
            if(fear>0.5f&&t%(int)(30-fear*20)==0)DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_HEARTBEAT,fear*0.5f,0.3f+fear*0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new FearEscalation(plugin);}
    }

    // 7. FLIGHT DENIAL — Dream prevents escape upward. Downward force when too high
    public static class FlightDenial extends EnvironmentalAttack {
        public FlightDenial(ChaosCraftPlugin p){super(p,new AttackConfig("flight_denial",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_SHULKER_BULLET_HIT,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Ceiling of dark particles
            if(t%3==0)for(int i=0;i<10;i++){double x=c.getX()+Math.random()*16-8;double z=c.getZ()+Math.random()*16-8;
                DisplayBuilder.dustParticles(new Location(w,x,c.getY()+8,z),1,0.3,20,10,30,2.0f);
                w.spawnParticle(Particle.SMOKE,new Location(w,x,c.getY()+8,z),1,0.5,0.1,0.5,0.005);}
            // Push down players who fly too high
            for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().getY()>c.getY()+6&&p.getLocation().distanceSquared(c)<256){
                p.setVelocity(p.getVelocity().add(new Vector(0,-0.3,0)));
                w.spawnParticle(Particle.REVERSE_PORTAL,p.getLocation(),5,0.3,0.5,0.3,0.1);}}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.ENTITY_SHULKER_BULLET_HIT,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new FlightDenial(plugin);}
    }

    // 8. PARANOIA WATCHER — Multiple "eyes" spawn at edges, staring inward
    public static class ParanoiaWatcher extends EnvironmentalAttack {
        private final List<double[]> eyes=new ArrayList<>();
        public ParanoiaWatcher(ChaosCraftPlugin p){super(p,new AttackConfig("paranoia_watcher",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){for(int i=0;i<10;i++){double a=(2*Math.PI*i)/10;
            eyes.add(new double[]{Math.cos(a)*10,4+Math.random()*3,Math.sin(a)*10});}
            DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(double[] eye:eyes){Location eLoc=c.clone().add(eye[0],eye[1],eye[2]);
                if(t%3==0){w.spawnParticle(Particle.END_ROD,eLoc.clone().add(-0.2,0,0),1,0.02,0.02,0.02,0.003);
                    w.spawnParticle(Particle.END_ROD,eLoc.clone().add(0.2,0,0),1,0.02,0.02,0.02,0.003);
                    DisplayBuilder.dustParticles(eLoc,2,0.15,220,180,50,1.0f);}
                // Gaze line toward center
                if(t%8==0)DisplayBuilder.particleLine(eLoc,c.clone().add(0,1.5,0),Particle.DUST,1,
                        new Particle.DustOptions(Color.fromRGB(200,180,50),0.5f));}
            if(t%60==0)DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_STARE,0.2f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();eyes.clear();}@Override public AbstractAttack newInstance(){return new ParanoiaWatcher(plugin);}
    }

    // 9. HYPER ADAPTATION — Dream matches everything. Multiple particle types cycle rapidly
    public static class HyperAdaptation extends EnvironmentalAttack {
        public HyperAdaptation(ChaosCraftPlugin p){super(p,new AttackConfig("hyper_adaptation",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WITHER_SPAWN,0.5f,0.5f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            Particle[] particles={Particle.FLAME,Particle.SOUL_FIRE_FLAME,Particle.REVERSE_PORTAL,Particle.END_ROD,Particle.SMOKE,Particle.SOUL};
            Particle current=particles[(t/10)%particles.length];
            for(int i=0;i<15;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*8;
                w.spawnParticle(current,c.clone().add(Math.cos(a)*d,Math.random()*4,Math.sin(a)*d),2,0.3,0.3,0.3,0.02);}
            Color[] cols={Color.fromRGB(255,50,50),Color.fromRGB(50,200,255),Color.fromRGB(200,50,255),Color.fromRGB(255,200,50)};
            if(t%3==0)for(int i=0;i<8;i++)DisplayBuilder.dustParticles(c.clone().add(Math.random()*12-6,Math.random()*4,Math.random()*12-6),
                    1,0.3,cols[(t/10)%4].getRed(),cols[(t/10)%4].getGreen(),cols[(t/10)%4].getBlue(),1.5f);
            if(t%10==0){Sound[] sounds={Sound.BLOCK_FIRE_AMBIENT,Sound.ENTITY_VEX_AMBIENT,Sound.BLOCK_SOUL_SAND_BREAK,Sound.ENTITY_ENDERMAN_TELEPORT};
                DisplayBuilder.playSound(c,sounds[(t/10)%sounds.length],0.3f,0.5f);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new HyperAdaptation(plugin);}
    }

    // 10. DREAM LEARNING — Targeted damage zones appear where the player has been standing most
    public static class DreamLearning extends EnvironmentalAttack {
        private final List<Location> hotspots=new ArrayList<>();
        public DreamLearning(ChaosCraftPlugin p){super(p,new AttackConfig("dream_learning",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_SCULK_SENSOR_CLICKING,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Track positions every 20 ticks
            if(t%20==0){hotspots.add(c.clone());if(hotspots.size()>15)hotspots.remove(0);}
            // Mark hotspots with damage zones
            for(Location hs:hotspots){if(t%4==0){DisplayBuilder.particleRing(hs.clone().add(0,0.2,0),2.0,Particle.DUST,10,
                    new Particle.DustOptions(Color.fromRGB(200,50,200),1.2f));
                w.spawnParticle(Particle.ENCHANT,hs.clone().add(0,1,0),3,0.5,0.5,0.5,0.3);}}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.BLOCK_SCULK_SENSOR_CLICKING,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();hotspots.clear();}@Override public AbstractAttack newInstance(){return new DreamLearning(plugin);}
    }

    // 11. PATTERN BREAKER — Sudden random effects that disrupt routine play
    public static class PatternBreaker extends EnvironmentalAttack {
        public PatternBreaker(ChaosCraftPlugin p){super(p,new AttackConfig("pattern_breaker",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,0.8f,0.5f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%25==0){int effect=(int)(Math.random()*4);Location effectLoc=c.clone().add(Math.random()*8-4,0,Math.random()*8-4);
                switch(effect){case 0:// Gravity burst up
                    for(Player p:w.getPlayers()){if(!isExempt(p)&&p.getLocation().distanceSquared(effectLoc)<=16)p.setVelocity(p.getVelocity().add(new Vector(0,1,0)));}
                    w.spawnParticle(Particle.REVERSE_PORTAL,effectLoc,15,1,1,1,0.1);break;
                case 1:// Random push
                    Vector push=new Vector((Math.random()-0.5)*0.8,0.2,(Math.random()-0.5)*0.8);
                    for(Player p:w.getPlayers()){if(!isExempt(p)&&p.getLocation().distanceSquared(c)<=64)p.setVelocity(p.getVelocity().add(push));}
                    w.spawnParticle(Particle.SMOKE,c,15,3,1,3,0.05);break;
                case 2:// Flash
                    w.spawnParticle(Particle.END_ROD,effectLoc.clone().add(0,3,0),30,4,2,4,0.02);
                    DisplayBuilder.playSound(effectLoc,Sound.ENTITY_LIGHTNING_BOLT_IMPACT,0.5f,0.5f);break;
                case 3:// Ground eruption
                    w.spawnParticle(Particle.FLAME,effectLoc,15,0.5,1.5,0.5,0.05);
                    DisplayBuilder.playSound(effectLoc,Sound.ENTITY_BLAZE_SHOOT,0.4f,0.5f);break;}}
            if(t%5==0)w.spawnParticle(Particle.PORTAL,c.clone().add(Math.random()*10-5,2,Math.random()*10-5),3,0.3,0.3,0.3,0.5);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new PatternBreaker(plugin);}
    }

    // 12. NIGHTMARE MEMORY — Past attack echoes replay as faint particle ghosts
    public static class NightmareMemory extends EnvironmentalAttack {
        public NightmareMemory(ChaosCraftPlugin p){super(p,new AttackConfig("nightmare_memory",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_RESPAWN_ANCHOR_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            int memoryType=(t/40)%4;Location memLoc=c.clone().add(Math.sin(t*0.03)*5,0,Math.cos(t*0.03)*5);
            if(t%3==0)switch(memoryType){
                case 0:DisplayBuilder.particleRing(memLoc.clone().add(0,0.3,0),3.0,Particle.SOUL,12,null);break;
                case 1:for(double y=0;y<5;y+=0.5)w.spawnParticle(Particle.REVERSE_PORTAL,memLoc.clone().add(0,y,0),2,0.2,0.1,0.2,0.03);break;
                case 2:for(int i=0;i<6;i++){double a=(2*Math.PI*i)/6;w.spawnParticle(Particle.SOUL_FIRE_FLAME,memLoc.clone().add(Math.cos(a)*2,1,Math.sin(a)*2),1,0.1,0.1,0.1,0.01);}break;
                case 3:w.spawnParticle(Particle.SMOKE,memLoc,8,1.5,1,1.5,0.02);break;}
            if(t%40==0)DisplayBuilder.playSound(c,Sound.ENTITY_VEX_AMBIENT,0.2f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new NightmareMemory(plugin);}
    }

    // 13. DREAM FEEDBACK — The more damage taken, the more particles. Escalating visual chaos
    public static class DreamFeedback extends EnvironmentalAttack {
        private float feedbackLevel=0;
        public DreamFeedback(ChaosCraftPlugin p){super(p,new AttackConfig("dream_feedback",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_BEACON_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(feedbackLevel<1.0f)feedbackLevel+=0.003f;
            int intensity=(int)(feedbackLevel*30);
            for(int i=0;i<intensity;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*10;double y=Math.random()*5;
                Particle p=(Math.random()<0.5)?Particle.SOUL_FIRE_FLAME:Particle.REVERSE_PORTAL;
                w.spawnParticle(p,c.clone().add(Math.cos(a)*d,y,Math.sin(a)*d),1,0.2,0.2,0.2,0.03);}
            if(feedbackLevel>0.5f&&t%3==0)for(int i=0;i<(int)(feedbackLevel*10);i++)
                DisplayBuilder.dustParticles(c.clone().add(Math.random()*14-7,Math.random()*5,Math.random()*14-7),
                        1,0.3,(int)(feedbackLevel*255),(int)(50-feedbackLevel*30),(int)(feedbackLevel*200),1.5f);
            if(feedbackLevel>0.7f&&t%2==0)w.spawnParticle(Particle.END_ROD,c.clone().add(Math.random()*12-6,Math.random()*6,Math.random()*12-6),2,0.2,0.2,0.2,0.02);
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_BEACON_AMBIENT,feedbackLevel*0.5f,0.3f+feedbackLevel*0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new DreamFeedback(plugin);}
    }
}
