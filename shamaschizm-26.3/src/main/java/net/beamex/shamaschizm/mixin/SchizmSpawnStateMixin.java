package net.beamex.shamaschizm.mixin;

import net.beamex.shamaschizm.world.SchizmPopulation;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
public abstract class SchizmSpawnStateMixin implements SchizmPopulation.SpawnState {
    @Unique private boolean shamaschizm$schizm;
    @Override public void shamaschizm$setSchizm(boolean value) { shamaschizm$schizm = value; }
    @Redirect(method = "canSpawnForCategoryGlobal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int shamaschizm$cap(MobCategory category) {
        return shamaschizm$schizm ? SchizmPopulation.cap(category) : category.getMaxInstancesPerChunk();
    }
}
