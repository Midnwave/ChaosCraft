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

/** Devil's Dream — SOUL HARVESTING ENVIRONMENTAL ATTACKS. 13 spirit/soul themed effects. */
public class SoulHarvesting {
    private SoulHarvesting() {}
    public static void registerAll(ChaosCraftPlugin plugin, AttackRegistry registry) {
        registry.register(new SoulDrain(plugin)); registry.register(new GhostTouch(plugin)); registry.register(new SpiritSwarm(plugin));
        registry.register(new SoulSiphon(plugin)); registry.register(new ReaperMark(plugin)); registry.register(new SoulStorm(plugin));
        registry.register(new PhantomChains(plugin)); registry.register(new SpiritBurst(plugin)); registry.register(new GhostWail(plugin));
        registry.register(new SoulTrap(plugin)); registry.register(new AncestralWrath(plugin)); registry.register(new EctoplasmicWave(plugin));
        registry.register(new SoulFunnel(plugin));
    }

    // 1. SOUL DRAIN — Soul particles pulled from players toward a central vortex
    public static class SoulDrain extends EnvironmentalAttack {
        public SoulDrain(ChaosCraftPlugin p){super(p,new AttackConfig("soul_drain",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.4f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Soul particles spiral inward from player positions
            for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=64){
                if(t%3==0){Location pLoc=p.getLocation().clone().add(0,1,0);
                    // Line of souls from player to center
                    DisplayBuilder.particleLine(pLoc,c.clone().add(0,2,0),Particle.SOUL,2,null);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,pLoc,2,0.3,0.5,0.3,0.02);}}}
            // Central vortex
            if(t%2==0){double a=t*0.15;for(int i=0;i<8;i++){double angle=a+i*(Math.PI/4);double r=0.5+i*0.2;
                w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(angle)*r,2+i*0.2,Math.sin(angle)*r),1,0.05,0.05,0.05,0.01);}}
            // Pull players slightly toward center
            for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(c);if(d<8&&d>1)
                p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.04)));}
            if(t%25==0)DisplayBuilder.playSound(c,Sound.ENTITY_VEX_AMBIENT,0.4f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SoulDrain(plugin);}
    }

    // 2. GHOST TOUCH — Invisible hands shove players, soul particles appear at contact point
    public static class GhostTouch extends EnvironmentalAttack {
        public GhostTouch(ChaosCraftPlugin p){super(p,new AttackConfig("ghost_touch",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(6.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(20);config.setDurationTicks(350);config.setCooldownTicks(280);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_VEX_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%18==0)for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=64){
                Vector shove=new Vector((Math.random()-0.5)*0.4,0.1,(Math.random()-0.5)*0.4);p.setVelocity(p.getVelocity().add(shove));
                Location touchLoc=p.getLocation().clone().add((Math.random()-0.5)*0.8,0.5+Math.random(),(Math.random()-0.5)*0.8);
                w.spawnParticle(Particle.SOUL,touchLoc,5,0.2,0.2,0.2,0.02);
                DisplayBuilder.playSound(touchLoc,Sound.ENTITY_VEX_AMBIENT,0.2f,0.5f+(float)(Math.random()*0.3));}}
            if(t%5==0){double a=Math.random()*Math.PI*2;double d=Math.random()*6;
                w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(a)*d,0.5+Math.random()*2,Math.sin(a)*d),1,0.1,0.2,0.1,0.01);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new GhostTouch(plugin);}
    }

    // 3. SPIRIT SWARM — Cloud of small soul wisps orbiting chaotically
    public static class SpiritSwarm extends EnvironmentalAttack {
        private final List<double[]> wisps=new ArrayList<>();
        public SpiritSwarm(ChaosCraftPlugin p){super(p,new AttackConfig("spirit_swarm",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){for(int i=0;i<15;i++)wisps.add(new double[]{Math.random()*Math.PI*2,2+Math.random()*5,1+Math.random()*3,0.03+Math.random()*0.06});
            DisplayBuilder.playSound(c,Sound.ENTITY_VEX_CHARGE,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(double[] wisp:wisps){double angle=wisp[0]+t*wisp[3];double r=wisp[1];double h=wisp[2];
                Location wLoc=c.clone().add(Math.cos(angle)*r,h+Math.sin(t*0.04+wisp[0])*1.5,Math.sin(angle)*r);
                if(t%2==0){w.spawnParticle(Particle.SOUL_FIRE_FLAME,wLoc,1,0.05,0.05,0.05,0.005);
                    w.spawnParticle(Particle.SOUL,wLoc,1,0.1,0.1,0.1,0.01);}}
            if(t%30==0)DisplayBuilder.playSound(c,Sound.ENTITY_VEX_CHARGE,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();wisps.clear();}@Override public AbstractAttack newInstance(){return new SpiritSwarm(plugin);}
    }

    // 4. SOUL SIPHON — Beam of soul particles connects to nearest player, draining
    public static class SoulSiphon extends EnvironmentalAttack {
        public SoulSiphon(ChaosCraftPlugin p){super(p,new AttackConfig("soul_siphon",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(10);config.setDurationTicks(300);config.setCooldownTicks(280);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_BEACON_DEACTIVATE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            Location siphonSource=c.clone().add(0,4,0);
            // Siphon beam to center
            if(t%2==0){for(Player p:w.getPlayers()){if(isExempt(p))continue;if(p.getLocation().distanceSquared(c)<=36){
                DisplayBuilder.particleLine(p.getLocation().clone().add(0,1,0),siphonSource,Particle.SOUL_FIRE_FLAME,3,null);}}
                // Collecting orb at source
                w.spawnParticle(Particle.SOUL,siphonSource,3,0.3,0.3,0.3,0.02);
                DisplayBuilder.dustParticles(siphonSource,2,0.5,80,200,220,1.5f);}
            // Pulsing glow at source
            if(t%6==0)w.spawnParticle(Particle.END_ROD,siphonSource,2,0.2,0.2,0.2,0.01);
            if(t%20==0)DisplayBuilder.playSound(siphonSource,Sound.BLOCK_BEACON_AMBIENT,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SoulSiphon(plugin);}
    }

    // 5. REAPER MARK — Skull particle crosshair appears on random player, damage pulse
    public static class ReaperMark extends EnvironmentalAttack {
        public ReaperMark(ChaosCraftPlugin p){super(p,new AttackConfig("reaper_mark",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(10.0);config.setDamageRadius(4.0);config.setTicksBetweenDamage(30);config.setDurationTicks(300);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WITHER_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Mark crosshair on center
            if(t%3==0){for(int i=-2;i<=2;i++){w.spawnParticle(Particle.SOUL_FIRE_FLAME,c.clone().add(i*0.5,2,0),1,0.02,0.02,0.02,0.005);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME,c.clone().add(0,2,i*0.5),1,0.02,0.02,0.02,0.005);}
                DisplayBuilder.particleRing(c.clone().add(0,2,0),1.5,Particle.SOUL,8,null);}
            // Death pulse every 30 ticks
            if(t%30==0){w.spawnParticle(Particle.SOUL,c.clone().add(0,1,0),20,2,1,2,0.05);
                DisplayBuilder.playSound(c,Sound.ENTITY_WITHER_HURT,0.5f,0.4f);
                for(float r=0.5f;r<=4;r+=1)DisplayBuilder.particleRing(c.clone().add(0,0.3,0),r,Particle.SOUL_FIRE_FLAME,(int)(r*6),null);}
            if(t%40==0)DisplayBuilder.playSound(c,Sound.ENTITY_WITHER_AMBIENT,0.3f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new ReaperMark(plugin);}
    }

    // 6. SOUL STORM — Dense swirling soul particles like a hurricane
    public static class SoulStorm extends EnvironmentalAttack {
        public SoulStorm(ChaosCraftPlugin p){super(p,new AttackConfig("soul_storm",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_ROAR,0.5f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            for(int layer=0;layer<6;layer++){double h=layer*1.2;double r=2+layer*1.2;
                for(int i=0;i<8;i++){double a=t*0.08+i*(Math.PI/4)+layer*0.5;
                    w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r),1,0.2,0.2,0.2,0.02);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,c.clone().add(Math.cos(a+0.5)*r,h+0.3,Math.sin(a+0.5)*r),1,0.1,0.1,0.1,0.01);}}
            // Wind push inward
            for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(c);if(d<10&&d>1)
                p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.06)));}
            if(t%15==0)DisplayBuilder.playSound(c,Sound.ENTITY_PHANTOM_FLAP,0.5f,0.3f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SoulStorm(plugin);}
    }

    // 7. PHANTOM CHAINS — Spectral chain links form around player, constricting
    public static class PhantomChains extends EnvironmentalAttack {
        private float chainR=6;
        public PhantomChains(ChaosCraftPlugin p){super(p,new AttackConfig("phantom_chains",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_CHAIN_PLACE,0.8f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(chainR>2)chainR-=0.012f;double spin=t*0.04;
            int links=16;for(int i=0;i<links;i++){double a=(2*Math.PI*i)/links+spin;double y=1.5+Math.sin(a*2+t*0.05)*0.8;
                Location lLoc=c.clone().add(Math.cos(a)*chainR,y,Math.sin(a)*chainR);
                if(t%2==0){DisplayBuilder.dustParticles(lLoc,2,0.1,160,180,220,1.2f);
                    w.spawnParticle(Particle.SOUL_FIRE_FLAME,lLoc,1,0.05,0.05,0.05,0.005);}}
            // Chain link connections
            if(t%4==0)for(int i=0;i<links;i++){double a1=(2*Math.PI*i)/links+spin;double a2=(2*Math.PI*(i+1))/links+spin;
                Location l1=c.clone().add(Math.cos(a1)*chainR,1.5,Math.sin(a1)*chainR);Location l2=c.clone().add(Math.cos(a2)*chainR,1.5,Math.sin(a2)*chainR);
                DisplayBuilder.particleLine(l1,l2,Particle.DUST,2,new Particle.DustOptions(Color.fromRGB(140,160,200),0.8f));}
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_CHAIN_BREAK,0.4f,0.5f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new PhantomChains(plugin);}
    }

    // 8. SPIRIT BURST — Periodic explosions of soul energy
    public static class SpiritBurst extends EnvironmentalAttack {
        public SpiritBurst(ChaosCraftPlugin p){super(p,new AttackConfig("spirit_burst",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(10.0);config.setDamageRadius(6.0);config.setTicksBetweenDamage(25);config.setDurationTicks(300);config.setCooldownTicks(280);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.4f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(t%25==0){Location burst=c.clone().add(Math.random()*8-4,1,Math.random()*8-4);
                w.spawnParticle(Particle.SOUL,burst,25,1.5,1,1.5,0.08);w.spawnParticle(Particle.SOUL_FIRE_FLAME,burst,15,1,0.5,1,0.05);
                w.spawnParticle(Particle.END_ROD,burst,10,0.5,0.5,0.5,0.05);
                for(float r=1;r<=5;r+=1.5f)DisplayBuilder.particleRing(burst,r,Particle.SOUL,(int)(r*5),null);
                DisplayBuilder.playSound(burst,Sound.ENTITY_WARDEN_SONIC_BOOM,0.4f,0.5f);
                for(Player p:w.getPlayers()){if(!isExempt(p)&&p.getLocation().distanceSquared(burst)<=25){
                    Vector push=p.getLocation().toVector().subtract(burst.toVector()).normalize().multiply(0.5).setY(0.3);
                    p.setVelocity(p.getVelocity().add(push));}}}
            if(t%4==0)w.spawnParticle(Particle.SOUL,c.clone().add(Math.random()*6-3,Math.random()*3,Math.random()*6-3),1,0.1,0.1,0.1,0.01);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SpiritBurst(plugin);}
    }

    // 9. GHOST WAIL — Eerie wailing sounds with visible sound waves (particle rings)
    public static class GhostWail extends EnvironmentalAttack {
        public GhostWail(ChaosCraftPlugin p){super(p,new AttackConfig("ghost_wail",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(10.0);config.setTicksBetweenDamage(15);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_GHAST_SCREAM,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Sound wave rings expanding outward
            float waveR=(t%30)*0.4f;if(waveR>0&&waveR<12){
                DisplayBuilder.particleRing(c.clone().add(0,2,0),waveR,Particle.DUST,(int)(waveR*5),
                        new Particle.DustOptions(Color.fromRGB(120,180,200),1.0f+(12-waveR)*0.1f));}
            // Second wave offset
            float wave2=((t+15)%30)*0.4f;if(wave2>0&&wave2<12)
                DisplayBuilder.particleRing(c.clone().add(0,2.5,0),wave2,Particle.DUST,(int)(wave2*4),
                        new Particle.DustOptions(Color.fromRGB(100,150,180),0.8f));
            // Wailing sounds
            if(t%30==0){Sound[] wails={Sound.ENTITY_GHAST_SCREAM,Sound.ENTITY_GHAST_AMBIENT,Sound.ENTITY_VEX_DEATH};
                DisplayBuilder.playSound(c.clone().add(Math.random()*8-4,2,Math.random()*8-4),wails[(t/30)%wails.length],0.4f,0.3f+(float)(Math.random()*0.3));}
            if(t%5==0)w.spawnParticle(Particle.SOUL,c.clone().add(0,2,0),2,1,0.5,1,0.02);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new GhostWail(plugin);}
    }

    // 10. SOUL TRAP — Circle of soul particles closes to trap player, then explodes
    public static class SoulTrap extends EnvironmentalAttack {
        private float trapR=8;private boolean trapped=false;
        public SoulTrap(ChaosCraftPlugin p){super(p,new AttackConfig("soul_trap",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(15);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.BLOCK_SOUL_SAND_BREAK,0.8f,0.3f);
            DisplayBuilder.particleRing(c,trapR,Particle.SOUL,24,null);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            if(!trapped){if(trapR>1.5f)trapR-=0.04f;else{trapped=true;
                w.spawnParticle(Particle.SOUL,c,30,1.5,1,1.5,0.08);DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_SONIC_BOOM,0.5f,0.5f);}}
            if(t%3==0){DisplayBuilder.particleRing(c.clone().add(0,0.3,0),trapR,Particle.SOUL_FIRE_FLAME,(int)(trapR*5),null);
                DisplayBuilder.particleRing(c.clone().add(0,1.5,0),trapR,Particle.SOUL,(int)(trapR*4),null);}
            // Push players inward
            for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(c);
                if(d>trapR&&d<trapR+2)p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.15)));}
            if(t%20==0)DisplayBuilder.playSound(c,Sound.BLOCK_SOUL_SAND_PLACE,0.4f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SoulTrap(plugin);}
    }

    // 11. ANCESTRAL WRATH — Ghostly figures rise from ground and charge outward
    public static class AncestralWrath extends EnvironmentalAttack {
        public AncestralWrath(ChaosCraftPlugin p){super(p,new AttackConfig("ancestral_wrath",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(9.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(18);config.setDurationTicks(350);config.setCooldownTicks(300);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_WARDEN_EMERGE,0.6f,0.4f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Ghost figures rise every 25 ticks
            if(t%25==0)for(int i=0;i<4;i++){double a=(2*Math.PI*i)/4+Math.random()*0.5;
                // Ghostly silhouette rising
                for(double y=0;y<2.5;y+=0.3){double r=1+t*0.01;Location gLoc=c.clone().add(Math.cos(a)*r,y,Math.sin(a)*r);
                    DisplayBuilder.dustParticles(gLoc,2,0.15,100,130,180,1.5f);w.spawnParticle(Particle.SOUL,gLoc,1,0.1,0.1,0.1,0.01);}
                DisplayBuilder.playSound(c.clone().add(Math.cos(a)*2,0,Math.sin(a)*2),Sound.ENTITY_VEX_DEATH,0.3f,0.3f);}
            // Ambient ancestral particles
            if(t%4==0)for(int i=0;i<5;i++){double a=Math.random()*Math.PI*2;double d=Math.random()*6;
                w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(a)*d,0.2+Math.random()*2,Math.sin(a)*d),1,0.1,0.2,0.1,0.01);}}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new AncestralWrath(plugin);}
    }

    // 12. ECTOPLASMIC WAVE — Green/blue translucent wave sweeps across the ground
    public static class EctoplasmicWave extends EnvironmentalAttack {
        private double wavePos=-10;
        public EctoplasmicWave(ChaosCraftPlugin p){super(p,new AttackConfig("ectoplasmic_wave",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(8.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(12);config.setDurationTicks(350);config.setCooldownTicks(280);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,0.6f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            wavePos+=0.2;if(wavePos>10)wavePos=-10;
            // Wave wall of ectoplasm
            for(int x=-8;x<=8;x+=1)for(double y=0;y<3;y+=0.5){
                Location wLoc=new Location(w,c.getX()+x,c.getY()+y,c.getZ()+wavePos);
                float waveHeight=(float)Math.exp(-0.5*y);
                DisplayBuilder.dustParticles(wLoc,1,0.3,(int)(50+waveHeight*80),(int)(180+waveHeight*40),(int)(100+waveHeight*50),1.0f+waveHeight);
                if(Math.random()<0.15)w.spawnParticle(Particle.SOUL,wLoc,1,0.1,0.1,0.1,0.01);}
            if(t%15==0)DisplayBuilder.playSound(c,Sound.ENTITY_ELDER_GUARDIAN_AMBIENT,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new EctoplasmicWave(plugin);}
    }

    // 13. SOUL FUNNEL — Inverted tornado of souls spiraling upward and dispersing
    public static class SoulFunnel extends EnvironmentalAttack {
        public SoulFunnel(ChaosCraftPlugin p){super(p,new AttackConfig("soul_funnel",AttackType.ENVIRONMENTAL,1,"modes/devilsdream/attacks"));
            config.setTracksPlayer(true);config.setDamage(7.0);config.setDamageRadius(8.0);config.setTicksBetweenDamage(12);config.setDurationTicks(400);config.setCooldownTicks(350);}
        @Override protected void onSpawn(Location c){DisplayBuilder.playSound(c,Sound.ENTITY_VEX_CHARGE,0.8f,0.3f);}
        @Override protected void onTick(int t){Location c=getCenter();if(c==null||c.getWorld()==null)return;World w=c.getWorld();
            // Inverted funnel: narrow at bottom, wide at top
            for(int layer=0;layer<10;layer++){double h=layer*1.0;double r=0.5+layer*0.6;
                for(int i=0;i<6;i++){double a=t*0.1+i*(Math.PI/3)+layer*0.3;
                    w.spawnParticle(Particle.SOUL,c.clone().add(Math.cos(a)*r,h,Math.sin(a)*r),1,0.1,0.1,0.1,0.01);
                    if(layer<4)w.spawnParticle(Particle.SOUL_FIRE_FLAME,c.clone().add(Math.cos(a+0.3)*r,h+0.2,Math.sin(a+0.3)*r),1,0.05,0.05,0.05,0.005);}}
            // Sucking particles from ground toward center
            if(t%3==0)for(int i=0;i<5;i++){double a=Math.random()*Math.PI*2;double d=3+Math.random()*5;
                DisplayBuilder.particleLine(c.clone().add(Math.cos(a)*d,0.2,Math.sin(a)*d),c.clone().add(0,0.5,0),Particle.SOUL,1,null);}
            // Pull toward center
            for(Player p:w.getPlayers()){if(isExempt(p))continue;double d=p.getLocation().distance(c);if(d<8&&d>1)
                p.setVelocity(p.getVelocity().add(c.toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.05)));}
            if(t%15==0)DisplayBuilder.playSound(c,Sound.ENTITY_VEX_CHARGE,0.3f,0.4f);}
        @Override protected void onCleanup(){super.onCleanup();}@Override public AbstractAttack newInstance(){return new SoulFunnel(plugin);}
    }
}
