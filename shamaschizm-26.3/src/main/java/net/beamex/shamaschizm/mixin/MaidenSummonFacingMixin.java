package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.maiden.IronMaiden;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.SummonCommand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Console/command-block spawns retain their supplied rotation because there is no player. */
@Mixin(SummonCommand.class)
public abstract class MaidenSummonFacingMixin {
    @Inject(method = "createEntity", at = @At("RETURN"))
    private static void shamaschizm$faceSummoner(CommandSourceStack source,
            Holder.Reference<EntityType<?>> type, Vec3 pos, CompoundTag tag, boolean finalize,
            CallbackInfoReturnable<Entity> result) {
        if (source.getEntity() instanceof Player player
                && result.getReturnValue() instanceof IronMaiden maiden) maiden.faceSpawner(player);
    }
}
