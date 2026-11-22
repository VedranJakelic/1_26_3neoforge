package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.eclipse.EclipseOrbit;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class EclipseLunarClockMixin {
    @Inject(method="addDynamicLayers",at=@At("RETURN"))
    private void eclipseLunarClock(Level level,CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> ci){
        if(!level.dimension().equals(Level.OVERWORLD))return;
        var builder=ci.getReturnValue();
        builder.addTimeBasedLayer(EnvironmentAttributes.SUN_ANGLE,(old,offset)->
                (float)Math.toDegrees(EclipseOrbit.sunAngle(level.getOverworldClockTime()+offset)));
        builder.addPositionalLayer(EnvironmentAttributes.MOON_ANGLE,(old,pos,interpolator)->{
            var m=EclipseOrbit.sky(level.getOverworldClockTime(),pos.x,pos.y,pos.z).moon();
            return (float)Math.toDegrees(Math.atan2(-m.x(),m.y()));
        });
        builder.addTimeBasedLayer(EnvironmentAttributes.MOON_PHASE,(old,offset)->{
            int phase=(int)Math.floor(EclipseOrbit.lunarPhase(level.getOverworldClockTime()+offset)*8+.5);
            return MoonPhase.values()[(4+phase)%8];
        });
    }
}
