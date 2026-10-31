package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.maiden.IronMaiden;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses the actual egg user, including multiplayer; never guesses the nearest player. */
@Mixin(EntityType.class)
public abstract class MaidenSpawnEggFacingMixin {
    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void shamaschizm$faceEggUser(ServerLevel level, ItemStack stack, LivingEntity user,
            BlockPos pos, EntitySpawnReason reason, boolean align, boolean offset,
            CallbackInfoReturnable<Entity> result) {
        if (reason == EntitySpawnReason.SPAWN_ITEM_USE && user instanceof Player player
                && result.getReturnValue() instanceof IronMaiden maiden) maiden.faceSpawner(player);
    }
}
