package net.beamex.shamaschizm.world;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent, indexed Overworld portal positions; avoids radius block scans. */
public final class PortalAtmosphereData extends SavedData {
    private final Set<BlockPos> portals;
    private static final Codec<PortalAtmosphereData> CODEC = BlockPos.CODEC.listOf().xmap(
            PortalAtmosphereData::new, data -> List.copyOf(data.portals));
    public static final SavedDataType<PortalAtmosphereData> TYPE = new SavedDataType<>(
            Shamaschizm.id("portal_atmosphere"), PortalAtmosphereData::new, CODEC, null);

    private PortalAtmosphereData() {
        this.portals = new HashSet<>();
    }

    private PortalAtmosphereData(List<BlockPos> portals) {
        this.portals = new HashSet<>();
        portals.forEach(pos -> this.portals.add(pos.immutable()));
    }

    public static PortalAtmosphereData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(BlockPos pos) {
        if (this.portals.add(pos.immutable())) setDirty();
    }

    public void addAll(Iterable<BlockPos> positions) {
        boolean changed = false;
        for (BlockPos pos : positions) changed |= this.portals.add(pos.immutable());
        if (changed) setDirty();
    }

    public void remove(BlockPos pos) {
        if (this.portals.remove(pos)) setDirty();
    }

    /** Finds portal blocks placed by structure/world generation, whose block onPlace hook may be bypassed. */
    public void indexChunk(LevelChunk chunk) {
        boolean changed = false;
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(net.beamex.shamaschizm.registry.ModBlocks.SCHIZM_PORTAL))) {
                continue;
            }
            int baseY = chunk.getMinY() + sectionIndex * 16;
            for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                if (section.getBlockState(x, y, z).is(net.beamex.shamaschizm.registry.ModBlocks.SCHIZM_PORTAL)) {
                    changed |= this.portals.add(new BlockPos(baseX + x, baseY + y, baseZ + z));
                }
            }
        }
        if (changed) setDirty();
    }

    /** Linear falloff: 1 at a portal, 0 at and beyond 300 blocks. */
    public float proximity(BlockPos playerPos) {
        double nearestSquared = 300.0D * 300.0D;
        for (BlockPos portal : this.portals) {
            nearestSquared = Math.min(nearestSquared, portal.distSqr(playerPos));
        }
        return (float)Math.max(0.0D, 1.0D - Math.sqrt(nearestSquared) / 300.0D);
    }
}
