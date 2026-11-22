package net.beamex.shamaschizm.wisdom.giant;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class WisdomBrood {
    private static final String OWNER="GiantWisdomOwner", POSITION="GiantWisdomPosition";
    public static void bind(Mob mob,GiantWisdom owner){
        var data=mob.getPersistentData();data.putString(OWNER,owner.getUUID().toString());data.putLong(POSITION,owner.blockPosition().asLong());
        data.putBoolean("GiantWisdomWasPersistent",mob.isPersistenceRequired());mob.setPersistenceRequired();
    }
    public static boolean bound(Mob mob){return mob.getPersistentData().contains(OWNER);}
    public static void release(Mob mob){
        ((net.beamex.shamaschizm.mixin.GiantWisdomMobAccessor)(Object)mob).shamaschizm$setBroodPersistence(mob.getPersistentData().getBooleanOr("GiantWisdomWasPersistent",false));
        mob.getPersistentData().remove("GiantWisdomWasPersistent");mob.getPersistentData().remove(OWNER);mob.getPersistentData().remove(POSITION);mob.setTarget(null);mob.getNavigation().stop();}
    public static ServerPlayer target(Mob mob){
        if(!(mob.level() instanceof ServerLevel level)||!bound(mob))return null;
        Entity owner;
        try{owner=level.getEntity(UUID.fromString(mob.getPersistentData().getStringOr(OWNER,"")));}catch(IllegalArgumentException ex){release(mob);return null;}
        if(!(owner instanceof GiantWisdom brain)||!brain.isAlive()){
            // Do not mistake an unloaded boss for a killed one. Never force its chunk to load.
            if(owner!=null||level.hasChunkAt(BlockPos.of(mob.getPersistentData().getLongOr(POSITION,0))))release(mob);
            return null;
        }
        mob.getPersistentData().putLong(POSITION,brain.blockPosition().asLong());
        return level.players().stream().filter(p->GiantWisdom.eligible(p)&&p.distanceToSqr(brain)<48*48)
            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }
    public static Goal attackGoal(RoachEntity roach){return new BroodAttack(roach);}
    @SubscribeEvent public static void tick(EntityTickEvent.Post e){
        if(!(e.getEntity() instanceof Mob m)||!(m.level() instanceof ServerLevel level)||!bound(m))return;
        // Observe an actual death tick, after cancellable death events have finished.
        if(m.deathTime>0){
            try{var owner=level.getEntity(UUID.fromString(m.getPersistentData().getStringOr(OWNER,"")));
                if(owner instanceof GiantWisdom brain)brain.broodDied(m.getUUID());
            }catch(IllegalArgumentException ignored){}
        }else if(m.tickCount%20==0)target(m);
    }
    private static final class BroodAttack extends Goal {
        private final RoachEntity mob;private ServerPlayer victim;private int cooldown;
        BroodAttack(RoachEntity mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK,Flag.JUMP));}
        @Override public boolean canUse(){victim=target(mob);return victim!=null;}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            if(victim==null)return;
            mob.setTarget(victim);mob.getLookControl().setLookAt(victim,30,30);
            if(mob.tickCount%5==0)mob.getNavigation().moveTo(victim,1.3);
            if(cooldown>0)cooldown--;
            if(cooldown==0&&mob.getBoundingBox().inflate(.35).intersects(victim.getBoundingBox())&&mob.hasLineOfSight(victim)){
                victim.hurtServer((ServerLevel)mob.level(),mob.damageSources().mobAttack(mob),mob instanceof net.beamex.shamaschizm.entity.custom.BabyRoachEntity?1:2);cooldown=20;
            }
        }
        @Override public void stop(){mob.setTarget(null);mob.getNavigation().stop();victim=null;}
    }
}
