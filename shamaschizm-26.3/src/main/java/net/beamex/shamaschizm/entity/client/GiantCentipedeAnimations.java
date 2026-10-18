package net.beamex.shamaschizm.entity.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** Blockbench's supplied eight-second head idle animation. */
public final class GiantCentipedeAnimations {
    private GiantCentipedeAnimations() {}

    public static final AnimationDefinition IDLE = AnimationDefinition.Builder.withLength(8.0F).looping()
            .addAnimation("bone2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    kl(0.0F, 0, 0, 0), k(0.75F, 21.0489F, 46.8312F, -12.698F),
                    k(0.875F, 18.0181F, 37.4008F, -17.2009F), k(1.1667F, 23.7632F, 50.303F, 8.8905F),
                    k(1.2917F, 41.0108F, 45.4836F, 21.992F), k(1.3333F, 37.9849F, 41.6231F, 17.6041F),
                    k(1.4167F, 42.7648F, 47.3451F, 24.4131F), k(1.7083F, 29.0197F, 18.4888F, 0.1675F),
                    k(1.9167F, 2.218F, 19.4454F, -13.0147F), k(2.125F, 2.1069F, 6.9542F, -13.4982F),
                    k(2.375F, 20.2643F, 2.6282F, 19.6076F), k(2.4583F, 10.2643F, 2.6282F, 19.6076F),
                    k(2.5F, 17.7643F, 2.6282F, 19.6076F), k(2.75F, 26.0671F, 48.3854F, 16.7606F),
                    k(2.8333F, 36.0671F, 48.3854F, 16.7606F), k(2.875F, 16.0671F, 48.3854F, 16.7606F),
                    k(3.0F, 25.5886F, 64.8137F, 28.0382F), k(3.2083F, 10.6055F, -2.9333F, 4.0595F),
                    k(3.5417F, 7.2357F, 10.4432F, -16.578F), k(3.7917F, 39.2246F, 52.443F, -8.5533F),
                    k(3.875F, 29.2341F, 37.8838F, -22.4947F), k(4.0417F, 39.7214F, 26.4359F, -3.0247F),
                    k(4.125F, 33.8714F, 33.9519F, -14.6023F), k(4.25F, 45.3824F, 49.4973F, 2.4648F),
                    k(4.3333F, 38.496F, 42.0369F, -7.1151F), k(4.4583F, 46.2174F, 53.234F, -2.8628F),
                    k(4.7917F, 33.0895F, 37.6711F, -21.0404F), k(5.0417F, 20.9559F, 28.5847F, -6.7438F),
                    k(5.2917F, 27.4524F, 58.5677F, -19.353F), k(5.5F, 15.8717F, 31.8173F, -21.901F),
                    k(5.7917F, 28.5391F, 13.3435F, 5.0591F), k(5.875F, 26.0075F, 41.1096F, 5.5901F),
                    k(6.25F, -15.6769F, 47.3516F, -28.5088F), k(6.375F, 9.9373F, 62.1986F, -24.7289F),
                    k(6.625F, 41.9461F, 51.8535F, 12.8794F), k(6.9167F, 33.7147F, 41.9405F, 1.664F),
                    k(7.1667F, 37.1946F, 39.0377F, 7.0195F), k(7.4167F, 25.7762F, 75.7985F, 1.0331F),
                    k(7.6667F, -3.0723F, 67.2046F, -28.5485F), k(7.75F, -2.3598F, 59.7129F, -27.7538F),
                    kl(7.9583F, 0, 0, 0)))
            .addAnimation("bone", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    kl(0.0F, 0, 0, 0), k(0.75F, 8.0265F, -21.6478F, -8.0419F),
                    k(1.2917F, 5.8274F, -1.613F, -4.5305F), k(1.5F, 6.0181F, -0.5785F, -14.4788F),
                    k(1.6667F, 6.0181F, -0.5785F, -14.4788F), k(1.7917F, 5.9452F, -1.0999F, -9.5055F),
                    k(2.25F, 23.4581F, -30.9419F, -7.2708F), k(2.5833F, 24.4057F, -44.3804F, -7.9203F),
                    k(3.0417F, -2.9538F, -19.9676F, 19.09F), k(3.2917F, 9.1529F, -22.6122F, 5.705F),
                    k(3.5F, -3.1151F, 7.3423F, 4.0183F), k(3.75F, -5.4425F, 5.8346F, -16.0593F),
                    k(3.875F, 24.4727F, -11.5864F, -14.3911F), k(4.1667F, 25.7009F, -20.6477F, -18.8F),
                    k(5.0417F, 25.7F, -20.65F, -18.8F), k(5.25F, -1.8864F, 18.9769F, -13.2363F),
                    k(5.6667F, 22.5657F, -16.3904F, -10.8215F), k(5.7917F, 22.57F, -16.39F, -10.82F),
                    k(6.125F, -10.0976F, -12.978F, 27.8415F), k(6.25F, -10.1F, -12.98F, 27.84F),
                    k(6.625F, 13.2165F, -42.8293F, 31.3146F), k(7.2083F, 13.2165F, -42.8293F, 31.3146F),
                    k(7.2917F, 4.3897F, -36.4234F, 7.9968F), k(7.5417F, -2.5026F, 1.0352F, 6.1036F),
                    k(7.625F, -2.5331F, -8.9552F, 6.5435F), kl(7.9583F, 0, 0, 0)))
            .build();

    private static Keyframe k(float time, float x, float y, float z) {
        return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.CATMULLROM);
    }

    private static Keyframe kl(float time, float x, float y, float z) {
        return new Keyframe(time, KeyframeAnimations.degreeVec(x, y, z), AnimationChannel.Interpolations.LINEAR);
    }
}
