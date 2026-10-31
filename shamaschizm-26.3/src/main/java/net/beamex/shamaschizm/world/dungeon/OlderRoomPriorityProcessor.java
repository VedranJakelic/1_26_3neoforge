package net.beamex.shamaschizm.world.dungeon;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/** Placement-only processor: earlier room volumes own blocks, air and entity anchors. */
final class OlderRoomPriorityProcessor implements StructureProcessor {
    private final List<BoundingBox> olderRooms;

    OlderRoomPriorityProcessor(List<BoundingBox> olderRooms) {
        this.olderRooms = List.copyOf(olderRooms);
    }

    private boolean protectedPosition(BlockPos pos) {
        return olderRooms.stream().anyMatch(box -> box.isInside(pos));
    }

    @Override
    public StructureTemplate.StructureBlockInfo process(LevelReader level, BlockPos origin,
            BlockPos reference, StructureTemplate.StructureBlockInfo original,
            StructureTemplate.StructureBlockInfo transformed, StructurePlaceSettings settings,
            StructureTemplate template) {
        return protectedPosition(transformed.pos()) ? null : transformed;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos origin,
            StructureTemplate.StructureEntityInfo original, StructureTemplate.StructureEntityInfo transformed,
            StructurePlaceSettings settings, StructureTemplate template) {
        return protectedPosition(transformed.blockPos) ? null : transformed;
    }

    @Override
    public MapCodec<? extends StructureProcessor> codec() {
        // Created directly for a single placement; never saved in a processor registry.
        return MapCodec.unit(this);
    }
}
