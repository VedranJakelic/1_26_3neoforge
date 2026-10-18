package net.beamex.shamaschizm.saints.client;
import net.minecraft.client.animation.*;
public final class Coffin2Animations {
private Coffin2Animations(){}

public static final AnimationDefinition OPEN = AnimationDefinition.Builder.withLength(4f)
.addAnimation("lid",
	new AnimationChannel(AnimationChannel.Targets.POSITION, 
		new Keyframe(0.04167f, KeyframeAnimations.posVec(0f, 0f, 0f),
			AnimationChannel.Interpolations.LINEAR), 
		new Keyframe(1f, KeyframeAnimations.posVec(-1f, 0f, 0f),
			AnimationChannel.Interpolations.LINEAR), 
		new Keyframe(1.875f, KeyframeAnimations.posVec(-1f, 0f, 0f),
			AnimationChannel.Interpolations.LINEAR), 
		new Keyframe(2.41667f, KeyframeAnimations.posVec(-2f, 0f, 0f),
			AnimationChannel.Interpolations.LINEAR), 
		new Keyframe(3.125f, KeyframeAnimations.posVec(-3f, 0f, 0f),
			AnimationChannel.Interpolations.CATMULLROM), 
		new Keyframe(3.33333f, KeyframeAnimations.posVec(-4f, -0.6f, 0f),
			AnimationChannel.Interpolations.LINEAR), 
		new Keyframe(3.45833f, KeyframeAnimations.posVec(-4f, -1.2f, 0f),
			AnimationChannel.Interpolations.LINEAR)))
.addAnimation("lid",
	new AnimationChannel(AnimationChannel.Targets.ROTATION,
		new Keyframe(0.04167f, KeyframeAnimations.degreeVec(0f, 0f, 0f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(0.25f, KeyframeAnimations.degreeVec(0f, 2.5f, 0.02f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(0.375f, KeyframeAnimations.degreeVec(0f, 2.5f, 0.02f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(0.75f, KeyframeAnimations.degreeVec(0f, 2.44f, 0.03f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(0.95833f, KeyframeAnimations.degreeVec(0f, 1.92f, 0.09f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(2.20833f, KeyframeAnimations.degreeVec(0f, 1.92f, 0.09f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(2.875f, KeyframeAnimations.degreeVec(0f, 0f, -2.5f),
			AnimationChannel.Interpolations.CATMULLROM),
		new Keyframe(3.125f, KeyframeAnimations.degreeVec(0f, 0f, -10f),
			AnimationChannel.Interpolations.CATMULLROM),
		new Keyframe(3.33333f, KeyframeAnimations.degreeVec(0f, 0f, -45f),
			AnimationChannel.Interpolations.LINEAR),
		new Keyframe(3.45833f, KeyframeAnimations.degreeVec(0f, 0f, -57.5f),
			AnimationChannel.Interpolations.LINEAR)))
.addAnimation("lid",
	new AnimationChannel(AnimationChannel.Targets.SCALE,
		new Keyframe(0.04167f, KeyframeAnimations.scaleVec(1f, 1f, 1f),
			AnimationChannel.Interpolations.LINEAR))).build();
}
