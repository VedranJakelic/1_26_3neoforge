package net.beamex.shamaschizm.ritual;

import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.homunculus.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Eat -> feed a specific villager -> kill that same villager in the intact circle. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class HomunculusRitualEvents {
    private record Circle(ResourceKey<Level> dimension,BlockPos center){}
    private record Fed(UUID player,Circle circle){}
    private record Pending(LivingDeathEvent death,Villager villager,Fed fed){}
    private static final Map<UUID,Circle> EATEN=new HashMap<>();
    private static final Map<UUID,Fed> FED=new HashMap<>();
    private static final List<Pending> PENDING=new ArrayList<>();
    private HomunculusRitualEvents(){}
    public static boolean villagerFood(ItemStack stack){
        return stack.is(Items.BREAD)||stack.is(Items.CARROT)||stack.is(Items.POTATO)||stack.is(Items.BEETROOT);
    }
    private static boolean ordinaryVillager(Villager v){return v.getType()==EntityTypes.VILLAGER;}
    private static boolean inside(Entity entity,BlockPos center){
        return HomunculusRitualRules.inside(entity.getX(),entity.getY(),entity.getZ(),center.getX(),center.getY(),center.getZ());
    }
    private static boolean intact(Level level,BlockPos center){
        var state=level.getBlockState(center);
        return state.is(Blocks.COMPOSTER)&&state.getValue(ComposterBlock.LEVEL)>=7
                && RitualEvents.hasLitRing(level,center.above(),false);
    }
    private static BlockPos findCircle(Player player){
        BlockPos feet=player.blockPosition();
        for(int dy=-2;dy<=1;dy++)for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){
            BlockPos center=feet.offset(dx,dy,dz);
            if(inside(player,center)&&intact(player.level(),center))return center;
        }
        return null;
    }
    @SubscribeEvent public static void eat(LivingEntityUseItemEvent.Finish event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var food=event.getItem().get(DataComponents.FOOD);
        if(food==null||food.nutrition()<=0)return;
        EATEN.remove(player.getUUID());
        BlockPos center=findCircle(player);
        if(center!=null)EATEN.put(player.getUUID(),new Circle(player.level().dimension(),center.immutable()));
    }
    private static Circle canFeed(ServerPlayer player,Villager villager){
        Circle circle=EATEN.get(player.getUUID());
        if(circle==null||!ordinaryVillager(villager)||!villager.isAlive()||player.level()!=villager.level()
                ||!circle.dimension.equals(player.level().dimension())||!inside(player,circle.center)
                ||!inside(villager,circle.center)||!intact(player.level(),circle.center))return null;
        return circle;
    }
    /** Called after a confirmed pickup or a successful explicit hand-feed. */
    public static void fed(ServerPlayer player,Villager villager){
        Circle circle=canFeed(player,villager);if(circle==null)return;
        EATEN.remove(player.getUUID());FED.put(villager.getUUID(),new Fed(player.getUUID(),circle));
        ServerLevel level=(ServerLevel)villager.level();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,villager.getX(),villager.getY()+1,villager.getZ(),3,.2,.2,.2,0);
        villager.playSound(SoundEvents.GENERIC_EAT.value(),.5F,1F);
    }
    @SubscribeEvent public static void handFeed(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Villager villager)||!ordinaryVillager(villager)||!villagerFood(event.getItemStack()))return;
        if(event.getLevel().isClientSide()){
            BlockPos center=findCircle(event.getEntity());
            if(center!=null&&inside(villager,center)){event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);}
            return;
        }
        if(!(event.getEntity() instanceof ServerPlayer player)||canFeed(player,villager)==null)return;
        ItemStack one=event.getItemStack().copyWithCount(1);
        if(!villager.getInventory().canAddItem(one))return;
        ItemStack remainder=villager.getInventory().addItem(one);
        if(!remainder.isEmpty())return;
        event.getItemStack().consume(1,player);fed(player,villager);
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(!(event.getEntity() instanceof Villager villager)||!(event.getSource().getEntity() instanceof ServerPlayer killer))return;
        Fed fed=FED.get(villager.getUUID());
        if(fed==null||!fed.player.equals(killer.getUUID())||killer.level()!=villager.level()
                ||!fed.circle.dimension.equals(killer.level().dimension())||!inside(killer,fed.circle.center)
                ||!inside(villager,fed.circle.center)||!intact(killer.level(),fed.circle.center))return;
        // Defer until every death listener has run, including cancellation/rescue hooks.
        PENDING.add(new Pending(event,villager,fed));
    }
    @SubscribeEvent public static void afterDeaths(ServerTickEvent.Post event){
        for(Pending pending:List.copyOf(PENDING)){
            if(pending.death.isCanceled()||pending.villager.isAlive())continue;
            ServerLevel level=(ServerLevel)pending.villager.level();
            BlockPos center=pending.fed.circle.center;
            FED.remove(pending.villager.getUUID());
            if(!intact(level,center))continue;
            if(!level.getEntitiesOfClass(HomunculusEntity.class,new AABB(center).inflate(1),HomunculusEntity::isIncubating).isEmpty())continue;
            HomunculusEntity child=HomunculusRegistration.HOMUNCULUS.create(level,EntitySpawnReason.TRIGGERED);
            if(child==null)continue;
            child.beginRitualBirth(center);
            if(!level.addFreshEntity(child))continue;
            level.setBlock(center,level.getBlockState(center).setValue(ComposterBlock.LEVEL,HomunculusRitualRules.compostLevel(0)),3);
            level.playSound(null,center,SoundEvents.COMPOSTER_EMPTY,SoundSource.BLOCKS,.7F,.65F);
        }
        PENDING.clear();
    }
    @SubscribeEvent public static void lockIncubatingComposter(PlayerInteractEvent.RightClickBlock event){
        if(!event.getLevel().getBlockState(event.getPos()).is(Blocks.COMPOSTER))return;
        if(event.getLevel().getEntitiesOfClass(HomunculusEntity.class,new AABB(event.getPos()).inflate(.5),HomunculusEntity::isIncubating).isEmpty())return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
    @SubscribeEvent public static void leaves(EntityLeaveLevelEvent event){
        if(event.getEntity() instanceof Villager)FED.remove(event.getEntity().getUUID());
        if(event.getEntity() instanceof ServerPlayer player){
            EATEN.remove(player.getUUID());FED.values().removeIf(f->f.player.equals(player.getUUID()));
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){EATEN.clear();FED.clear();PENDING.clear();}
}
