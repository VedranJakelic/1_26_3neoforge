package net.beamex.shamaschizm.mixin;
import java.util.EnumSet;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Phantom.class)
public abstract class PhantomDreamcatcherMixin extends Mob {
    @Shadow private Vec3 moveTargetPoint;
    protected PhantomDreamcatcherMixin(EntityType<? extends Mob> type, Level level) { super(type,level); }
    @Inject(method="registerGoals",at=@At("TAIL"))
    private void shamaschizm$avoidDreamcatcher(CallbackInfo ci) {
        // Vanilla's attack strategy has no flags. Give it TARGET so fleeing can
        // stop it BEFORE clearing its target (its tick assumes a non-null target).
        for (var wrapped : goalSelector.getAvailableGoals()) {
            var goal = wrapped.getGoal();
            if (goal.getFlags().isEmpty()) goal.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }
        goalSelector.addGoal(0,new net.beamex.shamaschizm.dreamcatcher.DreamcatcherAvoidGoal(
                (Phantom)(Object)this, point -> moveTargetPoint=point));
    }
}
