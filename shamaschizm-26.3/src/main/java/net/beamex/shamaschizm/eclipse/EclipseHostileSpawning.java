package net.beamex.shamaschizm.eclipse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Bounded supplemental packs; cave monsters filling vanilla's global cap cannot suppress the event. */
@EventBusSubscriber(modid="shamaschizm")
public final class EclipseHostileSpawning {
    private static final int INTERVAL=100, EVENT_CAP=48, LOCAL_CAP=22, MAX_NEW=6;
    private EclipseHostileSpawning(){}
    @SubscribeEvent public static void tick(LevelTickEvent.Post e){
        if(!(e.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
                || level.getDifficulty()==Difficulty.PEACEFUL || !level.getGameRules().get(GameRules.SPAWN_MOBS)
                || level.getGameTime()%INTERVAL!=0) return;
        long time=level.getOverworldClockTime();if(!EclipseOrbit.nearest(time).active(time))return;
        var shadow=EclipseOrbit.shadow(time);if(!shadow.aboveHorizon())return;
        AABB eventArea=new AABB(shadow.x()-250,level.getMinY(),shadow.z()-250,
                shadow.x()+250,level.getMaxY(),shadow.z()+250);
        int population=level.getEntitiesOfClass(Monster.class,eventArea,m->m.isAlive()).size();
        if(population>=EVENT_CAP)return;
        var players=level.players();if(players.isEmpty())return;
        int left=Math.min(MAX_NEW,EVENT_CAP-population);
        int start=(int)Math.floorMod(level.getGameTime()/INTERVAL,players.size());
        for(int i=0;i<players.size() && left>0;i++){
            var p=players.get((start+i)%players.size());
            if(!p.isAlive() || p.isSpectator() || !Eclipses.active(level,p.blockPosition()))continue;
            var random=level.getRandom();
            double angle=random.nextDouble()*Math.PI*2,range=32+random.nextDouble()*32;
            int x=(int)Math.floor(p.getX()+Math.cos(angle)*range),z=(int)Math.floor(p.getZ()+Math.sin(angle)*range);
            BlockPos rough=new BlockPos(x,p.blockPosition().getY(),z);
            if(!level.hasChunksAt(rough.offset(-5,-16,-5),rough.offset(5,16,5)))continue;
            int surface=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            int y=Math.abs(surface-p.getY())<=24?surface:p.blockPosition().getY();
            BlockPos center=new BlockPos(x,y,z);
            if(!Eclipses.active(level,center))continue;
            int nearby=level.getEntitiesOfClass(Monster.class,new AABB(center).inflate(64,48,64),m->m.isAlive()).size();
            int count=Math.min(left,Math.min(4+random.nextInt(3),LOCAL_CAP-nearby));
            EntityType<? extends Mob> type=random.nextBoolean()?EntityTypes.ZOMBIE:EntityTypes.SKELETON;
            for(int member=0;member<count && left>0;member++){
                var result=SpawnUtil.trySpawnMob(type,EntitySpawnReason.NATURAL,level,center,4,4,12,
                        (world,floor,floorState,pos,state)->allowed(world,pos)
                            && SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES.canSpawnOn(world,floor,floorState,pos,state),true);
                if(result.isPresent()){
                    Mob mob=result.get();
                    // Existing NATURAL finalization applies the Schizm Y=0 loadout first, including ancient variants.
                    // Ensure most supplemental mobs visibly wear armour without overwriting those special pieces.
                    if(random.nextFloat()<.8F){
                        var slots=new java.util.ArrayList<>(java.util.List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET));
                        int pieces=2+random.nextInt(3);boolean iron=random.nextInt(4)!=0;
                        for(int j=0;j<pieces;j++){
                            var slot=slots.remove(random.nextInt(slots.size()));
                            if(mob.getItemBySlot(slot).isEmpty())mob.setItemSlot(slot,new ItemStack(switch(slot){
                                case HEAD->iron?Items.IRON_HELMET:Items.CHAINMAIL_HELMET;
                                case CHEST->iron?Items.IRON_CHESTPLATE:Items.CHAINMAIL_CHESTPLATE;
                                case LEGS->iron?Items.IRON_LEGGINGS:Items.CHAINMAIL_LEGGINGS;
                                default->iron?Items.IRON_BOOTS:Items.CHAINMAIL_BOOTS;
                            }));
                        }
                    }
                    left--;
                }
            }
        }
    }
    private static boolean allowed(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || !Eclipses.active(level,pos))return false;
        Vec3 point=Vec3.atBottomCenterOf(pos);boolean near=false;
        for(var p:level.players()){
            if(!p.isAlive() || p.isSpectator())continue;
            double d=p.position().distanceToSqr(point);
            if(d<24*24)return false;
            if(d<96*96)near=true;
        }
        return near && level.getEntitiesOfClass(Monster.class,new AABB(pos).inflate(32,16,32),m->m.isAlive()).size()<10;
    }
}
