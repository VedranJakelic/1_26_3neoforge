package net.beamex.shamaschizm.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.worldgen.structure.PondStructure;
import net.beamex.shamaschizm.worldgen.structure.MountainPeakStructure;

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

    public static final DeferredHolder<StructureType<?>, StructureType<MountainPeakStructure>> MOUNTAIN_PEAK_STRUCTURE =
            STRUCTURE_TYPES.register("mountain_peak_structure", () -> new StructureType<>() {
                @Override
                public MapCodec<MountainPeakStructure> codec() {
                    return MountainPeakStructure.CODEC;
                }
            });
}
