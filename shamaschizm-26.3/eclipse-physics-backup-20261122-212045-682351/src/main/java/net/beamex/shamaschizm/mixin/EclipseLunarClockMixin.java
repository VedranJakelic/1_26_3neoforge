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

/** Share the accelerated lunar angle/phase with sky rendering AND astral celestial queries. */
@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class EclipseLunarClockMixin {
    @Inject(method="addDynamicLayers",at=@At("RETURN"))
    private void eclipseLunarClock(Level level,CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> ci) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        var builder=ci.getReturnValue();
        builder.addTimeBasedLayer(EnvironmentAttributes.MOON_ANGLE,(old,offset)-> {
            double phase=EclipseOrbit.lunarPhase(level.getOverworldClockTime()+offset);
            return (float)(old-180-360*phase);
        });
        builder.addTimeBasedLayer(EnvironmentAttributes.MOON_PHASE,(old,offset)-> {
            int phase=(int)Math.floor(EclipseOrbit.lunarPhase(level.getOverworldClockTime()+offset)*8+.5);
            return MoonPhase.values()[(4+phase)%8];
        });
    }
}
