package net.beamex.shamaschizm.localportal.client;

import java.util.HashMap;
import net.beamex.shamaschizm.localportal.Doorway;
import net.beamex.shamaschizm.localportal.PortalSoundPath;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Reposition an existing audio channel; never restart, duplicate or replace its sound. */
public final class PortalAudioContinuity {
    public static Vec3 routedPosition(SoundInstance sound, Vec3 ear) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(Schizm.KEY)
                || sound.isRelative() || sound.getAttenuation() != SoundInstance.Attenuation.LINEAR
                || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.RECORDS
                || sound.getSource() == SoundSource.MASTER || sound.getSound() == null) return null;
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (!PortalSoundPath.finite(at)) return null;
        float range = Math.min(64, sound.getSound().getAttenuationDistance(sound.getVolume()));
        var doors = DoorwayDebugRenderer.doorways();
        var byName = new HashMap<String, Doorway>();
        for (var door : doors) byName.put(door.name(), door);
        double shortest = ear.distanceTo(at);
        PortalSoundPath.Path best = null;
        for (var source : doors) {
            var target = byName.get(source.target());
            if (target == null) continue;
            var route = PortalSoundPath.find(source, target, ear, at, range);
            if (route != null && route.distance() < shortest) {
                shortest = route.distance();
                best = route;
            }
        }
        if (best == null) return null;
        // Preserve native attenuation over the full acoustic distance, directed at the aperture.
        Vec3 direction = best.aperture().subtract(ear);
        if (direction.lengthSqr() < 1.0E-10) return ear;
        return ear.add(direction.normalize().scale(best.distance()));
    }
}
