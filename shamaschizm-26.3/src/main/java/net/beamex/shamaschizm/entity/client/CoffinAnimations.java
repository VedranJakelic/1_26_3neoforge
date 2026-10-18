package net.beamex.shamaschizm.entity.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Supplied four-second Blockbench lid animation. */
public final class CoffinAnimations {
    private CoffinAnimations() {}

    public static final AnimationDefinition OPEN = AnimationDefinition.Builder.withLength(4.0F)
            .addAnimation("lid", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0417F, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.25F, KeyframeAnimations.degreeVec(0, -2.5F, 0.0193F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.375F, KeyframeAnimations.degreeVec(0, -2.5F, 0.0193F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0, -2.44F, 0.03F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.9583F, KeyframeAnimations.degreeVec(0, -1.9215F, 0.0858F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.2083F, KeyframeAnimations.degreeVec(0, -1.9215F, 0.0858F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.875F, KeyframeAnimations.degreeVec(0, 0, -2.5F), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.125F, KeyframeAnimations.degreeVec(0, 0, -10), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.3333F, KeyframeAnimations.degreeVec(0, 0, -45), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3.4583F, KeyframeAnimations.degreeVec(0, 0, -57.5F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("lid", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0417F, KeyframeAnimations.posVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.0F, KeyframeAnimations.posVec(-1, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1.875F, KeyframeAnimations.posVec(-1, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2.4167F, KeyframeAnimations.posVec(-2, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3.125F, KeyframeAnimations.posVec(-3, 0, 0), AnimationChannel.Interpolations.CATMULLROM),
                    new Keyframe(3.3333F, KeyframeAnimations.posVec(-5, -4, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3.4583F, KeyframeAnimations.posVec(-5, -7, 0), AnimationChannel.Interpolations.LINEAR)))
            .build();
}
