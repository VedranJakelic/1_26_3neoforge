package net.beamex.shamaschizm.effect;
import net.beamex.shamaschizm.Shamaschizm;
import net.beamex.shamaschizm.world.Schizm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.Arrays;
/** Saved independent onset timers: another mushroom never postpones an earlier dose. */
@EventBusSubscriber(modid=Shamaschizm.MOD_ID)
public final class MushroomOnset {
    private static final String NAUSEA="shamaschizm_mushroom_nausea_delay",TRIP="shamaschizm_mushroom_trip_delay",DOSES="shamaschizm_mushroom_doses";
    public static final int MAX_LEVEL=8;
    private MushroomOnset(){}
    private static int[] pending(ServerPlayer player){
        var data=player.getPersistentData();int[] doses=data.getIntArray(DOSES).orElse(new int[0]);
        int legacy=data.getInt(TRIP).orElse(0);
        if(legacy>0){doses=Arrays.copyOf(doses,doses.length+1);doses[doses.length-1]=legacy;}
        data.remove(TRIP);return doses;
    }
    public static void schedule(ServerPlayer player){
        var data=player.getPersistentData();if(data.getInt(NAUSEA).orElse(0)<=0)data.putInt(NAUSEA,200);
        int[] doses=pending(player);
        if(doses.length<64){doses=Arrays.copyOf(doses,doses.length+1);doses[doses.length-1]=300;}
        data.putIntArray(DOSES,doses);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var data=player.getPersistentData();
        if(!player.isAlive()){data.remove(NAUSEA);data.remove(TRIP);data.remove(DOSES);return;}
        if(ready(player,NAUSEA))player.addEffect(new MobEffectInstance(MobEffects.NAUSEA,300,0));
        int[] doses=pending(player);int kept=0,due=0;
        for(int delay:doses){if(delay<=1)due++;else doses[kept++]=delay-1;}
        if(kept==0)data.remove(DOSES);else data.putIntArray(DOSES,Arrays.copyOf(doses,kept));
        if(due==0)return;
        var current=player.getEffect(ModEffects.TRIPPING);int previous=current==null?-1:current.getAmplifier();
        int amplifier=Math.max(previous,Math.min(MAX_LEVEL-1,previous+due));
        int duration=player.level().dimension().equals(Schizm.KEY)?2880:1920;
        if(current!=null)duration=current.isInfiniteDuration()?-1:Math.max(duration,current.getDuration());
        player.addEffect(new MobEffectInstance(ModEffects.TRIPPING,duration,amplifier));
    }
    private static boolean ready(ServerPlayer player,String key){
        int ticks=player.getPersistentData().getInt(key).orElse(0);if(ticks<=0)return false;
        if(ticks==1){player.getPersistentData().remove(key);return true;}
        player.getPersistentData().putInt(key,ticks-1);return false;
    }
}
