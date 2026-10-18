package net.beamex.shamaschizm.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.worldgen.structure.PondStructure;

public final class ShamaschizmStructures {

    private ShamaschizmStructures() {
    }

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Shamaschizm.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<PondStructure>> POND_STRUCTURE =
            STRUCTURE_TYPES.register("pond_structure", () -> new StructureType<>() {
                @Override
                public MapCodec<PondStructure> codec() {
                    return PondStructure.CODEC;
                }
            });
}
