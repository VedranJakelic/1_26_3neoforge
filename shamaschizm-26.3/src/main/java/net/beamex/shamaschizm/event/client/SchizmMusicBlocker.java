// file: src/main/java/net/beamex/shamaschizm/event/client/SchizmMusicBlocker.java
package net.beamex.shamaschizm.event.client;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Suppresses unsolicited music while retaining the controlled garden and boss tracks. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class SchizmMusicBlocker {
    private SchizmMusicBlocker() {}

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent e) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        if (!mc.level.dimension().equals(Schizm.KEY)) return;

        var snd = e.getSound();
        // Keep blocking vanilla/random music, but allow the explicitly
        // controlled garden and battle tracks to use the player's Music volume slider.
        if (snd != null && snd.getSource() == SoundSource.MUSIC
                && !snd.getIdentifier().equals(Shamaschizm.id("ambijansa3"))
                && !snd.getIdentifier().equals(Shamaschizm.id("saints_battle_cue"))
                && !snd.getIdentifier().equals(Shamaschizm.id("gallop_of_the_damned"))) {
            e.setSound(null);
        }
    }
}
