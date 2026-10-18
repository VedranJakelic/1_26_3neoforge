package net.beamex.shamaschizm.elf;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.effect.ModEffects;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class ElfConversations {
    private static final String[] PROMPTS={
        "Hello stranger! You seem lost! [Y/N]",
        "Follow us, we will show you the fallen garden of Eden! [Y/N]",
        "If you are lost, you can always follow us, we know where you want to go! [Y/N]",
        "If you encounter the beast from Hell, crouch. It might not hear you! [Y/N]",
        "The frogs sometimes demand emeralds as food! [Y/N]",
        "Beware of the smiling waters, they have a very toothy grin! [Y/N]",
        "You should open your third eye! [Y/N]"
    };
    private record Session(ElfEntity elf,int prompt,long started,MobEffectInstance previous){}
    private static final Map<UUID,Session> SESSIONS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID,Long> COOLDOWN=new java.util.concurrent.ConcurrentHashMap<>();
    private static long now(ServerPlayer p){return p.level().getServer().overworld().getGameTime();}
    public static boolean busy(ElfEntity elf){return SESSIONS.values().stream().anyMatch(s->s.elf==elf);}
    private static boolean tripping(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&p.hasEffect(ModEffects.TRIPPING);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        Session session=SESSIONS.get(p.getUUID());
        if(session!=null){
            ElfEntity elf=session.elf;
            if(!tripping(p)||elf.isRemoved()||elf.level()!=p.level()||p.distanceToSqr(elf)>64){release(p);return;}
            p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,8,9,false,false,false));
            p.lookAt(EntityAnchorArgument.Anchor.EYES,elf.position().add(0,0.85,0));
            elf.getLookControl().setLookAt(p,180,180);
            float yaw=(float)(Math.atan2(p.getZ()-elf.getZ(),p.getX()-elf.getX())*180/Math.PI)-90;
            elf.setYRot(yaw);elf.yBodyRot=yaw;elf.yHeadRot=yaw;
            return;
        }
        if(!tripping(p)||p.tickCount%2!=0||now(p)<COOLDOWN.getOrDefault(p.getUUID(),0L))return;
        ElfEntity best=null;double nearest=Double.MAX_VALUE;
        for(ElfEntity elf:p.level().getEntitiesOfClass(ElfEntity.class,p.getBoundingBox().inflate(5))){
            if(busy(elf)||p.distanceToSqr(elf)>25||!p.hasLineOfSight(elf))continue;
            Vec3 delta=elf.position().add(0,0.85,0).subtract(p.getEyePosition());
            double d=delta.length();
            if(!elf.getBoundingBox().inflate(0.10).contains(p.getEyePosition())
                    &&elf.getBoundingBox().inflate(0.10).clip(p.getEyePosition(),
                            p.getEyePosition().add(p.getLookAngle().scale(5))).isEmpty())continue;
            if(d<nearest){best=elf;nearest=d;}
        }
        if(best==null)return;
        int prompt=p.getRandom().nextInt(PROMPTS.length);
        var previous=p.getEffect(MobEffects.SLOWNESS);
        SESSIONS.put(p.getUUID(),new Session(best,prompt,now(p),previous==null?null:new MobEffectInstance(previous)));
        best.getNavigation().stop();PacketDistributor.sendToPlayer(p,new ElfNetwork.Lock(best.getId()));
        p.lookAt(EntityAnchorArgument.Anchor.EYES,best.position().add(0,0.85,0));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,8,9,false,false,false));
        p.sendSystemMessage(Component.literal(PROMPTS[prompt]));
    }
    @SubscribeEvent public static void chat(ServerChatEvent event){
        ServerPlayer p=event.getPlayer();Session s=SESSIONS.get(p.getUUID());if(s==null)return;
        String answer=event.getRawText().trim().toLowerCase(Locale.ROOT);
        if(!Set.of("y","yes","n","no").contains(answer))return;
        // Keep the original signed player message in ordinary chat; only prompts are private.
        // Chat may be delivered through an asynchronous pipeline. Mutate the world on the server thread.
        p.level().getServer().execute(() -> {
            if(SESSIONS.get(p.getUUID())!=s)return;
            boolean valid=tripping(p)&&s.elf.level()==p.level()&&!s.elf.isRemoved();
            release(p);
            if(valid&&s.prompt==6&&(answer.equals("n")||answer.equals("no"))){
                p.sendSystemMessage(Component.literal("You wont? Then we should close the other two!"));
                punish(p);
            }
        });
    }
    private static void release(ServerPlayer p){
        Session s=SESSIONS.remove(p.getUUID());if(s==null)return;
        PacketDistributor.sendToPlayer(p,new ElfNetwork.Lock(-1));COOLDOWN.put(p.getUUID(),now(p)+60);
        var current=p.getEffect(MobEffects.SLOWNESS);
        if(current!=null&&current.getAmplifier()==9&&current.getDuration()<=8){
            p.removeEffect(MobEffects.SLOWNESS);
            if(s.previous!=null&&p.isAlive()){
                var old=s.previous;int duration=old.isInfiniteDuration()?-1:old.getDuration()-(int)(now(p)-s.started);
                if(duration==-1||duration>0)p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,duration,old.getAmplifier(),old.isAmbient(),old.isVisible(),old.showIcon()));
            }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p){release(p);COOLDOWN.remove(p.getUUID());}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){SESSIONS.clear();COOLDOWN.clear();}
    private static void punish(ServerPlayer p){
        ServerLevel level=p.level();Vec3 origin=p.position();BlockPos center=p.blockPosition();
        level.sendParticles(ParticleTypes.LARGE_SMOKE,p.getX(),p.getY()+0.7,p.getZ(),70,0.4,0.65,0.4,0.04);
        for(int attempt=0;attempt<700;attempt++){
            int dx=level.getRandom().nextInt(201)-100,dz=level.getRandom().nextInt(201)-100;
            if(dx*dx+dz*dz>10000)continue;
            int first=level.getRandom().nextInt(201)-100;
            for(int j=0;j<201;j++){
                int dy=-100+Math.floorMod(first+100+j,201);BlockPos pos=center.offset(dx,dy,dz);
                Vec3 dest=Vec3.atBottomCenterOf(pos);double distance=dest.distanceToSqr(origin);
                if(distance<=400||distance>10000||!safe(level,p,pos))continue;
                p.teleportTo(level,dest.x,dest.y,dest.z,Set.of(),p.getYRot(),p.getXRot(),true);
                p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
                level.sendParticles(ParticleTypes.LARGE_SMOKE,dest.x,dest.y+0.7,dest.z,50,0.4,0.65,0.4,0.04);return;
            }
        }
        // Never substitute an unsafe or out-of-range destination when none exists.
        p.sendSystemMessage(Component.literal("The darkness has nowhere to take you."));
    }
    private static boolean safe(ServerLevel level,ServerPlayer p,BlockPos pos){
        if(!level.hasChunkAt(pos)||pos.getY()<=level.getMinY()||pos.getY()+2>=level.getMaxY()
                ||!level.getWorldBorder().isWithinBounds(pos)||level.getMaxLocalRawBrightness(pos)>3)return false;
        if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir())return false;
        var below=pos.below();var floor=level.getBlockState(below);
        if(!floor.isFaceSturdy(level,below,Direction.UP)||floor.is(Blocks.MAGMA_BLOCK)||floor.is(Blocks.CACTUS)
                ||floor.is(Blocks.CAMPFIRE)||floor.is(Blocks.SOUL_CAMPFIRE))return false;
        var box=p.getBoundingBox().move(Vec3.atBottomCenterOf(pos).subtract(p.position()));
        return level.noCollision(p,box)&&!level.containsAnyLiquid(box);
    }
}
