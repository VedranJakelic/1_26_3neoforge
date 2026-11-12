package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
@Mixin(SleepStatus.class)
public abstract class AstralNightMixin {
 @Inject(method="areEnoughDeepSleeping",at=@At("HEAD"),cancellable=true)
 private void astral$night(int required,List<ServerPlayer> players,CallbackInfoReturnable<Boolean> ci){if(players.stream().anyMatch(AstralProjection::active))ci.setReturnValue(false);}
}
