package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Unit;
import com.mojang.datafixers.util.Either;
@Mixin(ServerPlayer.class)
public abstract class AstralSleepMixin {
 @Inject(method="startSleepInBed",at=@At("HEAD"),cancellable=true)
 private void astral$begin(AbstractBedBlock block,BlockState state,BedRule rule,BlockPos pos,CallbackInfoReturnable<Either<Player.BedSleepingProblem,Unit>> ci){if(AstralProjection.begin((ServerPlayer)(Object)this,pos))ci.setReturnValue(Either.right(Unit.INSTANCE));}
 @Inject(method="stopSleepInBed",at=@At("HEAD"),cancellable=true)
 private void astral$remain(CallbackInfo ci){if(AstralProjection.active((ServerPlayer)(Object)this)&&((ServerPlayer)(Object)this).isAlive())ci.cancel();}
}
