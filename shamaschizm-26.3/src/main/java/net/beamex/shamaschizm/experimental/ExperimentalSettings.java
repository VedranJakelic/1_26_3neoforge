package net.beamex.shamaschizm.experimental;

import com.mojang.serialization.Codec;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** World-wide opt-in, independent of the dimension where the command is run. */
public final class ExperimentalSettings extends SavedData {
    private boolean arenaGarden;
    private static final Codec<ExperimentalSettings> CODEC=Codec.BOOL.xmap(ExperimentalSettings::new,s->s.arenaGarden);
    private static final SavedDataType<ExperimentalSettings> TYPE=new SavedDataType<>(Shamaschizm.id("experimental_settings"),()->new ExperimentalSettings(false),CODEC,null);
    private ExperimentalSettings(boolean enabled){arenaGarden=enabled;}
    public static ExperimentalSettings get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);}
    public boolean arenaGarden(){return arenaGarden;}
    public void setArenaGarden(boolean value){arenaGarden=value;setDirty();}
}
