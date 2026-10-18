package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.stag.Antlers;
import net.beamex.shamaschizm.stag.client.AntlerState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public class AntlerExtractMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("RETURN"))
 private void shamaschizm$antlers(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci){
  ((AntlerState)state).shamaschizm$setAntlers(Antlers.has(entity.getItemBySlot(EquipmentSlot.HEAD)));
 }
}
