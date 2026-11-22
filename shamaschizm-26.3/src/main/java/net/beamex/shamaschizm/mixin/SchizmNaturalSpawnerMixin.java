package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.world.SchizmPopulation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class SchizmNaturalSpawnerMixin {
    @Inject(method = "createState", at = @At("RETURN"))
    private static void shamaschizm$dimension(int chunks, ServerLevel level,
            NaturalSpawner.ChunkGetter getter, LocalMobCapCalculator local,
            CallbackInfoReturnable<NaturalSpawner.SpawnState> cir) {
        ((SchizmPopulation.SpawnState)(Object)cir.getReturnValue())
                .shamaschizm$setSchizm(level.dimension().equals(Schizm.KEY) && level.players().stream().anyMatch(p ->
                        p.isAlive() && !p.isSpectator() && net.beamex.shamaschizm.world.dungeon.DeepRoomZones.bugHabitat(level, p.blockPosition())));
    }
}
