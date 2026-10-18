package net.beamex.shamaschizm.mixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Preserve legacy soul balances now that NeoForge no longer imports ForgeData. */
@Mixin(Entity.class)
public abstract class LegacySoulDataMixin {
 @Inject(method="load", at=@At("TAIL"))
 private void shamaschizm$importSouls(ValueInput input, CallbackInfo ci) {
  if ((Object)this instanceof ServerPlayer player && player.getPersistentData().getInt("shamaschizm_souls").isEmpty())
   input.read("ForgeData",CompoundTag.CODEC).flatMap(data -> data.getInt("shamaschizm_souls"))
    .ifPresent(value -> net.beamex.shamaschizm.SoulData.setSouls(player,value));
 }
}
