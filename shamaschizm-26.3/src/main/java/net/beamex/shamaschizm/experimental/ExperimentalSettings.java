package net.beamex.shamaschizm.experimental;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Saved per world, shared by commands run in every dimension. */
public final class ExperimentalSettings extends SavedData {
    public static final int DEFAULT_ROOMS = 60;
    public static final int MIN_ROOMS = 4; // Allows the existing first-five-room branch guarantee.
    public static final int MAX_ROOMS = 256;
    private boolean arenaGarden;
    private int roomTarget;
    private boolean deepRooms;
    private static final Codec<ExperimentalSettings> FIELDS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("arena_garden", false).forGetter(ExperimentalSettings::arenaGarden),
            Codec.intRange(MIN_ROOMS, MAX_ROOMS).optionalFieldOf("room_target", DEFAULT_ROOMS).forGetter(ExperimentalSettings::roomTarget),
            Codec.BOOL.optionalFieldOf("deep_rooms", false).forGetter(ExperimentalSettings::deepRooms)
    ).apply(instance, ExperimentalSettings::new));
    // Read old boolean-only saves without losing the arena2 toggle; always write the new object.
    private static final Codec<ExperimentalSettings> CODEC = Codec.either(Codec.BOOL, FIELDS).xmap(
            value -> value.map(enabled -> new ExperimentalSettings(enabled, DEFAULT_ROOMS, false), settings -> settings),
            Either::right);
    private static final SavedDataType<ExperimentalSettings> TYPE = new SavedDataType<>(
            Shamaschizm.id("experimental_settings"),
            () -> new ExperimentalSettings(false, DEFAULT_ROOMS, false), CODEC, null);
    private ExperimentalSettings(boolean arenaGarden, int roomTarget, boolean deepRooms) {
        this.arenaGarden = arenaGarden;
        this.roomTarget = roomTarget;
        this.deepRooms = deepRooms;
    }
    public static ExperimentalSettings get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public boolean arenaGarden() { return arenaGarden; }
    public int roomTarget() { return roomTarget; }
    public boolean deepRooms() { return deepRooms; }
    public boolean allowsTag(String tag) { return deepRooms || !"deep".equals(tag); }
    public void setArenaGarden(boolean value) { arenaGarden = value; setDirty(); }
    public void setDeepRooms(boolean value) { deepRooms = value; setDirty(); }
    public void setRoomTarget(int value) {
        if (value < MIN_ROOMS || value > MAX_ROOMS) throw new IllegalArgumentException("Room target must be 4..256");
        roomTarget = value;
        setDirty();
    }
}
