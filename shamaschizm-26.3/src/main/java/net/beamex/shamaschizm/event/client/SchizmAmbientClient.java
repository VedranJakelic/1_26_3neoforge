package net.beamex.shamaschizm.event.client;

import java.util.ArrayList;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.garden.GardenAudioClient;
import net.beamex.shamaschizm.sound.ModSounds;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Per-listener depth mix; never changes the player's volume options. */
@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class SchizmAmbientClient {
    private static final int BASE_INTERVAL_TICKS = 150 * 20;
    private static final int JITTER_TICKS = 45 * 20;
    // The supplied recording averages -18.9 dBFS; this puts it near -28 dBFS.
    private static final float TONE_GAIN = 0.35F;
    private static final List<DepthSound> additions = new ArrayList<>();
    private static Object level;
    private static DepthSound tone;
    private static float depth;
    private static double cooldown = 20;
    private static int checkTicks;

    private SchizmAmbientClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level || mc.player == null || mc.level == null
                || !mc.level.dimension().equals(Schizm.KEY)) {
            reset(mc);
            level = mc.level;
        }
        if (mc.level == null || mc.player == null
                || !mc.level.dimension().equals(Schizm.KEY) || mc.isPaused()) return;

        mc.getMusicManager().stopPlaying();
        float target = Mth.clamp((float) ((150.0 - mc.player.getY()) / 90.0), 0F, 1F);
        target = target * target * (3F - 2F * target);
        depth = Mth.approach(depth, target, 1F / 60F);

        // Keep a silent stream alive at the top, so descending never restarts it.
        // Retry after resource reload or a muted category, with a startup grace period.
        if (checkTicks-- <= 0) {
            checkTicks = 40;
            additions.removeIf(s -> !mc.getSoundManager().isActive(s));
            if (tone == null || !mc.getSoundManager().isActive(tone)) {
                if (tone != null) tone.finish();
                tone = new DepthSound(SoundEvent.createVariableRangeEvent(
                        Shamaschizm.id("shepard_tone")), true);
                mc.getSoundManager().play(tone);
            }
        }
        // Slow the remaining countdown too when crossing below Y=90.
        cooldown -= mc.player.getY() < 90.0 ? 0.25 : 1.0;
        if (cooldown > 0) return;
        cooldown = BASE_INTERVAL_TICKS + mc.level.getRandom().nextInt(JITTER_TICKS + 1);
        var sound = switch (mc.level.getRandom().nextInt(4)) {
            case 0 -> ModSounds.SCHIZM_01.get();
            case 1 -> ModSounds.SCHIZM_02.get();
            case 2 -> ModSounds.SCHIZM_03.get();
            default -> ModSounds.SCHIZM_04.get();
        };
        DepthSound addition = new DepthSound(sound, false);
        additions.add(addition);
        mc.getSoundManager().play(addition);
    }

    private static void reset(Minecraft mc) {
        if (tone != null) { tone.finish(); mc.getSoundManager().stop(tone); }
        for (DepthSound sound : additions) {
            sound.finish(); mc.getSoundManager().stop(sound);
        }
        additions.clear(); tone = null; depth = 0; cooldown = 20; checkTicks = 0;
    }

    /** Owns its garden fade, avoiding a wrapper that hides the active loop's identity. */
    public static final class DepthSound extends AbstractTickableSoundInstance {
        private final Object ownerLevel;
        private final boolean isTone;
        private DepthSound(SoundEvent event, boolean isTone) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.ownerLevel = Minecraft.getInstance().level;
            this.isTone = isTone;
            looping = isTone; delay = 0; relative = true; attenuation = Attenuation.NONE;
            updateVolume();
        }
        private void updateVolume() {
            // Complementary gains avoid adding full-volume tone over full ambience.
            volume = (isTone ? TONE_GAIN * depth : 1F - 0.95F * depth)
                    * GardenAudioClient.ambienceGain();
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            if (Minecraft.getInstance().level != ownerLevel) { stop(); return; }
            updateVolume();
        }
        private void finish() { stop(); }
    }
}
