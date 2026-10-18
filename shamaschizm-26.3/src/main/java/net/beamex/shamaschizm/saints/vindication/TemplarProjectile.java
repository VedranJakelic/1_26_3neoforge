package net.beamex.shamaschizm.saints.vindication;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.*;
/** Trident flight, eight base damage, enchantment hooks and loyalty return; spear appearance. */
public final class TemplarProjectile extends AbstractArrow {
 private static final EntityDataAccessor<ItemStack> STACK=SynchedEntityData.defineId(TemplarProjectile.class,EntityDataSerializers.ITEM_STACK);
 private boolean dealt,returning;private int loyalty;
 public TemplarProjectile(EntityType<? extends TemplarProjectile> t,Level l){super(t,l);pickup=Pickup.ALLOWED;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(STACK,ItemStack.EMPTY);}
 public void configure(ItemStack stack,LivingEntity owner){setPickupItemStack(stack.copy());entityData.set(STACK,stack.copy());if(owner!=null)setOwner(owner);refreshLoyalty();}
 private void refreshLoyalty(){if(level() instanceof ServerLevel l)loyalty=Math.min(127,EnchantmentHelper.getTridentReturnToOwnerAcceleration(l,getWeaponItem(),this));}
 public ItemStack displayedItem(){return entityData.get(STACK);}
 @Override public ItemStack getWeaponItem(){return getPickupItemStackOrigin();}
 @Override protected ItemStack getDefaultPickupItem(){return new ItemStack(VindicationRegistration.SPEAR);}
 @Override protected float getWaterInertia(){return .99F;}
 @Override protected net.minecraft.sounds.SoundEvent getDefaultHitGroundSoundEvent(){return SoundEvents.TRIDENT_HIT_GROUND;}
 @Override protected boolean canHitEntity(Entity e){return !dealt&&!returning&&super.canHitEntity(e);}
 @Override protected void onHitEntity(EntityHitResult hit){
  if(!(level() instanceof ServerLevel level))return;dealt=true;
  Entity victim=hit.getEntity(),owner=getOwner();var damage=damageSources().trident(this,owner==null?this:owner);
  float amount=EnchantmentHelper.modifyDamage(level,getWeaponItem(),victim,damage,8F);
  if(victim.hurtServer(level,damage,amount)){
   EnchantmentHelper.doPostAttackEffectsWithItemSourceOnBreak(level,victim,damage,getWeaponItem(),s->discard());
   if(victim instanceof LivingEntity living){doKnockback(living,damage);doPostHurtEffects(living);}
   channel(level,victim.blockPosition());
  }
  setDeltaMovement(getDeltaMovement().multiply(-.02,-.2,-.02));playSound(SoundEvents.TRIDENT_HIT,1,1);
 }
 @Override protected void hitBlockEnchantmentEffects(ServerLevel l,BlockHitResult hit,ItemStack weapon){
  EnchantmentHelper.onHitBlock(l,weapon,getOwner() instanceof LivingEntity living?living:null,this,null,
   hit.getBlockPos().clampLocationWithin(hit.getLocation()),l.getBlockState(hit.getBlockPos()),s->discard());
  if(l.getBlockState(hit.getBlockPos()).is(net.minecraft.tags.BlockTags.LIGHTNING_RODS))channel(l,hit.getBlockPos().above());
 }
 private void channel(ServerLevel level,net.minecraft.core.BlockPos at){
  // Vanilla Channeling explicitly checks entity type minecraft:trident, so a custom projectile
  // must perform this part itself; all other enchantment effects still use the normal hooks.
  var enchantment=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.CHANNELING);
  if(!level.isThundering()||!level.canSeeSky(at)||EnchantmentHelper.getItemEnchantmentLevel(enchantment,getWeaponItem())<=0)return;
  var bolt=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.TRIGGERED);
  if(bolt!=null){bolt.setPos(Vec3.atBottomCenterOf(at));if(getOwner() instanceof net.minecraft.server.level.ServerPlayer p)bolt.setCause(p);level.addFreshEntity(bolt);playSound(SoundEvents.TRIDENT_THUNDER.value(),5,1);}
 }
 @Override public void tick(){
  if(level() instanceof ServerLevel l){
   if(inGroundTime>4)dealt=true;
   if(loyalty>0&&dealt&&getOwner()!=null){
    Entity owner=getOwner();
    if(!owner.isAlive()||owner.isSpectator()){
     if(pickup==Pickup.ALLOWED)spawnAtLocation(l,getPickupItem(),.1F);discard();return;
    }
    if(!returning){playSound(SoundEvents.TRIDENT_RETURN,10,1);returning=true;}
    setNoPhysics(true);Vec3 delta=owner.getEyePosition().subtract(position());
    setPos(getX(),getY()+delta.y*.015*loyalty,getZ());
    setDeltaMovement(getDeltaMovement().scale(.95).add(delta.normalize().scale(.05*loyalty)));
   }
  }
  super.tick();
 }
 @Override protected boolean tryPickup(Player p){
  if(getOwner()!=null&&!ownedBy(p))return false;
  return super.tryPickup(p)||(returning&&ownedBy(p)&&pickup==Pickup.ALLOWED&&p.getInventory().add(getPickupItem()));
 }
 @Override public void tickDespawn(){if(loyalty<=0||pickup!=Pickup.ALLOWED)super.tickDespawn();}
 @Override protected void addAdditionalSaveData(ValueOutput o){super.addAdditionalSaveData(o);o.putBoolean("Dealt",dealt);}
 @Override protected void readAdditionalSaveData(ValueInput i){super.readAdditionalSaveData(i);dealt=i.getBooleanOr("Dealt",false);entityData.set(STACK,getPickupItemStackOrigin().copy());refreshLoyalty();}
}
