package net.beamex.shamaschizm.loading;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** All state and generation stay on the server thread. No off-thread world writes. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class DungeonLoading {
    private static final String TAG="ShamaschizmDungeonLoadingRestore";
    private static final Map<UUID,Wait> WAITING=new HashMap<>();
    private static final class Wait {
        final ServerPlayer player; final UUID token=UUID.randomUUID(); final Runnable work;
        Vec3 position; boolean ready,finished; int ticks;
        Wait(ServerPlayer p,Runnable r){player=p;work=r;position=p.position();}
    }
    public static boolean active(ServerPlayer p){return WAITING.containsKey(p.getUUID());}
    public static void begin(ServerPlayer p,Runnable work){
        if(active(p))return;
        int flags=(p.isInvisible()?1:0)|(p.isPermanentlyInvulnerable()?2:0)|(p.noPhysics?4:0)|(p.isNoGravity()?8:0);
        p.getPersistentData().putInt(TAG,flags);
        Wait w=new Wait(p,work);WAITING.put(p.getUUID(),w);
        p.closeContainer();p.setInvisible(true);p.setPermanentlyInvulnerable(true);p.noPhysics=true;p.setNoGravity(true);
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setPortalCooldown();
        send(w,0);
    }
    private static void send(Wait w,int phase){
        PacketDistributor.sendToAllPlayers(new DungeonLoadingNetwork.State(w.player.getUUID(),w.token,phase,
                w.player.level().dimension().identifier().toString(),w.player.blockPosition()));
    }
    public static void ack(ServerPlayer p,DungeonLoadingNetwork.Ack a){
        Wait w=WAITING.get(p.getUUID());if(w==null||!w.token.equals(a.token()))return;
        if(a.phase()==1&&!w.finished)w.ready=true;
        else if(a.phase()==2&&w.finished || a.phase()==3&&!w.ready)end(w);
    }
    private static void restore(ServerPlayer p){
        if(!p.getPersistentData().contains(TAG))return;
        int f=p.getPersistentData().getIntOr(TAG,0);p.getPersistentData().remove(TAG);
        p.setInvisible((f&1)!=0);p.setPermanentlyInvulnerable((f&2)!=0);p.noPhysics=(f&4)!=0;p.setNoGravity((f&8)!=0);
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setPortalCooldown();
    }
    private static void end(Wait w){WAITING.remove(w.player.getUUID());restore(w.player);send(w,2);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        for(Wait w:new ArrayList<>(WAITING.values())){
            if(w.player.isRemoved()||!w.player.isAlive()){end(w);continue;}
            if(++w.ticks>200){end(w);continue;}
            if(!w.ready||w.finished)continue;
            w.finished=true; // Never run the same job twice, including failure paths.
            try {w.work.run();}
            catch(RuntimeException ex){Shamaschizm.LOGGER.error("Dungeon generation failed during loading",ex);}
            finally {w.position=w.player.position();w.ticks=0;send(w,1);}
            break; // At most one new dungeon per server tick.
        }
    }
    @SubscribeEvent public static void freeze(PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        Wait w=WAITING.get(p.getUUID());if(w==null)return;
        p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setPortalCooldown();
        if(p.position().distanceToSqr(w.position)>.0001)p.teleportTo(w.position.x,w.position.y,w.position.z);
    }
    @SubscribeEvent public static void hurt(LivingIncomingDamageEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){if(e.getNewAboutToBeSetTarget() instanceof ServerPlayer p&&active(p))e.setNewAboutToBeSetTarget(null);}
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p))e.setCanceled(true);}
    @SubscribeEvent public static void interactEntity(PlayerInteractEvent.EntityInteract e){blockInteraction(e);}
    @SubscribeEvent public static void rightClickBlock(PlayerInteractEvent.RightClickBlock e){blockInteraction(e);}
    @SubscribeEvent public static void rightClickItem(PlayerInteractEvent.RightClickItem e){blockInteraction(e);}
    @SubscribeEvent public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock e){blockInteraction(e);}
    private static void blockInteraction(PlayerInteractEvent e){if(e.getEntity() instanceof ServerPlayer p&&active(p)&&e instanceof net.neoforged.bus.api.ICancellableEvent cancel)cancel.setCanceled(true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){Wait w=WAITING.get(p.getUUID());if(w!=null)end(w);else restore(p);}}
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppingEvent e){
        for(Wait w:new ArrayList<>(WAITING.values()))restore(w.player);
        WAITING.clear();
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        restore(p); // Recover original flags if a server stopped while the player was waiting.
        for(Wait w:WAITING.values())PacketDistributor.sendToPlayer(p,new DungeonLoadingNetwork.State(w.player.getUUID(),w.token,0,
                w.player.level().dimension().identifier().toString(),w.player.blockPosition()));
    }
}
