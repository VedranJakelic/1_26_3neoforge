package net.beamex.shamaschizm.saints.vindication;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
public final class TemplarSpearItem extends TridentItem {
 public TemplarSpearItem(Item.Properties p){super(p);}
 @Override public InteractionResult use(Level level,Player p,InteractionHand hand){
  // Mounted saints retain the kinetic component; player use is always a throw.
  p.getItemInHand(hand).remove(DataComponents.KINETIC_WEAPON);
  return super.use(level,p,hand);
 }
 @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
  if(!(user instanceof Player player))return false;
  if(EnchantmentHelper.getTridentSpinAttackStrength(stack,player)>0)return super.releaseUsing(stack,level,user,remaining);
  if(getUseDuration(stack,user)-remaining<10||stack.nextDamageWillBreak())return false;
  if(level instanceof ServerLevel server){
   stack.hurtWithoutBreaking(1,player);
   var thrown=new TemplarProjectile(VindicationRegistration.SPEAR_PROJECTILE,level);
   thrown.configure(stack.consumeAndReturn(1,player),player);
   thrown.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
   thrown.shootFromRotation(player,player.getXRot(),player.getYRot(),0,2.5F,1);
   if(player.hasInfiniteMaterials())thrown.pickup=AbstractArrow.Pickup.CREATIVE_ONLY;
   server.addFreshEntity(thrown);
   level.playSound(null,thrown,SoundEvents.TRIDENT_THROW.value(),SoundSource.PLAYERS,1,1);
   player.awardStat(Stats.ITEM_USED.get(this));
  }
  return true;
 }
 @Override public Projectile asProjectile(Level level,Position pos,ItemStack stack,Direction direction){
  var result=new TemplarProjectile(VindicationRegistration.SPEAR_PROJECTILE,level);
  result.configure(stack.copyWithCount(1),null);result.setPos(pos.x(),pos.y(),pos.z());return result;
 }
}
