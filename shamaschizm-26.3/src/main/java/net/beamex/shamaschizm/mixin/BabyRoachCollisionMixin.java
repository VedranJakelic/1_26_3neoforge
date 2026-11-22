package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.entity.custom.BabyRoachEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancel both directions of entity pushing; block collision and attack targeting remain intact. */
@Mixin(Entity.class)
public abstract class BabyRoachCollisionMixin {
    private static boolean excluded(Entity e) {
        return e instanceof Player || e.getType().getCategory() == MobCategory.MONSTER;
    }
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void shamaschizm$babyPush(Entity other, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (self instanceof BabyRoachEntity && excluded(other)
                || other instanceof BabyRoachEntity && excluded(self)) ci.cancel();
    }
}
