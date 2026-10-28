package net.beamex.shamaschizm.localportal;

import java.util.*;
import net.beamex.shamaschizm.mixin.PortalMiningAccessor;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.*;

/** Server-authoritative actions. Never moves the player or installs remote blocks in their world. */
@EventBusSubscriber(modid="shamaschizm")
public final class PortalInteractions {
    private record Target(PortalInteractionRay.Ray ray,BlockHitResult block,Entity entity,Vec3 hit) {}
    private record Scope(ServerPlayer player,Vec3 eye,float yaw,float pitch) {}
    private record Menu(AbstractContainerMenu menu,String source,String target,BlockPos pos) {}
    private static final class State {
        net.minecraft.server.level.ServerLevel level;
        long heartbeat=-100,actionTick=-100,useTick=-100,nextMine=-100;
        BlockPos mining;
        ItemStack tool=ItemStack.EMPTY;
        
        Menu menu;
    }
    private static final Map<ServerPlayer,State> STATES=new WeakHashMap<>();
    private static final ThreadLocal<Scope> ACTIVE=new ThreadLocal<>();
    public static Vec3 eye(Entity entity){var s=ACTIVE.get();return s!=null&&s.player==entity?s.eye:null;}
    public static Float yaw(Entity entity){var s=ACTIVE.get();return s!=null&&s.player==entity?s.yaw:null;}
    public static Float pitch(Entity entity){var s=ACTIVE.get();return s!=null&&s.player==entity?s.pitch:null;}
    private static void scoped(ServerPlayer p,PortalInteractionRay.Ray ray,Runnable action){
        Vec3 look=ray.source().transformVector(ray.target(),p.getLookAngle());
        var old=ACTIVE.get();
        ACTIVE.set(new Scope(p,ray.eye(),(float)Math.toDegrees(Math.atan2(-look.x,look.z)),p.getXRot()));
        try{action.run();}finally{if(old==null)ACTIVE.remove();else ACTIVE.set(old);}
    }
    private static boolean usable(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.isRemoved()
        &&p.level().dimension().equals(Schizm.KEY)&&p.containerMenu==p.inventoryMenu&&!p.isUsingItem();}
    public static void handle(ServerPlayer p,PortalInteractionPacket packet){
        State s=STATES.computeIfAbsent(p,k->new State());
        if(s.level!=p.level()){s.level=p.level();s.mining=null;s.menu=null;s.heartbeat=-100;}
        if(packet.action()==PortalInteractionPacket.Action.RELEASE){s.heartbeat=-100;abort(p,s);return;}
        if(!usable(p)){s.heartbeat=-100;abort(p,s);return;}
        long now=p.level().getGameTime();
        if(packet.action()==PortalInteractionPacket.Action.HOLD){s.heartbeat=now;return;}
        if(packet.action()==PortalInteractionPacket.Action.USE){if(now-s.useTick<4)return;s.useTick=now;}
        else{if(now==s.actionTick)return;s.actionTick=now;}
        boolean attack=packet.action()==PortalInteractionPacket.Action.ATTACK;
        Target t=target(p,attack);
        abort(p,s);
        if(t==null)return;
        p.resetLastActionTime();
        if(attack){
            if(t.entity!=null){
                if(t.entity instanceof net.minecraft.world.entity.item.ItemEntity||t.entity instanceof net.minecraft.world.entity.ExperienceOrb)return;
                if(t.entity instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow a&&!a.isAttackable())return;
                scoped(p,t.ray,()->{
                    p.connection.handleAttack(new ServerboundAttackPacket(t.entity.getId()));
                    p.swingAndResetAttackStrength(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);
                });
            }else if(t.block.getType()==HitResult.Type.BLOCK){
                s.heartbeat=now;begin(p,s,t);
            }
        }else{
            var previous=p.containerMenu;
            scoped(p,t.ray,()->{
                for(var hand:InteractionHand.values()){
                    if(!p.getItemInHand(hand).isItemEnabled(p.level().enabledFeatures())||p.getCooldowns().isOnCooldown(p.getItemInHand(hand)))continue;
                    if(t.entity!=null){
                        var result=p.interactOn(t.entity,hand,t.hit.subtract(t.entity.position()));
                        if(result.consumesAction())break;
                    }else if(t.block.getType()==HitResult.Type.BLOCK){
                        BlockPos pos=t.block.getBlockPos();
                        if(!p.mayInteract(p.level(),pos)||!p.level().mayInteract(p,pos)
                            ||p.level().getServer().isUnderSpawnProtection(p.level(),pos,p))break;
                        var result=p.gameMode.useItemOn(p,p.level(),p.getItemInHand(hand),hand,t.block);
                        if(result.consumesAction()||result==net.minecraft.world.InteractionResult.FAIL)break;
                    }
                }
            });
            if(previous!=p.containerMenu&&t.entity==null)
                s.menu=new Menu(p.containerMenu,t.ray.source().name(),t.ray.target().name(),t.block.getBlockPos());
            p.containerMenu.broadcastChanges();
            if(t.entity==null)PortalTerrainServer.refresh(p);
        }
    }
    private static void begin(ServerPlayer p,State s,Target t){
        if(p.level().getGameTime()<s.nextMine)return;
        s.mining=t.block.getBlockPos();s.tool=p.getMainHandItem().copy();
        scoped(p,t.ray,()->p.gameMode.handleBlockBreakAction(s.mining,START_DESTROY_BLOCK,t.block.getDirection(),p.level().getMaxY(),0));
        var mining=(PortalMiningAccessor)p.gameMode;
        if(p.level().getBlockState(s.mining).isAir())PortalTerrainServer.refresh(p);
        if(p.level().getBlockState(s.mining).isAir()||!mining.portal$isDestroying()||!s.mining.equals(mining.portal$destroyPos())){s.mining=null;s.nextMine=p.level().getGameTime()+5;}
    }
    private static Target target(ServerPlayer p,boolean attack){
        double entityReach=attack?p.getAttackRangeWith(p.getMainHandItem()).effectiveMaxRange(p):p.entityInteractionRange();
        double blockReach=p.blockInteractionRange(),reach=Math.min(15,Math.max(blockReach,entityReach));
        var ray=PortalInteractionRay.trace(p,DoorwayData.get(p.level()).all(),reach);
        if(ray==null)return null;
        Vec3 limit=ray.end();
        var next=DoorwayProjectileCrossing.first(DoorwayData.get(p.level()).all(),ray.start(),limit,0,0);
        if(next!=null)limit=ray.start().lerp(limit,next.fraction()).subtract(limit.subtract(ray.start()).normalize().scale(.0001));
        if(!loaded(p,ray.start(),limit))return null;
        var block=p.level().clip(new ClipContext(ray.start(),limit,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        Vec3 end=block.getType()==HitResult.Type.MISS?limit:block.getLocation();
        Entity entity=null;Vec3 hit=end;double nearest=ray.start().distanceToSqr(end);
        for(var e:p.level().getEntities(p,new AABB(ray.start(),end).inflate(1),e->e.isPickable()&&!e.isSpectator()&&e.isAlive())){
            var box=e.getBoundingBox().inflate(e.getPickRadius());
            Vec3 point=box.contains(ray.start())?ray.start():box.clip(ray.start(),end).orElse(null);
            if(point==null)continue;
            double dist=point.distanceToSqr(ray.start());
            if(dist<nearest&&ray.before()+Math.sqrt(dist)<=entityReach){nearest=dist;entity=e;hit=point;}
        }
        if(entity==null&&(block.getType()!=HitResult.Type.BLOCK||ray.before()+ray.start().distanceTo(end)>blockReach))return null;
        if(!p.level().getWorldBorder().isWithinBounds(BlockPos.containing(hit)))return null;
        return new Target(ray,block,entity,hit);
    }
    private static boolean loaded(ServerPlayer p,Vec3 start,Vec3 end){
        int steps=(int)Math.ceil(start.distanceTo(end)*2)+1;
        for(int i=0;i<=steps;i++)if(!p.level().hasChunkAt(BlockPos.containing(start.lerp(end,(double)i/steps))))return false;
        return true;
    }
    private static void abort(ServerPlayer p,State s){
        if(s.mining!=null){
            p.gameMode.handleBlockBreakAction(s.mining,ABORT_DESTROY_BLOCK,net.minecraft.core.Direction.UP,p.level().getMaxY(),0);
            s.mining=null;
        }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        var s=STATES.get(p);if(s==null)return;
        if(s.level!=p.level()){STATES.remove(p);return;}
        if(s.menu!=null&&(p.containerMenu!=s.menu.menu||!menuReach(p,s.menu.pos,4))) {
            if(p.containerMenu==s.menu.menu)p.closeContainer();s.menu=null;
        }
        if(s.mining==null){
            if(usable(p)&&p.level().getGameTime()-s.heartbeat<=3&&p.level().getGameTime()>=s.nextMine){
                var next=target(p,true);if(next!=null&&next.entity==null)begin(p,s,next);
            }
            return;
        }
        if(!usable(p)||p.level().getGameTime()-s.heartbeat>3||!ItemStack.isSameItemSameComponents(s.tool,p.getMainHandItem())){abort(p,s);return;}
        var t=target(p,true);
        if(t==null||t.entity!=null||!s.mining.equals(t.block.getBlockPos())){abort(p,s);return;}
        scoped(p,t.ray,()->{
            var block=p.level().getBlockState(s.mining);
            if(block.isAir()){s.mining=null;return;}
            var mining=(PortalMiningAccessor)p.gameMode;
            if(!mining.portal$isDestroying()||!s.mining.equals(mining.portal$destroyPos())){s.mining=null;return;}
            if(p.level().getGameTime()%4==0){
                var sound=block.getSoundType();
                p.level().playSound(null,s.mining,sound.getHitSound(),net.minecraft.sounds.SoundSource.BLOCKS,
                    (sound.getVolume()+1)/8,sound.getPitch()*.5f);
            }
            float progress=block.getDestroyProgress(p,p.level(),s.mining)*(mining.portal$ticks()-mining.portal$start()+1);
            if(progress>=1){
                p.gameMode.handleBlockBreakAction(s.mining,STOP_DESTROY_BLOCK,t.block.getDirection(),p.level().getMaxY(),0);
                s.mining=null;s.nextMine=p.level().getGameTime()+5;PortalTerrainServer.refresh(p);
            }
        });
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer p)STATES.remove(p);
    }
    /** Extends only the existing block menu's reach, never arbitrary packet targets. */
    public static boolean menuReach(Player player,BlockPos pos,double padding){
        if(!(player instanceof ServerPlayer p)||!p.isAlive()||!p.level().dimension().equals(Schizm.KEY))return false;
        var s=STATES.get(p);if(s==null||s.menu==null||s.menu.menu!=p.containerMenu)return false;
        if(!s.menu.pos.equals(pos)){
            var state=p.level().getBlockState(s.menu.pos);
            if(!(state.getBlock() instanceof net.minecraft.world.level.block.ChestBlock)
                ||state.getValue(net.minecraft.world.level.block.ChestBlock.TYPE)==net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                ||!net.minecraft.world.level.block.ChestBlock.getConnectedBlockPos(s.menu.pos,state).equals(pos))return false;
            // The connected half is covered by the original, validated chest sightline.
            pos=s.menu.pos;
        }
        var data=DoorwayData.get(p.level());var a=data.get(s.menu.source);var b=data.get(s.menu.target);
        if(a==null||b==null||!a.enabled()||!b.enabled()||!a.target().equals(b.name())||!b.target().equals(a.name()))return false;
        Vec3 eye=p.getEyePosition(),destination=Vec3.atCenterOf(pos),unfolded=b.transformPoint(a,destination);
        double da=a.distance(eye),db=a.distance(unfolded);
        if(da<=0||db>=0||da-db<1e-7)return false;
        Vec3 plane=eye.lerp(unfolded,da/(da-db)),local=a.local(plane);
        if(Math.abs(local.x)>a.width()/2||local.y<0||local.y>a.height())return false;
        if(eye.distanceTo(unfolded)>p.blockInteractionRange()+Math.min(4,Math.max(0,padding)))return false;
        if(p.level().clip(new ClipContext(eye,plane,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p)).getType()!=HitResult.Type.MISS)return false;
        Vec3 start=a.transformPoint(b,plane).add(b.facing().normal.scale(.0001));
        if(!loaded(p,start,destination))return false;
        var hit=p.level().clip(new ClipContext(start,destination,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos);
    }
}
