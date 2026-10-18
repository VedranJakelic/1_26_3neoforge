package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.saints.CatacombSaints;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SaintsCombatMovementMixin {
    @Inject(method="blockUsingItem",at=@At("HEAD"))
    private void shamaschizm$pickaxeDisablesLid(net.minecraft.server.level.ServerLevel level,
            LivingEntity attacker,net.minecraft.world.damagesource.DamageSource source,float damage,boolean fullyBlocked, CallbackInfo ci){
        net.beamex.shamaschizm.saints.CoffinLidDisabling.onBlockedHit(level,(LivingEntity)(Object)this,attacker);
    }
    @Inject(method="startUsingItem",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$keepLidDisabled(net.minecraft.world.InteractionHand hand,CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;
        if(self.getItemInHand(hand).is(CatacombSaints.LID)
                && net.beamex.shamaschizm.saints.CoffinLidDisabling.isDisabled(self))ci.cancel();
    }

    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$noOrdinarySaintJumps(CallbackInfo ci){
        Entity self=(Entity)(Object)this;
        // The intentional dismounted mace leap sets velocity directly. No
        // navigation-triggered jump should compete with that controlled attack.
        if(CatacombSaints.encounterId(self)!=null
                && !self.getPersistentData().getBooleanOr("SaintsReleased",false))ci.cancel();
    }
    @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
    private void shamaschizm$noFriendlySpearHit(EquipmentSlot slot,Entity target,float damage,
            boolean dealsDamage,boolean knockback,boolean dismounts,CallbackInfoReturnable<Boolean> ci){
        Entity self=(Entity)(Object)this;
        var encounter=CatacombSaints.encounterId(self);
        if(encounter==null)return;
        // Never let the native kinetic spear damage, knock back or dismount
        // another participant or one of the encounter's horses.
        if(encounter.equals(CatacombSaints.encounterId(target)))ci.setReturnValue(false);
    }
}
