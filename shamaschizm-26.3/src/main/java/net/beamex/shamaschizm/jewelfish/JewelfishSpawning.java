package net.beamex.shamaschizm.jewelfish;

import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.beamex.shamaschizm.saints.audio.ArenaZones;
import net.beamex.shamaschizm.experimental.BossGardenLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class JewelfishSpawning {
    /** Three blocks deep and a 3x3 swimming pocket at the candidate's height. */
    static boolean pool(ServerLevel level,BlockPos p){
        for(int y=-1;y<=1;y++)if(!water(level,p.offset(0,y,0)))return false;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!water(level,p.offset(x,0,z)))return false;
        return true;
    }
    private static boolean water(ServerLevel level,BlockPos p){return level.getFluidState(p).is(FluidTags.WATER)&&level.getBlockState(p).getCollisionShape(level,p).isEmpty();}
    @SubscribeEvent public static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||!level.dimension().equals(Schizm.KEY)
                ||level.getGameTime()%200!=0||!level.getGameRules().get(GameRules.SPAWN_MOBS))return;
        int count=0,attempts=0;
        for(var e:level.getAllEntities())if(e instanceof Jewelfish&&e.isAlive()&&++count>=24)return;
        for(var player:level.players()){
            if(!player.isAlive()||player.isSpectator()||level.getRandom().nextFloat()>.35F)continue;
            for(int i=0;i<6&&attempts++<18;i++){
                double angle=level.getRandom().nextDouble()*Math.PI*2,range=16+level.getRandom().nextDouble()*24;
                BlockPos column=BlockPos.containing(player.getX()+Math.cos(angle)*range,player.getY()+12,player.getZ()+Math.sin(angle)*range);
                if(!level.hasChunksAt(column.offset(-2,-32,-2),column.offset(2,2,2)))continue;
                for(int dy=0;dy<32;dy++){
                    BlockPos pos=column.below(dy);
                    if(!level.getWorldBorder().isWithinBounds(pos)||!pool(level,pos))continue;
                    if(ArenaZones.get(level).arena(pos)||BossGardenLocks.get(level).insideArena(pos))continue;
                    if(level.players().stream().anyMatch(p->!p.isSpectator()&&p.distanceToSqr(pos.getX()+.5,pos.getY(),pos.getZ()+.5)<16*16))continue;
                    if(!level.getEntitiesOfClass(Jewelfish.class,new AABB(pos).inflate(12)).isEmpty())break;
                    var fish=JewelfishRegistration.TYPE.create(level,EntitySpawnReason.NATURAL);if(fish==null)return;
                    fish.snapTo(pos.getX()+.5,pos.getY()+.2,pos.getZ()+.5,level.getRandom().nextFloat()*360,0);
                    if(level.noCollision(fish)&&level.addFreshEntity(fish)){if(++count>=24)return;break;}
                }
            }
            if(attempts>=18)return;
        }
    }
}
