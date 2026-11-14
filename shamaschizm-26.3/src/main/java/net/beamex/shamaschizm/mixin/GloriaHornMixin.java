package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.ritual.GloriaRitual;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.InstrumentItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InstrumentItem.class)
public abstract class GloriaHornMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private static void shamaschizm$gloriaHorn(Level level, Player player, Instrument instrument, CallbackInfo ci) {
        if (!player.getUseItem().is(Items.GOAT_HORN)) return;
        // Skip predicted horn audio. The server chooses and broadcasts exactly one sound,
        // including to the user; no client-only authorization state can get out of sync.
        ci.cancel();
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return;
        boolean completed = GloriaRitual.completeWithHorn(serverPlayer);
        SoundEvent sound = completed ? SoundEvents.WITHER_SPAWN : instrument.soundEvent().value();
        float volume = completed ? 1.0F : instrument.range() / 16.0F;
        // Use the normal entity sound broadcast so existing local-portal sound routing applies.
        server.playSound((Entity) null, player, sound, completed ? SoundSource.PLAYERS : SoundSource.RECORDS, volume, 1.0F);
        server.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
    }
}
