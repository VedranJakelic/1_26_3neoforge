package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.astral.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
@Mixin(Camera.class)
public abstract class AstralCameraMixin {
 @Shadow private float eyeHeight;
 @Shadow private float eyeHeightOld;
 @Inject(method="update",at=@At("HEAD"))
 private void astral$height(CallbackInfo ci){if(AstralClient.active()){eyeHeight=Minecraft.getInstance().getCameraEntity().getEyeHeight();eyeHeightOld=eyeHeight;}}
}
