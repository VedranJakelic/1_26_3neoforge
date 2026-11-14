package net.beamex.shamaschizm.ritual;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Actual anchor activations, persisted separately in each dimension. */
public final class RitualCompletionData extends SavedData {
    private static final Codec<RitualCompletionData> CODEC=Codec.LONG.listOf().fieldOf("centers").codec().xmap(
            RitualCompletionData::new,data->new ArrayList<>(data.completed));
    private static final SavedDataType<RitualCompletionData> TYPE=new SavedDataType<>(
            Shamaschizm.id("completed_villager_rituals"),RitualCompletionData::new,CODEC,DataFixTypes.LEVEL);
    private final Set<Long> completed;
    private RitualCompletionData(){this(List.of());}
    private RitualCompletionData(List<Long> positions){completed=new HashSet<>(positions);}
    private static RitualCompletionData get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public static void record(ServerLevel level,BlockPos center){
        RitualCompletionData data=get(level);
        if(data.completed.add(center.asLong())) data.setDirty();
    }
    public static boolean contains(ServerLevel level,BlockPos center){return get(level).completed.contains(center.asLong());}
    /** Same 7x7 horizontal and five-block vertical search as player crafting. */
    public static boolean canCraftNear(ServerLevel level,BlockPos origin){
        for(int y=-2;y<=2;y++)for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            BlockPos center=origin.offset(x,y,z);
            if(contains(level,center)&&RitualEvents.isRitual(level,center,true))return true;
        }
        return false;
    }
}
