package net.beamex.shamaschizm.garden;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.dungeon.GardenZoneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

@EventBusSubscriber(modid = Shamaschizm.MOD_ID, value = Dist.CLIENT)
public final class GardenAudioClient {
    public static final Identifier MUSIC = Shamaschizm.id("ambijansa3");
    private static float ambience = 1, musicGain;
    private static boolean entered;
    private static GardenLoop loop;
    private static Object lastLevel;
    public static float ambienceGain() { return ambience * net.beamex.shamaschizm.saints.audio.ArenaAudio.clientGain; }
    private static float approach(float current, float target) {
        return current + Math.max(-1F/60F, Math.min(1F/60F, target-current));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            if (loop != null) mc.getSoundManager().stop(loop);
            lastLevel = mc.level; loop = null; entered = false; musicGain = 0; ambience = 1;
            GardenAudio.proximity = 0; GardenAudio.inside = false;
        }
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        boolean schizm = mc.level.dimension().equals(Schizm.KEY);
        float near = schizm ? GardenAudio.proximity : 0;
        boolean inside = schizm && GardenAudio.inside;
        // Also support older gardens represented by a zone entity.
        if (schizm) {
            var point = mc.player.position();
            for (GardenZoneEntity zone : mc.level.getEntitiesOfClass(GardenZoneEntity.class,
                    mc.player.getBoundingBox().inflate(96))) {
                var box = zone.zoneBounds();
                double dx = Math.max(Math.max(box.minX - point.x, 0), point.x - box.maxX);
                double dy = Math.max(Math.max(box.minY - point.y, 0), point.y - box.maxY);
                double dz = Math.max(Math.max(box.minZ - point.z, 0), point.z - box.maxZ);
                double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
                near = Math.max(near, (float)Math.max(0, 1 - distance / 18.0));
                inside |= box.contains(point);
            }
        }
        if (inside) entered = true;
        // Outside: fade over 18..6 blocks, then retain only 2% ambience.
        float quiet = Math.min(1F, near * 1.5F);
        ambience = approach(ambience, inside ? 0F : 1F - 0.98F * quiet);
        // Music starts inside; on departure it fades to the quiet buffer level.
        musicGain = approach(musicGain, inside ? 1F : (entered ? 0.02F * near : 0F));
        if (entered && loop == null) {
            loop = new GardenLoop(); mc.getSoundManager().play(loop);
        }
        if (near == 0 && musicGain <= 0) {
            entered = false;
            if (loop != null) mc.getSoundManager().stop(loop);
            loop = null;
        } else if (loop != null && !mc.getSoundManager().isActive(loop)) {
            // Restart after resource reload or an explicit sound stop.
            loop = new GardenLoop(); mc.getSoundManager().play(loop);
        }
    }
    @SubscribeEvent public static void play(PlaySoundEvent e) {
        var mc = Minecraft.getInstance();
        var sound = e.getSound();
        if (sound == null || sound instanceof Faded || net.beamex.shamaschizm.saints.audio.ArenaMusicClient.isBattleSound(sound)
                || sound instanceof net.beamex.shamaschizm.event.client.SchizmAmbientClient.DepthSound || sound.getIdentifier().equals(MUSIC)
                || mc.level == null || !mc.level.dimension().equals(Schizm.KEY)) return;
        if (sound.getSource() == SoundSource.AMBIENT || sound.getSource() == SoundSource.MUSIC)
            e.setSound(new Faded(sound, mc.level));
    }
    private static final class GardenLoop extends AbstractTickableSoundInstance {
        GardenLoop() {
            super(SoundEvent.createVariableRangeEvent(MUSIC), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            looping = true; delay = 0; relative = true; attenuation = Attenuation.NONE; volume = musicGain * net.beamex.shamaschizm.saints.audio.ArenaAudio.clientGain;
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() { volume = musicGain * net.beamex.shamaschizm.saints.audio.ArenaAudio.clientGain; }
    }
    /** Delegates resolution/playback properties while updating only volume every sound tick. */
    private static final class Faded implements TickableSoundInstance {
        private final SoundInstance source;
        private final Object level;
        Faded(SoundInstance source, Object level) { this.source = source; this.level = level; }
        public Identifier getIdentifier() { return source.getIdentifier(); }
        public WeighedSoundEvents getOrResolve(SoundManager m) { return source.getOrResolve(m); }
        public net.minecraft.client.sounds.WeighedSoundEvents getSoundEvent() { return source.getSoundEvent(); }
        public Sound getSound() { return source.getSound(); }
        public SoundSource getSource() { return source.getSource(); }
        public boolean isLooping() { return source.isLooping(); }
        public boolean isRelative() { return source.isRelative(); }
        public int getDelay() { return source.getDelay(); }
        public float getVolume() { return source.getVolume()*ambienceGain(); }
        public float getPitch() { return source.getPitch(); }
        public double getX() { return source.getX(); }
        public double getY() { return source.getY(); }
        public double getZ() { return source.getZ(); }
        public Attenuation getAttenuation() { return source.getAttenuation(); }
        public boolean canStartSilent() { return true; }
        public boolean canPlaySound() { return source.canPlaySound(); }
        public boolean isStopped() { return Minecraft.getInstance().level != level
                || (source instanceof TickableSoundInstance t && t.isStopped()); }
        public void tick() { if (source instanceof TickableSoundInstance t) t.tick(); }
    }
}
