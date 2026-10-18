package net.beamex.shamaschizm.saints.vindication;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
public final class SaintRemains {
 public static void scatter(ServerLevel level,LivingEntity saint,UUID encounter,int role){
  if(role<0||role>3 || saint.getPersistentData().getBooleanOr("SaintBonesScattered",false))return;
  saint.getPersistentData().putBoolean("SaintBonesScattered",true);
  for(int part=0;part<11;part++){
   SaintBoneEntity bone=VindicationRegistration.BONE.create(level,EntitySpawnReason.EVENT);if(bone==null)continue;
   double x=part==7?-.3125:part==8?.3125:part==9?-.125:part==10?.125:0;
   double y=part==0?1.75:part<=6?.81+(part-1)*.125:part<=8?1.1:.375;
   Vec3 offset=new Vec3(x,y-(part==0?.25:.075),0).yRot((float)Math.toRadians(-saint.getYRot()));
   bone.configure(encounter,role,part,saint.position().add(offset),saint.getYRot());bone.setCorpse(saint.getId());level.addFreshEntity(bone);
  }
  // Leave the dying entity alive long enough for vanilla loot/XP handling, but hide its corpse.
  saint.setInvisible(true);saint.setGlowingTag(false);saint.removeEffect(net.minecraft.world.effect.MobEffects.GLOWING);
 }
}
