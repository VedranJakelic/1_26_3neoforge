package net.beamex.shamaschizm.mixin;
import net.beamex.shamaschizm.entity.custom.RoachEntity;
import net.beamex.shamaschizm.wisdom.giant.WisdomBrood;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RoachEntity.class)
public abstract class GiantWisdomRoachMixin extends PathfinderMob {
    protected GiantWisdomRoachMixin(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void shamaschizm$broodGoal(CallbackInfo ci){goalSelector.addGoal(-10,WisdomBrood.attackGoal((RoachEntity)(Object)this));}
}
