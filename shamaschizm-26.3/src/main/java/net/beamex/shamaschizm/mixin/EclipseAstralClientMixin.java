package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.EclipseOrbit;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.beamex.shamaschizm.astral.AstralClient")
public abstract class EclipseAstralClientMixin {
    @Inject(method="celestial",at=@At("HEAD"),cancellable=true)
    private static void orbitalCelestial(boolean moon,CallbackInfoReturnable<Vec3> ci){
        var mc=Minecraft.getInstance();if(mc.level==null || !mc.level.dimension().equals(Level.OVERWORLD))return;
        var p=mc.gameRenderer.mainCamera().position();var sky=EclipseOrbit.sky(mc.level.getOverworldClockTime(),p.x,p.y,p.z);
        var v=moon?sky.moon():sky.sun();ci.setReturnValue(new Vec3(v.x(),v.y(),v.z()));
    }
}
