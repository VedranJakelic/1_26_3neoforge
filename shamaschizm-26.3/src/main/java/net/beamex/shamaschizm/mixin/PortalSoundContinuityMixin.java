package net.beamex.shamaschizm.mixin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.beamex.shamaschizm.localportal.client.PortalAudioContinuity;
import net.minecraft.client.Camera;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class PortalSoundContinuityMixin {
    @Shadow @Final private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;
    @Unique private final Set<SoundInstance> shamaschizm$routed = new HashSet<>();

    @Inject(method = "updateSource", at = @At("TAIL"))
    private void shamaschizm$followPortal(Camera camera, CallbackInfo ci) {
        if (!camera.isInitialized()) return;
        Vec3 ear = camera.position();
        shamaschizm$routed.retainAll(instanceToChannel.keySet());
        for (var entry : instanceToChannel.entrySet()) {
            var sound = entry.getKey();
            Vec3 routed = PortalAudioContinuity.routedPosition(sound, ear);
            if (routed != null) {
                shamaschizm$routed.add(sound);
                entry.getValue().execute(channel -> channel.setSelfPosition(routed));
            } else if (shamaschizm$routed.remove(sound)) {
                Vec3 original = new Vec3(sound.getX(), sound.getY(), sound.getZ());
                entry.getValue().execute(channel -> channel.setSelfPosition(original));
            }
        }
    }
}
