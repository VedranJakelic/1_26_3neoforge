package net.beamex.shamaschizm.world.dungeon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persisted room ownership; unrecorded old dungeons use ordinary spawning. */
public final class DeepRoomZones extends SavedData {
    private record Zone(BoundingBox box, boolean deep) {
        static final Codec<Zone> CODEC = RecordCodecBuilder.create(i -> i.group(
                BoundingBox.CODEC.fieldOf("box").forGetter(Zone::box),
                Codec.BOOL.fieldOf("deep").forGetter(Zone::deep)).apply(i, Zone::new));
    }
    private static final Codec<DeepRoomZones> CODEC = Zone.CODEC.listOf().xmap(DeepRoomZones::new, z -> z.rooms);
    private static final SavedDataType<DeepRoomZones> TYPE = new SavedDataType<>(
            Shamaschizm.id("deep_spawn_rooms"), () -> new DeepRoomZones(List.of()), CODEC, DataFixTypes.LEVEL);
    private final List<Zone> rooms = new ArrayList<>();
    private final Map<Long, List<Zone>> chunks = new HashMap<>();
    private DeepRoomZones(List<Zone> saved) { for (Zone z : saved) { rooms.add(z); index(z); } }
    public static DeepRoomZones get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public static boolean contains(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && level.dimension().equals(Schizm.KEY) && get(server).deep(pos);
    }
    private static long key(int x, int z) { return ((long)x << 32) ^ (z & 0xffffffffL); }
    private void index(Zone zone) {
        BoundingBox b = zone.box();
        for (int x = b.minX() >> 4; x <= b.maxX() >> 4; x++)
            for (int z = b.minZ() >> 4; z <= b.maxZ() >> 4; z++)
                chunks.computeIfAbsent(key(x,z), k -> new ArrayList<>()).add(zone);
    }
    public void add(BoundingBox box, boolean deep) {
        if (rooms.stream().anyMatch(z -> z.box().equals(box))) return;
        Zone zone = new Zone(new BoundingBox(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ()), deep);
        rooms.add(zone); index(zone); setDirty();
    }
    /** Full placed room volume, strict less-than 12 cubed; oldest overlapping room owns the point. */
    public static boolean small(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !level.dimension().equals(Schizm.KEY)) return false;
        DeepRoomZones data = get(server);
        for (Zone zone : data.chunks.getOrDefault(key(pos.getX() >> 4, pos.getZ() >> 4), List.of())) {
            if (!zone.box().isInside(pos)) continue;
            BoundingBox b = zone.box();
            return ((long)b.maxX()-b.minX()+1) * ((long)b.maxY()-b.minY()+1)
                    * ((long)b.maxZ()-b.minZ()+1) < 1728;
        }
        return false;
    }
    public static boolean bugHabitat(Level level, BlockPos pos) {
        return small(level, pos) || contains(level, pos);
    }
    private boolean deep(BlockPos pos) {
        for (Zone zone : chunks.getOrDefault(key(pos.getX() >> 4, pos.getZ() >> 4), List.of()))
            if (zone.box().isInside(pos)) return zone.deep();
        return false;
    }
}
