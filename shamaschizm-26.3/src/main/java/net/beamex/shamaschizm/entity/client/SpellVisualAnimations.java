package net.beamex.shamaschizm.entity.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

public final class SpellVisualAnimations {
    private SpellVisualAnimations() {}

    public static final AnimationDefinition ROTATE = AnimationDefinition.Builder.withLength(4.0F)
            .addAnimation("bone", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(1, KeyframeAnimations.degreeVec(0, -90, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(2, KeyframeAnimations.degreeVec(0, -180, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(3, KeyframeAnimations.degreeVec(0, -270, 0), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(4, KeyframeAnimations.degreeVec(0, -360, 0), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("bone", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0, KeyframeAnimations.scaleVec(0.1F, 1, 0.1F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.2917F, KeyframeAnimations.scaleVec(1, 1, 1), AnimationChannel.Interpolations.CATMULLROM)))
            .build();
}
