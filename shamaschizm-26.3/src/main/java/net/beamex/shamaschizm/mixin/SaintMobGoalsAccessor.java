package net.beamex.shamaschizm.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reinstalls the horse's normal goals, including horses saved by the first boss patch. */
@Mixin(Mob.class)
public interface SaintMobGoalsAccessor {
    @Invoker("registerGoals")
    void shamaschizm$registerGoals();
}
