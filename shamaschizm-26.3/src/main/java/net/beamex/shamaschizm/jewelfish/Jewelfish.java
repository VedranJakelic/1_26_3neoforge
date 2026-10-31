package net.beamex.shamaschizm.jewelfish;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class Jewelfish extends AbstractFish {
    private int eaten, biteCooldown;
    public Jewelfish(EntityType<? extends Jewelfish> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return AbstractFish.createAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.MOVEMENT_SPEED,.8).add(Attributes.TEMPT_RANGE,10);}
    public int eaten(){return eaten;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new PanicGoal(this,1.6));
        goalSelector.addGoal(1,new AvoidEntityGoal<Player>(this,Player.class,8,1.5,1.8,
                p->!p.isSpectator()&&!JewelfishFood.holding((Player)p)){
            @Override public boolean canContinueToUse(){return super.canContinueToUse()&&toAvoid!=null&&!JewelfishFood.holding(toAvoid);}
        });
        goalSelector.addGoal(2,new TemptGoal(this,1.0,s->JewelfishFood.value(s)>0,false));
        goalSelector.addGoal(3,new ForageGoal());
        goalSelector.addGoal(4,new RandomSwimmingGoal(this,.8,40));
    }
    private boolean canEat(int value){return value>0&&eaten<=Integer.MAX_VALUE-value;}
    private void ate(int value){eaten+=value;setPersistenceRequired();biteCooldown=20;playSound(SoundEvents.GENERIC_EAT.value(),.5F,1.4F);}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);int value=JewelfishFood.value(stack);
        if(isAlive()&&canEat(value)){
            if(level() instanceof ServerLevel&&biteCooldown==0){
                // Every credited item is actually consumed, including creative feeding.
                stack.shrink(1);ate(value);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player,hand);
    }
    @Override public void aiStep(){super.aiStep();if(level() instanceof ServerLevel&&biteCooldown>0)biteCooldown--;}
    @Override public boolean requiresCustomPersistence(){return eaten>0||super.requiresCustomPersistence();}
    @Override public boolean removeWhenFarAway(double distance){return eaten==0&&super.removeWhenFarAway(distance);}
    @Override public ItemStack getBucketItemStack(){return new ItemStack(JewelfishRegistration.BUCKET);}
    @Override public void saveToBucketTag(ItemStack stack){super.saveToBucketTag(stack);CustomData.update(DataComponents.BUCKET_ENTITY_DATA,stack,t->t.putInt("JewelsEaten",eaten));}
    @Override public void loadFromBucketTag(CompoundTag tag){super.loadFromBucketTag(tag);eaten=Math.max(0,tag.getIntOr("JewelsEaten",0));if(eaten>0)setPersistenceRequired();}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("JewelsEaten",eaten);}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);eaten=Math.max(0,in.getIntOr("JewelsEaten",0));if(eaten>0)setPersistenceRequired();}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean playerKill){
        super.dropCustomDeathLoot(level,source,playerKill);
        var pool=JewelfishFood.jewels();int[] counts=new int[pool.size()];
        for(int i=0;i<eaten;i++)counts[random.nextInt(counts.length)]++;
        eaten=0;
        for(int i=0;i<counts.length;i++)while(counts[i]>0){ItemStack drop=new ItemStack(pool.get(i));int n=Math.min(counts[i],drop.getMaxStackSize());drop.setCount(n);spawnAtLocation(level,drop);counts[i]-=n;}
    }
    @Override protected SoundEvent getFlopSound(){return SoundEvents.SALMON_FLOP;}
    @Override protected SoundEvent getHurtSound(DamageSource source){return SoundEvents.SALMON_HURT;}
    @Override protected SoundEvent getDeathSound(){return SoundEvents.SALMON_DEATH;}

    /** Water navigation only; targets are checked again at the bite, not trusted from discovery. */
    private final class ForageGoal extends Goal {
        private ItemEntity food;
        private BlockPos block;
        private int ticks,nextSearchTick;
        ForageGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        private boolean edible(ItemEntity e){return e.isAlive()&&e.isInWater()&&canEat(JewelfishFood.value(e.getItem()));}
        @Override public boolean canUse(){
            if(!isInWater()||biteCooldown>0||tickCount<nextSearchTick)return false;
            nextSearchTick=tickCount+10;
            food=null;block=null;
            double best=64;
            for(var e:level().getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(8),this::edible)){
                double d=distanceToSqr(e);if(d<best&&hasLineOfSight(e)){food=e;best=d;}
            }
            if(food!=null)return true;
            // Only exposed nearby storage blocks; never ores, machines or ordinary terrain.
            if(level() instanceof ServerLevel server&&EventHooks.canEntityGrief(server,Jewelfish.this)){
                for(BlockPos p:BlockPos.betweenClosed(blockPosition().offset(-2,-1,-2),blockPosition().offset(2,1,2))){
                    var state=server.getBlockState(p);
                    if(state.hasBlockEntity()||JewelfishFood.value(new ItemStack(state.getBlock().asItem()))!=9)continue;
                    if(!server.getFluidState(p.above()).is(FluidTags.WATER))continue;
                    Vec3 at=Vec3.atCenterOf(p.above());
                    if(distanceToSqr(at)<9){block=p.immutable();return true;}
                }
            }
            return false;
        }
        @Override public boolean canContinueToUse(){return ticks<100&&isInWater()&&biteCooldown==0&&(food!=null?edible(food):block!=null);}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void start(){ticks=0;}
        @Override public void stop(){getNavigation().stop();food=null;block=null;}
        @Override public void tick(){
            ticks++;
            Vec3 target=food!=null?food.position():Vec3.atCenterOf(block.above());
            if(ticks%10==1)getNavigation().moveTo(target.x,target.y,target.z,1.0);
            getLookControl().setLookAt(target.x,target.y,target.z);
            if(!(level() instanceof ServerLevel server)||distanceToSqr(target)>1.5)return;
            if(food!=null&&edible(food)&&hasLineOfSight(food)){
                int value=JewelfishFood.value(food.getItem());ItemStack rest=food.getItem().copy();rest.shrink(1);
                if(rest.isEmpty())food.discard();else food.setItem(rest);ate(value);
            }else if(block!=null&&canEat(9)&&EventHooks.canEntityGrief(server,Jewelfish.this)){
                var state=server.getBlockState(block);
                if(!state.hasBlockEntity()&&JewelfishFood.value(new ItemStack(state.getBlock().asItem()))==9
                        &&server.getFluidState(block.above()).is(FluidTags.WATER)
                        &&server.clip(new net.minecraft.world.level.ClipContext(getEyePosition(),target,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,Jewelfish.this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS
                        &&server.destroyBlock(block,false,Jewelfish.this))ate(9);
            }
        }
    }
}
