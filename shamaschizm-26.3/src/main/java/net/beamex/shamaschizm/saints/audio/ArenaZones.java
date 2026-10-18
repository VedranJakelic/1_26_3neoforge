package net.beamex.shamaschizm.saints.audio;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.phys.Vec3;
/** Marker positions are captured before the generator removes the wool. */
public final class ArenaZones extends SavedData {
 public record Zone(BoundingBox box,BlockPos green,BlockPos red,boolean arena){
  static final Codec<Zone> CODEC=RecordCodecBuilder.create(i->i.group(
   BoundingBox.CODEC.fieldOf("box").forGetter(Zone::box),BlockPos.CODEC.fieldOf("green").forGetter(Zone::green),
   BlockPos.CODEC.fieldOf("red").forGetter(Zone::red),Codec.BOOL.fieldOf("arena").forGetter(Zone::arena)).apply(i,Zone::new));
 }
 private final List<Zone> zones;
 private static final Codec<ArenaZones> CODEC=Zone.CODEC.listOf().xmap(ArenaZones::new,d->d.zones);
 private static final SavedDataType<ArenaZones> TYPE=new SavedDataType<>(Shamaschizm.id("arena_audio_zones"),()->new ArenaZones(List.of()),CODEC,null);
 private ArenaZones(List<Zone> z){zones=new ArrayList<>(z);}
 public static ArenaZones get(ServerLevel l){return l.getDataStorage().computeIfAbsent(TYPE);}
 public void add(BoundingBox box,BlockPos green,BlockPos red,boolean arena){
  if(zones.stream().anyMatch(z->z.box.equals(box)&&z.arena==arena))return;
  zones.add(new Zone(box,green.immutable(),red.immutable(),arena));setDirty();
 }
 public boolean arena(BlockPos p){return zones.stream().anyMatch(z->z.arena&&z.box.isInside(p));}
 public float gain(Vec3 p){
  float gain=1;
  for(Zone z:zones)if(z.box.isInside(BlockPos.containing(p))){
   if(z.arena)return 0;
   Vec3 start=Vec3.atCenterOf(z.green),direction=Vec3.atCenterOf(z.red).subtract(start);
   double length=direction.lengthSqr();
   if(length>0)gain=Math.min(gain,(float)(1-Math.max(0,Math.min(1,p.subtract(start).dot(direction)/length))));
  }
  return gain;
 }
}
