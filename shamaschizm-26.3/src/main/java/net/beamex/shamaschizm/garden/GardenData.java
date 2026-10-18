package net.beamex.shamaschizm.garden;

import com.mojang.serialization.Codec;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class GardenData extends SavedData {
    public final List<BoundingBox> rooms;
    private static final Codec<GardenData> CODEC = BoundingBox.CODEC.listOf().xmap(GardenData::new, d -> d.rooms);
    private static final SavedDataType<GardenData> TYPE = new SavedDataType<>(
            Shamaschizm.id("garden_rooms"), () -> new GardenData(List.of()), CODEC, null);
    public GardenData(List<BoundingBox> rooms) { this.rooms = new ArrayList<>(rooms); }
    public static GardenData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(TYPE); }
    public void add(BoundingBox box) {
        if (rooms.stream().anyMatch(existing -> sameBox(existing, box))) return;
        rooms.add(box);
        setDirty();
    }

    public int count() {
        return rooms.size();
    }

    private static boolean sameBox(BoundingBox first, BoundingBox second) {
        return first.minX() == second.minX() && first.minY() == second.minY()
                && first.minZ() == second.minZ() && first.maxX() == second.maxX()
                && first.maxY() == second.maxY() && first.maxZ() == second.maxZ();
    }
}
