package net.beamex.shamaschizm.saints;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Trident gravity/drag and damage, with terrain collision and piercing player-only hits. */
public final class SaintThrownSpear extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> DISPLAY=SynchedEntityData.defineId(SaintThrownSpear.class,EntityDataSerializers.ITEM_STACK);
    private UUID encounter;
    private final Set<String> struck=new HashSet<>();
    public SaintThrownSpear(EntityType<? extends SaintThrownSpear> type,Level level){super(type,level);pickup=Pickup.ALLOWED;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){super.defineSynchedData(data);data.define(DISPLAY,new ItemStack(Items.NETHERITE_SPEAR));}
    public void configure(ItemStack stack,UUID encounter){this.encounter=encounter;setPickupItemStack(stack.copyWithCount(1));entityData.set(DISPLAY,stack.copyWithCount(1));pickup=Pickup.ALLOWED;}
    public ItemStack displayedItem(){return entityData.get(DISPLAY);}
    public boolean grounded(){return isInGround();}
    public ItemStack reclaim(){ItemStack result=getPickupItem();discard();return result;}
    @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(Items.NETHERITE_SPEAR);}
    @Override public ItemStack getWeaponItem(){return getPickupItemStackOrigin();}
    @Override protected float getWaterInertia(){return .99F;}
    @Override protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent(){return SoundEvents.TRIDENT_HIT_GROUND;}
    @Override protected boolean canHitEntity(Entity entity){return false;}
    @Override protected void tickDespawn(){} // A recoverable encounter weapon, never a disposable arrow.
    @Override public void tick(){
        Vec3 from=position();boolean wasGrounded=isInGround();
        super.tick();
        if(!(level() instanceof ServerLevel level))return;
        Vec3 to=position();
        if(!wasGrounded && from.distanceToSqr(to)>1.0E-8){
            // Use the actual terrain-clipped travel segment, so players behind walls
            // cannot be struck. No entity ever deflects or slows this spear.
            for(Player player:level.getEntitiesOfClass(Player.class,new AABB(from,to).inflate(1),p->p.isAlive()&&!p.isSpectator()&&!p.isCreative())){
                AABB box=player.getBoundingBox().inflate(.15);
                if(!box.contains(from) && box.clip(from,to).isEmpty())continue;
                if(!struck.add(player.getUUID().toString()))continue;
                Entity owner=getOwner();var source=damageSources().trident(this,owner==null?this:owner);
                float damage=EnchantmentHelper.modifyDamage(level,getWeaponItem(),player,source,8.0F);
                if(player.hurtServer(level,source,damage)){
                    EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(level,player,source,getWeaponItem(),item->discard());
                    doKnockback(player,source);doPostHurtEffects(player);
                }
                playSound(SoundEvents.TRIDENT_HIT,1,1);
            }
        }
        if(isInGround() && tickCount%4==0)level.sendParticles(ParticleTypes.SMOKE,getX(),getY()+.2,getZ(),2,.12,.2,.12,.015);
    }
    @Override protected boolean tryPickup(Player player){
        boolean taken=super.tryPickup(player);
        if(taken && encounter!=null && level() instanceof ServerLevel level)SaintsDeathData.get(level).recordSpearClaimed(encounter);
        return taken;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        if(encounter!=null)out.putString("SaintsEncounter",encounter.toString());
        out.store("StruckPlayers",Codec.STRING.listOf(),struck.stream().toList());
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        try{encounter=UUID.fromString(in.getString("SaintsEncounter").orElse(""));}catch(IllegalArgumentException ignored){encounter=null;}
        struck.clear();struck.addAll(in.read("StruckPlayers",Codec.STRING.listOf()).orElse(java.util.List.of()));
        entityData.set(DISPLAY,getPickupItemStackOrigin().copy());
    }
}
