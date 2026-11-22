package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.loading.DungeonLoading;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class DungeonLoadingCollisionMixin {
    @Inject(method={"isPushable","isPickable"},at=@At("HEAD"),cancellable=true)
    private void shamaschizm$untouchable(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof ServerPlayer p&&DungeonLoading.active(p))cir.setReturnValue(false);
    }
}
