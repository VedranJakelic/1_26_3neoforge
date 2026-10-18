package net.beamex.shamaschizm.saints;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Death reports survive even when the centre of an arena is unloaded. */
public final class SaintsDeathData extends SavedData {
    private final Map<String,Integer> deaths;
    private static final Codec<SaintsDeathData> CODEC = Codec.unboundedMap(Codec.STRING,Codec.INT)
            .xmap(SaintsDeathData::new,data->data.deaths);
    private static final SavedDataType<SaintsDeathData> TYPE = new SavedDataType<>(
            Shamaschizm.id("catacomb_saints_deaths"),()->new SaintsDeathData(Map.of()),CODEC,null);
    private SaintsDeathData(Map<String,Integer> deaths){this.deaths=new HashMap<>(deaths);}
    public static SaintsDeathData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public int mask(UUID encounter){return deaths.getOrDefault(encounter.toString(),0);}
    public void record(UUID encounter,int role){
        if(role<0||role>3)return;
        deaths.merge(encounter.toString(),1<<role,(a,b)->a|b);setDirty();
    }
    public void recordSpearClaimed(UUID encounter){deaths.merge(encounter.toString(),16,(a,b)->a|b);setDirty();}
    public boolean spearClaimed(UUID encounter){return (mask(encounter)&16)!=0;}
    public void recordVindicationDefeated(UUID encounter){deaths.merge(encounter.toString(),32,(a,b)->a|b);setDirty();}
    public boolean vindicationDefeated(UUID encounter){return (mask(encounter)&32)!=0;}
    public void clear(UUID encounter){if(deaths.remove(encounter.toString())!=null)setDirty();}
}
