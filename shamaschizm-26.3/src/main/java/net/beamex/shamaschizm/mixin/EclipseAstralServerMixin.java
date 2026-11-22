package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.EclipseOrbit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.beamex.shamaschizm.astral.AstralProjection")
public abstract class EclipseAstralServerMixin {
    @Inject(method="celestial",at=@At("HEAD"),cancellable=true)
    private static void orbitalCelestial(ServerLevel level,Vec3 p,boolean moon,CallbackInfoReturnable<Vec3> ci){
        if(!level.dimension().equals(Level.OVERWORLD))return;
        var sky=EclipseOrbit.sky(level.getOverworldClockTime(),p.x,p.y,p.z);
        var v=moon?sky.moon():sky.sun();ci.setReturnValue(new Vec3(v.x(),v.y(),v.z()));
    }
}
