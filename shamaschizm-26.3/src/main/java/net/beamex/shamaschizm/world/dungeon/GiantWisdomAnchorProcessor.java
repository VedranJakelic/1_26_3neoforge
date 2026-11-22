package net.beamex.shamaschizm.world.dungeon;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.Vec3;

/** Transforms the brain's saved world-space tether with its room, without editing the template. */
final class GiantWisdomAnchorProcessor implements StructureProcessor {
    @Override
    public StructureTemplate.StructureEntityInfo processEntity(LevelReader level, BlockPos origin,
            StructureTemplate.StructureEntityInfo original, StructureTemplate.StructureEntityInfo transformed,
            StructurePlaceSettings settings, StructureTemplate template) {
        if (!"shamaschizm:giant_tainted_wisdom".equals(original.nbt.getStringOr("id", ""))) return transformed;
        var tag = transformed.nbt.copy();
        var savedPosition = original.nbt.read("Pos", Vec3.CODEC);
        var savedAnchor = original.nbt.read("GiantAnchor", Vec3.CODEC);
        if (savedPosition.isPresent() && savedAnchor.isPresent()) {
            // Pos still refers to the build world; original.pos is template-local.
            Vec3 sourceOrigin = savedPosition.get().subtract(original.pos);
            Vec3 localAnchor = savedAnchor.get().subtract(sourceOrigin);
            Vec3 anchor = StructureTemplate.transform(localAnchor, settings.getMirror(),
                    settings.getRotation(), settings.getRotationPivot()).add(Vec3.atLowerCornerOf(origin));
            tag.store("GiantAnchor", Vec3.CODEC, anchor);
            original.nbt.read("GiantAnchorBlock", BlockPos.CODEC).ifPresent(block -> {
                // Round the inferred integral template origin to avoid floating-point floor errors.
                BlockPos sourceBlock = new BlockPos((int)Math.round(sourceOrigin.x),
                        (int)Math.round(sourceOrigin.y), (int)Math.round(sourceOrigin.z));
                BlockPos support = StructureTemplate.transform(block.subtract(sourceBlock), settings.getMirror(),
                        settings.getRotation(), settings.getRotationPivot()).offset(origin);
                tag.store("GiantAnchorBlock", BlockPos.CODEC, support);
            });
        } else {
            // Older templates without a complete anchor use the boss's normal support search.
            tag.remove("GiantAnchor");
            tag.remove("GiantAnchorBlock");
        }
        tag.putBoolean("PersistenceRequired", true);
        // A copied room must never retain links to minions from the build world.
        tag.remove("GiantBrood");
        tag.remove("WisdomVibration");
        tag.store("Motion", Vec3.CODEC, Vec3.ZERO);
        return new StructureTemplate.StructureEntityInfo(transformed.pos, transformed.blockPos, tag);
    }

    @Override public MapCodec<? extends StructureProcessor> codec() { return MapCodec.unit(this); }
}
