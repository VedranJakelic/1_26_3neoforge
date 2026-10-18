package net.beamex.shamaschizm.event.client;

import net.beamex.shamaschizm.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** Relative looping music whose volume is driven by GardenMusicClient. */
final class GardenMusicSound extends AbstractTickableSoundInstance {
    GardenMusicSound() {
        super(ModSounds.AMBIJANSA3.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = 1.0F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        // Volume and lifetime are controlled centrally so transitions survive movement.
    }

    void setFadeVolume(float volume) {
        this.volume = volume;
    }

    void finish() {
        this.stop();
    }
}
