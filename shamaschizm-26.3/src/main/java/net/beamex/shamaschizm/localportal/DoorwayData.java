package net.beamex.shamaschizm.localportal;

import com.mojang.serialization.Codec;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Separate from the Overworld/Schizm entrance links and from dungeon generation. */
public final class DoorwayData extends SavedData {
    private final Map<String, Doorway> entries = new LinkedHashMap<>();
    private long revision;
    private boolean alignmentChecked;
    private static final Codec<DoorwayData> CODEC = Doorway.CODEC.listOf()
            .xmap(DoorwayData::new, DoorwayData::all);
    private static final SavedDataType<DoorwayData> TYPE = new SavedDataType<>(
            Shamaschizm.id("local_doorways"), () -> new DoorwayData(List.of()), CODEC, null);

    public DoorwayData(List<Doorway> saved) { saved.forEach(d -> entries.put(d.name(), d)); }
    public static DoorwayData get(ServerLevel level) {
        DoorwayData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.alignExisting();
        return data;
    }
    private void alignExisting() {
        if (alignmentChecked) return;
        alignmentChecked = true;
        boolean moved = false;
        for (var entry : entries.entrySet()) {
            Doorway old = entry.getValue(), aligned = old.blockAligned();
            if (aligned != old) { entry.setValue(aligned); moved = true; }
        }
        // Run after SavedData decoding so the migration's dirty flag survives loading.
        if (moved) changed();
    }
    public List<Doorway> all() { return List.copyOf(entries.values()); }
    public Doorway get(String name) { return entries.get(name); }
    public long revision() { return revision; }
    private void changed() { revision++; setDirty(); }

    public void put(Doorway doorway) { entries.put(doorway.name(), doorway.blockAligned()); changed(); }

    public void unlink(String name) {
        entries.replaceAll((key, d) -> key.equals(name) || d.target().equals(name) ? d.link("", false) : d);
        changed();
    }

    public void remove(String name) { unlink(name); entries.remove(name); changed(); }

    public void link(String first, String second) {
        unlink(first); unlink(second);
        entries.put(first, entries.get(first).link(second, false));
        entries.put(second, entries.get(second).link(first, false));
        changed();
    }

    public void enable(String name, boolean enabled) {
        Doorway a = entries.get(name), b = entries.get(a.target());
        entries.put(name, a.link(b.name(), enabled));
        entries.put(b.name(), b.link(name, enabled));
        changed();
    }
}
