package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
@Mixin(MouseHandler.class)
public abstract class AstralMouseMixin {
 @Redirect(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
 private void astral$turn(LocalPlayer p,double x,double y){if(AstralClient.active())AstralClient.turn(x,y);else p.turn(x,y);}
}
