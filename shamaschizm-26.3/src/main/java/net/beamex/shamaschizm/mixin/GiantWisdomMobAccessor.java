package net.beamex.shamaschizm.mixin;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Mob.class)
public interface GiantWisdomMobAccessor {
    @Accessor("persistenceRequired") void shamaschizm$setBroodPersistence(boolean value);
}
