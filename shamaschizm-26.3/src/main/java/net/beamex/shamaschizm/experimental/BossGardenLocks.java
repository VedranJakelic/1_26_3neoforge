package net.beamex.shamaschizm.experimental;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.saints.CatacombEncounterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

/** Persistent locks remain after the encounter unloads or its reward data is cleared. */
public final class BossGardenLocks extends SavedData {
    private record Lock(BoundingBox arena,BoundingBox garden,BlockPos doorway,String encounter,boolean beaten){
        private static final Codec<Lock> CODEC=RecordCodecBuilder.create(i->i.group(
                BoundingBox.CODEC.fieldOf("arena").forGetter(Lock::arena),
                BoundingBox.CODEC.fieldOf("garden").forGetter(Lock::garden),
                BlockPos.CODEC.fieldOf("doorway").forGetter(Lock::doorway),
                Codec.STRING.optionalFieldOf("encounter","").forGetter(Lock::encounter),
                Codec.BOOL.optionalFieldOf("beaten",false).forGetter(Lock::beaten)).apply(i,Lock::new));
    }
    private final List<Lock> locks;
    private static final Codec<BossGardenLocks> CODEC=Lock.CODEC.listOf().xmap(BossGardenLocks::new,d->d.locks);
    private static final SavedDataType<BossGardenLocks> TYPE=new SavedDataType<>(Shamaschizm.id("boss_garden_locks"),()->new BossGardenLocks(List.of()),CODEC,null);
    private BossGardenLocks(List<Lock> records){locks=new ArrayList<>(records);}
    public static BossGardenLocks get(ServerLevel level){return level.getDataStorage().computeIfAbsent(TYPE);}
    public void add(ServerLevel level,BoundingBox arena,BoundingBox garden,BlockPos doorway){
        if(locks.stream().anyMatch(l->l.doorway.equals(doorway)))return;
        var bosses=level.getEntitiesOfClass(CatacombEncounterEntity.class,new AABB(arena.minX(),arena.minY(),arena.minZ(),arena.maxX()+1,arena.maxY()+1,arena.maxZ()+1));
        String id=bosses.size()==1?bosses.get(0).getUUID().toString():"";
        locks.add(new Lock(arena,garden,doorway.immutable(),id,false));setDirty();
    }
    public boolean encounterBeaten(UUID id){return locks.stream().anyMatch(l->l.beaten && l.encounter.equals(id.toString()));}
    public boolean insideArena(BlockPos pos){return locks.stream().anyMatch(l->l.arena.isInside(pos));}
    /** Victory is persisted independently of loaded encounter entities. */
    public boolean insideDefeatedArena(BlockPos pos){return locks.stream().anyMatch(l->l.beaten && l.arena.isInside(pos));}
    public void bind(BlockPos centre,UUID encounter){
        for(int i=0;i<locks.size();i++){
            Lock l=locks.get(i);
            if(!l.beaten && l.encounter.isEmpty() && l.arena.isInside(centre)){
                locks.set(i,new Lock(l.arena,l.garden,l.doorway,encounter.toString(),false));setDirty();
            }
        }
    }
    public void defeated(UUID encounter){
        for(int i=0;i<locks.size();i++){
            Lock l=locks.get(i);
            if(!l.beaten && l.encounter.equals(encounter.toString())){
                locks.set(i,new Lock(l.arena,l.garden,l.doorway,l.encounter,true));setDirty();
            }
        }
    }
    public static boolean isLockedGate(ServerLevel level,BlockPos gate){
        // Cover the shared entrance and gates set farther inside the garden.
        // Other arena doors, away from this garden connection, keep their own rules.
        return get(level).locks.stream().anyMatch(l->!l.beaten
                && (l.garden.isInside(gate) || l.doorway.distSqr(gate)<=64));
    }
}
